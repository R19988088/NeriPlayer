# Repository Instructions

## Scope
- These instructions apply to the whole repository.
- User instructions in the current conversation override this file.

## Workflow
- Read the relevant code and callers before editing.
- Keep changes scoped to the requested behavior.
- Prefer existing project patterns, Compose components, Gradle tasks, and Android APIs.
- If behavior, business meaning, or visual expectations are unclear, ask before coding.
- Do not hide problems with cosmetic fallbacks, silent catches, fake success states, or broad masking logic.

## UI Standard
- Balance aesthetics and usability first.
- Match the existing NeriPlayer visual language unless the user asks for a redesign.
- Build real usable states: loading, empty, error, disabled, and content where relevant.
- Avoid adding speculative settings, abstractions, or feature hooks.

## Verification
- After code changes, run the smallest relevant local check, such as:
  - `./gradlew :app:assembleDebug`
  - `./gradlew :app:testDebugUnitTest`
  - a narrower Gradle task when it directly covers the change
- Report any check that could not be run.

## Push And Actions
- After modifications are ready, push the branch.
- Then verify GitHub Actions at:
  <https://github.com/R19988088/NeriPlayer/actions>
- Do not call the task done until the relevant workflow result is checked, or explicitly report why it could not be checked.
