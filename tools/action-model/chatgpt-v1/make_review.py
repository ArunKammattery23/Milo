"""Generate an offline review sheet and audit published split/provenance alignment."""
from collections import defaultdict
import hashlib
import json
from pathlib import Path
from assemble_dataset import read_jsonl, input_key, require

HERE = Path(__file__).resolve().parent


def main():
    release = HERE / "release"
    meta = {r["id"]: r for r in read_jsonl(release / "provenance.jsonl")}
    rows = read_jsonl(release / "milo-chatgpt.jsonl")
    review = ["# Review required before training", "", "Synthetic corpus. Review meaning and Malayalam fluency; automated checks are not human approval.", "", "For each row: verify requested action, exact arguments, language, time/date and truth of the success-path confirmation. Leave test/challenge examples held out of training.", ""]
    for row in rows:
        review += [f"## {row['id']} · {row['lang']} · {meta[row['id']]['split']}", "", "- [ ] Intent and arguments correct", "- [ ] Natural language and confirmation correct", "", "Now: " + row["now"], ""]
        for m in row["messages"]:
            content = m.get("content", json.dumps(m.get("tool_calls"), ensure_ascii=False))
            review.append(f"- **{m['role']}**: {content}")
        review += ["", "Say (after app success): " + row["say"], "", "Reviewer correction: ", ""]
    (release / "REVIEW.md").write_text("\n".join(review), encoding="utf-8")
    by_family, by_input = defaultdict(set), defaultdict(set)
    ids_seen = set()
    for split in ("train", "validation", "test"):
        for r in read_jsonl(release / (split+".raw.jsonl")):
            require(r["id"] not in ids_seen, "row occurs in multiple splits")
            ids_seen.add(r["id"])
            require(meta[r["id"]]["split"] == split, "provenance/split mismatch")
            by_family[meta[r["id"]]["family"]].add(split)
            by_input[input_key(r)].add(split)
    require(len(ids_seen) == len(rows), "split rows missing")
    require(all(len(v)==1 for v in by_family.values()), "family leakage")
    require(all(len(v)==1 for v in by_input.values()), "input leakage")
    challenge = read_jsonl(release / "challenge.raw.jsonl")
    require(not ({input_key(r) for r in challenge} & set(by_input)), "challenge duplicate of base corpus")
    manifests = {p.relative_to(release).as_posix(): hashlib.sha256(p.read_bytes()).hexdigest() for p in sorted(release.iterdir()) if p.is_file() and p.name != "SHA256SUMS.json"}
    (release / "SHA256SUMS.json").write_text(json.dumps(manifests, indent=2)+"\n", encoding="utf-8")
    print("Published split audit passed; review sheet and checksums written.")


if __name__ == "__main__":
    main()
