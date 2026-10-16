"""Validate and package Milo's expanded action corpus. Python standard library only."""
import argparse
from collections import Counter, defaultdict
from datetime import datetime
import hashlib
import json
from pathlib import Path
import random
import re
import unicodedata

HERE = Path(__file__).resolve().parent
SPLITS = ("train", "validation", "test")
RATIOS = (0.8, 0.1, 0.1)
ASK = {"time", "contact", "message", "app", "place", "item", "list", "date", "which"}
DEVELOPER = "You are a model that can do function calling with the following functions"


def read_jsonl(path):
    rows = []
    with path.open(encoding="utf-8-sig") as f:
        for n, line in enumerate(f, 1):
            if not line.strip():
                raise ValueError(f"{path.name}:{n}: blank line")
            rows.append(json.loads(line))
    return rows


def write_jsonl(path, rows):
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", encoding="utf-8", newline="\n") as f:
        for row in rows:
            f.write(json.dumps(row, ensure_ascii=False, separators=(",", ":")) + "\n")


def normalized(text):
    return " ".join(unicodedata.normalize("NFC", text).casefold().split())


def input_key(row):
    # now is deliberately excluded: changing only the date is not a new utterance.
    return normalized(json.dumps(row["messages"][:-1], ensure_ascii=False, sort_keys=True))


def require(ok, message):
    if not ok:
        raise ValueError(message)


def validate_value(value, spec, label):
    kind = spec["type"]
    if kind == "string":
        require(isinstance(value, str) and bool(value.strip()), label + ": empty/non-string")
    elif kind == "boolean":
        require(type(value) is bool, label + ": expected boolean")
    elif kind == "integer":
        require(type(value) is int and value >= 0, label + ": expected nonnegative integer")
    elif kind == "array":
        require(isinstance(value, list) and len(value) > 0, label + ": empty/non-array")
        for item in value:
            validate_value(item, spec["items"], label)
    elif kind == "object":
        require(isinstance(value, dict), label + ": expected object")
        props = spec.get("properties", {})
        require(not (set(value) - set(props)), label + ": unknown argument")
        require(set(spec.get("required", [])).issubset(value), label + ": missing required argument")
        for key, val in value.items():
            validate_value(val, props[key], label + "." + key)
    else:
        raise ValueError("Unsupported schema kind: " + kind)
    if "enum" in spec:
        require(value in spec["enum"], label + ": invalid enum")


def validate_time(value):
    require(isinstance(value, str), "time must be a string")
    if re.fullmatch(r"\d{4}-\d{2}-\d{2} \d{2}:\d{2}", value):
        datetime.strptime(value, "%Y-%m-%d %H:%M")
        return
    relative = re.fullmatch(r"in (?:(\d+) hours?(?: (\d+) minutes?)?|(\d+) minutes?)", value)
    if relative:
        require(any(int(x) > 0 for x in relative.groups() if x is not None), "zero duration")
        return
    clock = re.fullmatch(r"(?:(?:today|tomorrow|day after tomorrow|monday|tuesday|wednesday|thursday|friday|saturday|sunday) )?(\d{1,2})(?::(\d{2}))?(?: (am|pm))?", value)
    require(clock is not None, "noncanonical time: " + value)
    hour, minute, ap = clock.groups()
    require(0 <= int(hour) <= 23 and (minute is None or int(minute) <= 59), "invalid clock")
    if ap:
        require(1 <= int(hour) <= 12, "12-hour clock out of range")


def validate_row(row, schemas):
    require(set(row) == {"id", "lang", "now", "messages", "say"}, "unexpected row fields")
    require(re.fullmatch(r"\d{1,2}-\d{2}", row["id"]) is not None, "bad id")
    require(row["lang"] in ("ml", "en", "mixed"), "bad language")
    stamp, weekday = row["now"].split(" ")
    now = datetime.strptime(stamp, "%Y-%m-%dT%H:%M")
    require(now.year in (2026, 2027) and now.strftime("%A") == weekday, "incorrect now date/weekday")
    require(isinstance(row["say"], str) and len(row["say"].split()) <= 12, "say exceeds 12 words")
    if row["say"] and row["lang"] in ("ml", "mixed"):
        require(re.search(r"[\u0d00-\u0d7f]", row["say"]) is not None, "say needs Malayalam script")
        require("നിങ്ങൾ" not in row["say"], "say uses formal pronoun")
    messages = row["messages"]
    require(isinstance(messages, list) and len(messages) >= 2, "missing conversation")
    require(messages[0]["role"] == "user" and messages[-1]["role"] == "assistant", "bad first/final role")
    pending = []
    for m in messages:
        role = m["role"]
        require(role in ("user", "assistant", "tool"), "bad role")
        if role == "assistant" and "tool_calls" in m:
            require(set(m) == {"role", "tool_calls"}, "action also contains free text")
            require(isinstance(m["tool_calls"], list) and m["tool_calls"], "empty calls")
            pending = []
            for call in m["tool_calls"]:
                require(set(call) == {"name", "arguments"}, "bad call fields")
                name, args = call["name"], call["arguments"]
                require(name in schemas, "unknown tool " + name)
                validate_value(args, schemas[name]["parameters"], name)
                if "time" in args:
                    validate_time(args["time"])
                if name == "set_alarm":
                    require(("time" in args) != ("duration" in args), "alarm needs exactly one of time/duration")
                    if "duration" in args:
                        duration = re.fullmatch(r"(?:(\d+) hours?(?: (\d+) minutes?)?|(\d+) minutes?|(\d+) seconds?)", args["duration"])
                        require(duration is not None and any(int(x) > 0 for x in duration.groups() if x is not None), "invalid timer duration")
                if name == "phone_setting" and args["setting"] not in ("volume_up", "volume_down"):
                    require("on" in args, "toggle must specify on/off")
                if name == "show_plan" and "day" in args:
                    if re.fullmatch(r"\d{4}-\d{2}-\d{2}", args["day"]):
                        datetime.strptime(args["day"], "%Y-%m-%d")
                    else:
                        require(args["day"] in ("today", "tomorrow", "monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday"), "invalid plan day")
                pending.append(name)
        elif role == "tool":
            require(set(m) == {"role", "name", "content"}, "invalid tool result")
            require(m["name"] in pending, "tool result without matching prior call")
            pending.remove(m["name"])
        else:
            require(set(m) == {"role", "content"}, "invalid text message fields")
            require(isinstance(m["content"], str) and bool(m["content"].strip()), "empty message")
            if role == "assistant":
                reply = m["content"]
                if reply.startswith("Ask: "):
                    slots = reply[5:].split(", ")
                    require(bool(slots) and set(slots).issubset(ASK) and len(set(slots)) == len(slots), "invalid missing slots")
                else:
                    require(reply in ("No action.", "Not allowed."), "invalid fixed reply")
    if messages[-1].get("content") == "No action.":
        require(row["say"] == "", "No action must have empty say")
    else:
        require(bool(row["say"]), "missing spoken confirmation")


def target_kind(row):
    final = row["messages"][-1]
    return "+".join(c["name"] for c in final["tool_calls"]) if "tool_calls" in final else final["content"].split(":")[0]


def split_groups(rows, metadata):
    groups = defaultdict(list)
    for r in rows:
        groups[metadata[r["id"]]["family"]].append(r)
    totals = Counter((r["lang"], metadata[r["id"]]["category"]) for r in rows)
    # Keep complete template families together; balance language/category approximately.
    rng = random.Random(270)
    ordered = list(groups.items())
    rng.shuffle(ordered)
    ordered.sort(key=lambda pair: -len(pair[1]))
    counts = [Counter() for _ in SPLITS]
    sizes = [0] * 3
    assignment = {}
    for family, members in ordered:
        additions = Counter((r["lang"], metadata[r["id"]]["category"]) for r in members)
        scores = []
        for i, ratio in enumerate(RATIOS):
            change = sum(((counts[i][k] + v - totals[k]*ratio)**2 - (counts[i][k] - totals[k]*ratio)**2) / max(1, totals[k]*ratio) for k, v in additions.items())
            change += (((sizes[i]+len(members)-len(rows)*ratio)**2 - (sizes[i]-len(rows)*ratio)**2) / (len(rows)*ratio))
            scores.append(change)
        chosen = min(range(3), key=lambda i: scores[i])
        assignment[family] = SPLITS[chosen]
        counts[chosen].update(additions)
        sizes[chosen] += len(members)
    # Keep rare tools evaluable without splitting their generation families.
    family_tools = {f: Counter(c["name"] for r in members for c in r["messages"][-1].get("tool_calls", [])) for f, members in groups.items()}
    available = Counter(t for ts in family_tools.values() for t in ts)
    for destination in ("test", "validation"):
        for tool in sorted(available):
            if available[tool] < (2 if destination == "test" else 3):
                continue
            coverage = {s: Counter() for s in SPLITS}
            for f, ts in family_tools.items():
                coverage[assignment[f]].update(ts)
            if coverage[destination][tool]:
                continue
            candidates = [f for f, ts in family_tools.items() if ts[tool] and assignment[f] != destination and all(coverage[assignment[f]][t] > n for t, n in ts.items())]
            if candidates:
                chosen_family = min(candidates, key=lambda f: (len(groups[f]), f))
                assignment[chosen_family] = destination
    return {r["id"]: assignment[metadata[r["id"]]["family"]] for r in rows}


def convert(row, tools):
    stamp, weekday = row["now"].split(" ")
    developer = f"Current date and time: {stamp}:00\nDay of week: {weekday}\nTimezone: Asia/Kolkata\n{DEVELOPER}"
    messages = [{"role": "developer", "content": developer}]
    for original in row["messages"]:
        m = dict(original)
        if "tool_calls" in m:
            m["tool_calls"] = [{"type": "function", "function": c} for c in m["tool_calls"]]
        messages.append(m)
    return {"tools": tools, "messages": messages}


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--spec", type=Path, required=True)
    ap.add_argument("--output", type=Path, default=HERE / "release")
    args = ap.parse_args()
    spec_bytes = args.spec.read_bytes()
    source = spec_bytes.decode("utf-8-sig")
    schema_list = json.loads(re.search(r"```json\s*(\[.*?\])\s*```", source, re.S).group(1))
    schemas = {s["name"]: s for s in schema_list}
    rows, metas = [], []
    for stem in ("scheduling", "actions", "edges"):
        rows += read_jsonl(HERE / "parts" / (stem + ".jsonl"))
        metas += read_jsonl(HERE / "parts" / (stem + "-meta.jsonl"))
    rows.sort(key=lambda r: tuple(map(int, r["id"].split("-"))))
    require(len(rows) == 2300, f"expected 2300 rows, got {len(rows)}")
    meta = {m["id"]: m for m in metas}
    require(len(meta) == len(metas) == len(rows), "duplicate/missing metadata")
    seen_ids, seen_inputs, batches, errors = set(), {}, defaultdict(Counter), []
    for row in rows:
        try:
            validate_row(row, schemas)
            require(row["id"] not in seen_ids, "duplicate id")
            require(row["id"] in meta and meta[row["id"]].get("family") and meta[row["id"]].get("category"), "missing provenance/family")
            require(input_key(row) not in seen_inputs, "duplicate input with " + seen_inputs.get(input_key(row), ""))
            seen_ids.add(row["id"])
            seen_inputs[input_key(row)] = row["id"]
            batches[int(row["id"].split("-")[0])][row["lang"]] += 1
        except (ValueError, KeyError, TypeError) as e:
            errors.append(f"{row.get('id')}: {e}")
    if not errors:
        for b in range(1, 47):
            if batches[b] != Counter(ml=25, en=15, mixed=10):
                errors.append(f"batch {b}: bad language/count balance {dict(batches[b])}")
    args.output.mkdir(parents=True, exist_ok=True)
    if errors:
        (args.output / "validation-errors.txt").write_text("\n".join(errors)+"\n", encoding="utf-8")
        print("VALIDATION FAILED", len(errors), "issues; see validation-errors.txt")
        for e in errors[:20]:
            print(e)
        raise SystemExit(1)
    (args.output / "validation-errors.txt").unlink(missing_ok=True)
    tools = [{"type": "function", "function": s} for s in schema_list]
    assignments = split_groups(rows, meta)
    write_jsonl(args.output / "milo-chatgpt.jsonl", rows)
    (args.output / "tools.json").write_text(json.dumps(tools, ensure_ascii=False, indent=2)+"\n", encoding="utf-8")
    for split in SPLITS:
        selected = [r for r in rows if assignments[r["id"]] == split]
        write_jsonl(args.output / (split+".jsonl"), (convert(r, tools) for r in selected))
        write_jsonl(args.output / (split+".raw.jsonl"), selected)
        # Prompt/completion separates the LAST target from earlier assistants in multi-turn traces.
        def completion(r):
            converted = convert(r, tools)
            return {"tools": tools, "prompt": converted["messages"][:-1], "completion": converted["messages"][-1:]}
        write_jsonl(args.output / (split+".completion.jsonl"), (completion(r) for r in selected))
    write_jsonl(args.output / "provenance.jsonl", ({**meta[r["id"]], "split": assignments[r["id"]], "language_review": "pending", "semantic_review": "synthetic_not_human_verified"} for r in rows))
    write_jsonl(args.output / "confirmations.jsonl", ({"id": r["id"], "lang": r["lang"], "say": r["say"], "use": "only_after_success_or_draft_ready; never_train_as_action_target"} for r in rows))
    counts = {s: {"total": sum(assignments[r["id"]] == s for r in rows), "languages": dict(Counter(r["lang"] for r in rows if assignments[r["id"]] == s)), "targets": dict(Counter(target_kind(r) for r in rows if assignments[r["id"]] == s))} for s in SPLITS}
    report = {"rows": len(rows), "tools": len(tools), "languages": dict(Counter(r["lang"] for r in rows)), "splits": counts, "template_families": len({m["family"] for m in metas}), "schema_errors": 0, "duplicate_inputs": 0, "cross_split_template_families": 0, "source_spec_sha256": hashlib.sha256(spec_bytes).hexdigest(), "synthetic": True, "human_review": "pending", "tokenizer_rendering": "not_run", "model_training": "not_run"}
    tool_counts = Counter(c["name"] for r in rows for c in r["messages"][-1].get("tool_calls", []))
    require(set(schemas) == set(tool_counts), "some expanded tools have no final-target examples")
    report["final_tool_call_counts"] = dict(sorted(tool_counts.items()))
    report["tools_missing_by_split"] = {s: sorted(set(schemas) - {c["name"] for r in rows if assignments[r["id"]] == s for c in r["messages"][-1].get("tool_calls", [])}) for s in SPLITS}
    report["multiturn_rows"] = sum(len(r["messages"]) > 2 for r in rows)
    report["multi_action_rows"] = sum(len(r["messages"][-1].get("tool_calls", [])) > 1 for r in rows)
    (args.output / "validation-report.json").write_text(json.dumps(report, ensure_ascii=False, indent=2)+"\n", encoding="utf-8")
    manifest = {p.name: hashlib.sha256(p.read_bytes()).hexdigest() for p in sorted(args.output.glob("*.json*"))}
    (args.output / "SHA256SUMS.json").write_text(json.dumps(manifest, indent=2)+"\n", encoding="utf-8")
    print(json.dumps({"rows": len(rows), "families": report["template_families"], "splits": {s: counts[s]["total"] for s in SPLITS}}, indent=2))


if __name__ == "__main__":
    main()
