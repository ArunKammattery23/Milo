# Milo expanded action dataset — synthetic review candidate

Target: `google/functiongemma-270m-it`. Source: `CHATGPT-DATASET-PROMPT.md`; prepared 15–16 October 2026. This dataset teaches action selection and argument extraction, not speech synthesis or a human voice. No model was trained as part of this delivery.

## Deliverables

- `release/milo-chatgpt.jsonl`: the complete 46-batch, 2,300-example corpus in the requested raw format.
- `release/train.jsonl`, `validation.jsonl`, `test.jsonl`: conversations plus the full 21-tool expanded schema, using FunctionGemma-style function objects.
- `release/*.completion.jsonl`: prompt/completion variants. Only the final assistant response is the completion; earlier assistant turns are context, not targets.
- `release/*.raw.jsonl`: inspectable raw split rows, preserving IDs and friendly `say` text.
- `release/challenge*.jsonl`: 24 additional manually composed contrast examples; keep out of training.
- `release/provenance.jsonl`: row IDs, semantic/template family, category, split and review status.
- `release/confirmations.jsonl`: spoken confirmations separated from action targets.
- `release/tools.json`, validation reports, checksums and reproducible scripts.
- `SOURCE-SPEC.md`: snapshot of the requested source document for reproducibility.

The exact split totals and distributions are in `validation-report.json`. Allocation targets 80/10/10 while keeping complete generation families together, then moves whole families to improve held-out tool coverage. The report lists tools absent from each split; a tool with only two independent families cannot appear in all three splits. Languages in the base corpus are 1,150 Malayalam, 690 English and 460 mixed. Shards use explicitly authored scenarios and deterministic slot/template expansion. These are synthetic data, not 2,300 independent human utterances or proof of model accuracy.

## Runtime compatibility — do not blindly merge into the existing data

The source prompt specifies 21 tools. The current `MiloAgent`/`build_dataset.py` contract exposes six: `set_reminder`, `list_reminders`, `cancel_reminder`, `open_app`, `phone_status`, and `remember_name`.

Expanded `show_plan(day)` replaces `list_reminders()`. Expanded `cancel_reminder(item)` differs from current `cancel_reminder(task)`. Expanded reminders include `repeat`; expanded missing-detail replies use `Ask: ...` rather than `Need time.`. New alarms, events, notes, lists, messaging, contacts, settings, navigation, media, search and emergencies require executor implementation and validation. This delivery does not silently change the Android app or existing training files.

## Important specification decisions

1. A message or email tool prepares a draft, and a call tool prepares a call screen. `say` must not assert successful delivery or an answered call. It is a success-path UX fixture, not a pre-execution model promise. Only play it after the app reports the corresponding success/draft-ready result. On failure, use a failure response instead.
2. The original prompt includes a birthday message to be sent at midnight, but `send_message` has no scheduling argument. We exclude that unsupported action rather than teach an immediate send or invent parameters. Add a scheduling capability before training such cases.
3. Do not use generated `say` strings as the FunctionGemma training completion. Ordinary `No action.` rows have an empty `say` and hand off to the conversation model.
4. Dates are relative to the row's `now` in Asia/Kolkata. The converted developer message includes that date, weekday and timezone, otherwise calendar-year targets would be unknowable from the input.
5. The spec defaults a part of day to a fixed hour (morning 9 am, afternoon 2 pm, evening 6 pm, night 9 pm). Bare-hour choices follow the existing runtime. These are product conventions, not universally correct interpretations; use the same rules at inference.
6. `set_alarm` must have exactly one of `time` or `duration`, despite its loose JSON schema. Timer durations accept positive seconds, minutes, hours, or hours plus minutes; implement those units in the new timer executor. Toggle settings need explicit `on`; volume up/down do not. The validator enforces these extra invariants.
7. `Not allowed.` encodes Milo's narrow product capability limits. It is not a general claim that every excluded action is inherently unsafe. Ordinary educational questions mentioning money, OTPs or settings can still be `No action.`.

## Validation and remaining review

Automated checks cover required/extra fields, tool names, types, enums, nonempty inputs, fixed replies, date validity and weekday agreement, canonical time syntax, 12-word `say` limit, language quotas, exact input duplicates, template-family split isolation and conversion shape. The Java time harness extracts and executes the actual `MiloTasks.parseWhen` method in Asia/Kolkata, comparing each time result with an independent calculation. It does not test Android tool execution.

Automated schema checks do not prove intent labels or Malayalam fluency. A fluent Malayalam reviewer must review final sentences, contact inflections, code-mixing, temporal phrases and task forms before training. Template family isolation reduces leakage but does not eliminate semantic similarity. Do not merge previous generated train/eval files without a new cross-corpus leakage audit.

The model/tokenizer was not downloaded and tokenized lengths were not measured. Run the accepted model's official chat template with the `tools` argument, inspect multi-turn tool-result rendering, and measure lengths before training. Use completion-only loss for `*.completion.jsonl`; do not truncate away the final target or relevant earlier turns. The full schema may consume a substantial fraction of a small context budget.

## Reproduce

Requires Python 3.10+ and Java 17 for runtime time checks; no Python packages required for dataset construction.

```powershell
python parts/build_scheduling.py
python parts/build_actions.py
python parts/build_edges.py
python assemble_dataset.py --spec SOURCE-SPEC.md
python build_challenge.py
python -m unittest discover -v
python check_android_times.py --source ..\..\..\android\app\src\main\java\life\milo\companion\MiloTasks.java --data release\milo-chatgpt.jsonl
python make_review.py
```

Use absolute paths for `--spec` and `--source` if this folder is stored elsewhere. Compare final report hashes to the source revision used for training. Preserve the provenance IDs outside model input.

## Evaluate after fine-tuning

Report results separately for Malayalam, English, mixed language, single-turn, multi-turn, tool selection, argument exact match, no-action false positives, missing-detail accuracy and refused-action false negatives. Score ordered multi-call sequences, not only the first call. Inspect disagreement cases manually. Use validation for model selection; open the test and challenge scores only for the final candidate. Do not claim a device latency or accuracy improvement before running inference on the intended phone.

Official references:

- https://huggingface.co/google/functiongemma-270m-it
- https://huggingface.co/datasets/google/mobile-actions
- https://ai.google.dev/gemma/docs/mobile-actions

All examples and names are synthetic. Do not replace placeholders with customer data or publish private conversation logs in this corpus.
