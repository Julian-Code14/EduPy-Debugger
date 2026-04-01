# AGENTS.md

## Branching
- Never work directly on `main` or `dev`.
- Start every task from `dev`.
- Create a dedicated task branch for each task.
- Follow the branch naming convention configured in Codex/Git settings.
- Open pull requests against `dev`.

## Commits
- Use Conventional Commits.
- Allowed types: feat, fix, refactor, docs, test, chore, ci, build, perf.
- Format: `<type>(<scope>): <summary>`

## Safety
- Never force-push unless explicitly requested.
- Keep changes scoped to the task.
- Do not change unrelated files.

## Validation
Before finishing:
- run tests relevant to the change
- run lint if configured
- run typecheck if configured
- summarize risks or follow-up work