---
description: Senior-engineer code review of BrandX — security findings first, then performance.
---

Run a senior-engineer code review using the `senior-reviewer` agent.

Target: $ARGUMENTS (if empty, use `git diff main...HEAD`).

Subagents cannot call `AskUserQuestion`, so the scope choice has to be made here, in the
main session, before the agent is spawned.

**Step 1 — resolve the scope.** If `$ARGUMENTS` already contains `diff` or `audit`, use it
and skip the question. Otherwise call `AskUserQuestion` with these two options:

- **Diff-focused (Recommended)** — Reviews the changed lines, plus up to 3 short notes on
  pre-existing risk where the diff lands on an unprotected or N+1-prone path. Fast and
  actionable.
- **Full-context audit** — Reviews the diff and re-audits the controllers, services,
  repositories, entities and templates it touches. Thorough, slower, noisier.

**Step 2 — delegate.** Spawn the `senior-reviewer` agent via the `Agent` tool with
`subagent_type: senior-reviewer`, and pass it:

- the scope word (`diff` or `audit`) — required, the agent stops and asks without it
- the review target
- any extra instructions the user gave in `$ARGUMENTS`

**Step 3 — relay.** Report the agent's findings to the user in the order it returned them.
Do not re-rank them, do not soften a severity, and do not drop findings you disagree with —
say you disagree and why, underneath the finding. The agent is read-only; if the user wants
the findings fixed, that is a separate step you do yourself after they ask.
