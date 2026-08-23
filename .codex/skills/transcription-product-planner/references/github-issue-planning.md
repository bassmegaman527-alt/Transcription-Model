# GitHub issue planning

Read this reference only when the user explicitly asks to create or update a planning issue.

## Before writing

1. Inspect the latest intended `main`, `AGENTS.md`, relevant source, active issues, recently completed issues, and relevant merged PRs.
2. Identify the single selected development direction and confirm it does not duplicate completed or active work.
3. Resolve platform uncertainty before presenting implementation assumptions as acceptance criteria.

## Issue contents

Create one focused issue or an ordered checklist containing:

- **Goal:** the user outcome.
- **Product principle:** how it supports low-friction capture or later thought development.
- **Current baseline:** verified behavior only.
- **Ordered checklist:** unchecked, deliberately sequenced, one focused behavior per item.
- **Acceptance principles:** boundaries shared by all items.
- **Out of scope:** adjacent features and refactors intentionally excluded.
- **Verification expectations:** automated and device checks appropriate to each behavior.
- **Development rule:** one selected checklist item per independently mergeable PR.

Do not overload the issue with speculative implementation detail. Name a concrete architecture or dependency only when repository evidence or platform constraints require it.

## Review after mutation

Fetch the issue again and review it exactly as stored on GitHub. Check for ambiguous wording, excessive scope, duplicate work, unsupported implementation assumptions, missing verification, and missing platform constraints. Correct the issue if necessary, fetch it again when corrected, report whether it is ready for implementation, and stop. Do not begin implementation automatically.
