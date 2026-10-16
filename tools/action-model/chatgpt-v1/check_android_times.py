"""Execute the actual checked-out MiloTasks.parseWhen without Android dependencies."""
import argparse
from datetime import datetime, timedelta, timezone
import hashlib
import json
from pathlib import Path
import re
import subprocess

ZONE = timezone(timedelta(hours=5, minutes=30))


def expected_time(value, now):
    if re.fullmatch(r"\d{4}-\d{2}-\d{2} \d{2}:\d{2}", value):
        return datetime.strptime(value, "%Y-%m-%d %H:%M").replace(tzinfo=ZONE)
    if value.startswith("in "):
        amounts = re.findall(r"(\d+) (hours?|minutes?)", value)
        return now + timedelta(minutes=sum(int(n)*(60 if u.startswith("hour") else 1) for n,u in amounts))
    match = re.fullmatch(r"(?:(today|tomorrow|day after tomorrow|monday|tuesday|wednesday|thursday|friday|saturday|sunday) )?(\d{1,2})(?::(\d{2}))?(?: (am|pm))?", value)
    day, h, minute, ap = match.groups()
    hour = int(h)
    if ap:
        hour = hour % 12 + (12 if ap == "pm" else 0)
    elif day and 1 <= hour <= 6:
        hour += 12  # Current Android runtime policy for a bare hour on a named day.
    candidate = now.replace(hour=hour, minute=int(minute or 0), second=0, microsecond=0)
    weekdays = ["monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday"]
    if day in weekdays:
        candidate += timedelta(days=(weekdays.index(day)-now.weekday()) % 7)
        if candidate <= now:
            candidate += timedelta(days=7)
    elif day == "tomorrow":
        candidate += timedelta(days=1)
    elif day == "day after tomorrow":
        candidate += timedelta(days=2)
    elif not day:
        step = timedelta(hours=12 if not ap and 1 <= hour <= 11 else 24)
        while candidate <= now:
            candidate += step
    return candidate


def main():
    p = argparse.ArgumentParser()
    p.add_argument("--source", required=True, type=Path)
    p.add_argument("--data", required=True, type=Path)
    args = p.parse_args()
    text = args.source.read_text(encoding="utf-8-sig")
    start = text.index("    private static final Pattern IN=")
    code = text[start:text.rfind("}")]
    work = args.data.parent / "time-check"
    work.mkdir(exist_ok=True)
    harness = """import java.util.*;
import java.util.regex.*;
import java.io.*;
public class TimeProbe {
CODE
public static void main(String[] args) throws Exception {
  TimeZone.setDefault(TimeZone.getTimeZone("Asia/Kolkata"));
  BufferedReader reader=new BufferedReader(new InputStreamReader(System.in, java.nio.charset.StandardCharsets.UTF_8));
  String line;
  while((line=reader.readLine())!=null){String[] p=line.split("\\t",3); System.out.println(p[0]+"\\t"+parseWhen(p[2],Long.parseLong(p[1])));}
}
}
""".replace("CODE", code)
    (work / "TimeProbe.java").write_text(harness, encoding="utf-8")
    subprocess.run(["javac", "-encoding", "UTF-8", str(work / "TimeProbe.java")], check=True, capture_output=True)
    cases = {}
    for line in args.data.read_text(encoding="utf-8").splitlines():
        row = json.loads(line)
        now = datetime.strptime(row["now"].split()[0], "%Y-%m-%dT%H:%M").replace(tzinfo=ZONE)
        for i, message in enumerate(row["messages"]):
            for j, call in enumerate(message.get("tool_calls", [])):
                if "time" in call["arguments"]:
                    key = f"{row['id']}:{i}:{j}"
                    cases[key] = (now, call["arguments"]["time"])
    input_data = "".join(f"{k}\t{int(now.timestamp()*1000)}\t{value}\n" for k,(now,value) in cases.items())
    result = subprocess.run(["java", "-cp", str(work), "TimeProbe"], input=input_data, encoding="utf-8", capture_output=True, check=True)
    failures = []
    received = set()
    for line in result.stdout.splitlines():
        key, number = line.split("\t")
        if key not in cases or key in received:
            raise ValueError("Unexpected or duplicate runtime output: " + key)
        received.add(key)
        now, value = cases[key]
        parsed = int(number)
        expected = int(expected_time(value, now).timestamp()*1000)
        if parsed <= int(now.timestamp()*1000) or parsed != expected:
            failures.append({"case": key, "now": now.isoformat(), "time": value, "runtime": parsed, "expected": expected})
    if received != set(cases):
        raise ValueError("Runtime omitted some test cases")
    report = {"cases": len(cases), "failures": failures, "timezone": "Asia/Kolkata", "parser_source_sha256": hashlib.sha256(args.source.read_bytes()).hexdigest(), "scope": "Every time argument in the corpus; other expanded tools still need runtime implementation."}
    (args.data.parent / "android-time-report.json").write_text(json.dumps(report, ensure_ascii=False, indent=2)+"\n", encoding="utf-8")
    print(json.dumps(report, ensure_ascii=False, indent=2))
    raise SystemExit(bool(failures))


if __name__ == "__main__":
    main()
