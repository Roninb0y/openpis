# Player playback continuity implementation plan

> Execution: superpowers:executing-plans, with regression tests written before the fix.

**Goal:** Keep single-item loops and timed image/text playback working across periodic plan refreshes.

**Architecture:** Keep the existing Vue player and API. Compare the playlist's playback content rather than object identity; explicitly start each playback cycle, including a one-item loop. Reset only the changed module's playlist. Invalidate pending subtitle loads when their cycle ends.

**Tech stack:** Vue 3, TypeScript, Vite, Playwright Chromium.

**Spec:** The user authorized one focused optimization based on the preceding main-branch review. Acceptance: a single video repeats, 60-second image/text items advance despite 30-second polling, and a shortened/changed playlist remains playable. No backend or visual redesign.

## Constraints and decisions

- Preserve video natural-end behavior, API shapes, screen layout, heartbeat and polling intervals.
- Use real browser rendering with controlled API/media fixtures; do not touch a live database.
- An unchanged playlist keeps its current cycle. A changed playlist restarts at its first item.
- Record logs once per cycle; unchanged refreshes must not create extra log entries.
- Keep failures bounded: a broken single media item must not cause a tight retry loop.

## Review focus

- Single video/image/text item repeats and produces distinct cycles.
- Polling with identical JSON does not restart timers or split playback logs.
- Shortened or empty playlists cannot leave an out-of-range index.
- A late subtitle response cannot replace newer content or restart a retired timer.
- Module-only geometry changes preserve playback; actual playlist changes restart it.

## Task 1: Playback regression and fix

- [x] Add Playwright config, focused browser scenarios and test commands.
- [x] Run against main's player and capture the expected failures.
- [x] Modify ModulePlayer.vue to explicitly manage cycles and semantic playlist changes.
- [x] Run the whole browser suite, including actual video playback and responsive rendering.

## Task 2: Verification and delivery

- [x] Add the browser regression command to frontend CI and document local execution.
- [x] Run frontend build and diff checks; inspect a browser screenshot.
- [x] Obtain a fresh review and address material findings within this scope.
- [ ] Commit only related files, push fix/player-playback-continuity, open a PR to main.

## Verification record

- Original player: 6 failed / 2 passed browser cases; both additional edge cases also failed as expected.
- Fixed player: 10 browser cases passed. Production build passed with the pre-existing large-chunk warning.
- Inspected desktop (1280x720) and portrait (390x844) browser screenshots.
- Independent review found no production regression; strengthened the video test to require two real `ended` events and zero media errors.
- Mutation check: deliberately invalid video bytes now fail the video test (zero ended events); restored the generated valid WebM afterward.
- Deferred nonblocking test coverage: single-subtitle repetition and delayed subtitle responses after component unmount have no dedicated scenarios yet. Playlist replacement and single-image/video repetition are covered.
- No backend code, database schema, authentication, deployment settings, or business data changed.
