import copy
import json
from pathlib import Path
import re
import unittest
from assemble_dataset import validate_row, split_groups, convert


class ValidationTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        path = Path(__file__).parent / "tools.json"
        cls.tools = json.loads(path.read_text(encoding="utf-8"))
        cls.schemas = {x["function"]["name"]: x["function"] for x in cls.tools}

    def row(self):
        return {"id":"1-01", "lang":"en", "now":"2026-10-15T08:00 Thursday", "messages":[{"role":"user", "content":"Remind me to read at 7 pm"}, {"role":"assistant", "tool_calls":[{"name":"set_reminder", "arguments":{"task":"read", "time":"7 pm"}}]}], "say":"I'll remind you at 7 pm."}

    def test_valid(self):
        validate_row(self.row(), self.schemas)

    def test_unknown_tool_and_argument_rejected(self):
        for change in ("tool", "argument"):
            row = self.row()
            call = row["messages"][-1]["tool_calls"][0]
            if change == "tool": call["name"] = "send_money"
            else: call["arguments"]["invented"] = "x"
            with self.assertRaises(ValueError): validate_row(row, self.schemas)

    def test_string_boolean_rejected(self):
        row = self.row()
        row["messages"][-1]["tool_calls"] = [{"name":"phone_setting", "arguments":{"setting":"wifi", "on":"false"}}]
        with self.assertRaises(ValueError): validate_row(row, self.schemas)

    def test_invalid_date_and_weekday_rejected(self):
        row = self.row()
        row["messages"][-1]["tool_calls"][0]["arguments"]["time"] = "2027-02-29 09:00"
        with self.assertRaises(ValueError): validate_row(row, self.schemas)
        row = self.row(); row["now"] = "2026-10-15T08:00 Friday"
        with self.assertRaises(ValueError): validate_row(row, self.schemas)

    def test_empty_alarm_rejected(self):
        row = self.row()
        row["messages"][-1]["tool_calls"] = [{"name":"set_alarm", "arguments":{}}]
        with self.assertRaises(ValueError): validate_row(row, self.schemas)

    def test_conversion_retains_time_context_but_not_say(self):
        converted = convert(self.row(), self.tools)
        self.assertIn("2026-10-15", converted["messages"][0]["content"])
        self.assertNotIn("say", converted)
        self.assertEqual(converted["messages"][-1]["tool_calls"][0]["function"]["name"], "set_reminder")

    def test_timer_duration_and_alarm_exclusivity(self):
        row = self.row()
        call = {"name":"set_alarm", "arguments":{"duration":"45 seconds"}}
        row["messages"][-1]["tool_calls"] = [call]
        validate_row(row, self.schemas)
        for duration in ("0 seconds", "-1 minutes", "some time"):
            call["arguments"] = {"duration":duration}
            with self.assertRaises(ValueError): validate_row(row, self.schemas)
        call["arguments"] = {"duration":"10 minutes", "time":"7 pm"}
        with self.assertRaises(ValueError): validate_row(row, self.schemas)

    def test_error_context_requires_matching_prior_call(self):
        row = self.row()
        row["messages"] = [{"role":"user", "content":"Call Ramu"},
            {"role":"assistant", "tool_calls":[{"name":"call_contact", "arguments":{"contact":"Ramu"}}]},
            {"role":"tool", "name":"call_contact", "content":"Error: no contact called Ramu"},
            {"role":"assistant", "content":"Ask: contact"}]
        row["say"] = "Which contact did you mean?"
        validate_row(row, self.schemas)
        row["messages"][2]["name"] = "send_message"
        with self.assertRaises(ValueError): validate_row(row, self.schemas)

    def test_no_action_cannot_include_spoken_confirmation(self):
        row = self.row()
        row["messages"][-1] = {"role":"assistant", "content":"No action."}
        with self.assertRaises(ValueError): validate_row(row, self.schemas)

    def test_family_cannot_cross_splits(self):
        rows, meta = [], {}
        for i in range(30):
            row = self.row(); row["id"] = str(i)
            rows.append(row); meta[str(i)] = {"family":str(i//3), "category":"reminders"}
        assignments = split_groups(rows, meta)
        for i in range(0,30,3):
            self.assertEqual(len({assignments[str(j)] for j in range(i,i+3)}),1)


if __name__ == "__main__":
    unittest.main()
