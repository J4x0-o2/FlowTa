# UI performance and database migrations

## Objective
Make FlowTa animations and scrolling feel light, and introduce real SQLDelight migrations so the
database schema can evolve without wiping user data (prerequisite for the MiCuadre integration).

## Problem / why
- Animations feel heavy: long navigation transitions (320 ms slide+fade), bouncy low-stiffness
  press spring, triple-animated balance header, formatters allocated on every recomposition,
  composition-phase computations without `remember`, no `contentType` in lazy lists.
- No release tooling for runtime performance (no profileinstaller, no shrinkResources) and no
  easy way to feel a release-like build locally.
- No `.sqm` migrations: the schema is recreated on install, so any schema change breaks
  existing installs. The upcoming integration needs new columns.

## Scope
In: FlowTa only. Performance fixes from the audit, release-like build type, SQLDelight
migration infrastructure (schema v1 snapshot, verifyMigrations, migration test harness), docs.
Out: MiCuadre changes, integration schema columns (`source_app`, `external_id`), baseline
profile generation (needs a macrobenchmark module + device; follow-up).

## Constraints
- Do not upgrade AGP to 9.x; keep SQLDelight/KSP workarounds in `app/build.gradle.kts`
  (any new build type needs matching `sourceSets` + `afterEvaluate` entries).
- Follow CLAUDE.md conventions (MVI, use cases, Conventional Commits, no AI attribution).
- Build with `JAVA_HOME` = Android Studio `jbr`.

## TDD
- Mode: strict, ON (source: user global CLAUDE.md "Strict TDD Mode: enabled").
- Runner: `./gradlew testDebugUnitTest` (JUnit4 + MockK + Turbine + SQLDelight sqlite driver).
- Pure UI tuning (durations, springs) has no meaningful unit test; covered by build + manual check.

## Tasks
- [x] T1 Cached formatters: extract money/date formatting into shared, cached formatters in
  `core` with unit tests (RED first); replace per-call `SimpleDateFormat`/`DecimalFormat`
  creation in TransactionBubble, TransactionDetailBottomSheet, TransactionScreen, AccountCard,
  AccountDetailScreen, ProfileScreen, MetricsScreen.
- [x] T2 Animation tuning: NavGraph transitions ~200 ms with FastOutSlowInEasing; bubble press
  spring no-bounce/medium stiffness read in `graphicsLayer {}`; simplify balance header
  animation (~200 ms, no stacked AnimatedContent+color).
- [ ] T3 Composition cost: `remember` for selectedTransaction lookup and metrics chart
  calculations (extract pure calc functions + tests where logic moves); `contentType` in
  LazyColumns; `collectAsStateWithLifecycle` in MainActivity/Settings/Profile.
- [ ] T4 Release tooling: `isShrinkResources = true`, `androidx.profileinstaller`, `staging`
  build type (initWith release, debug signing, local only) with SQLDelight workarounds.
- [ ] T5 Migrations: freeze schema v1 (`.db` snapshot under `src/main/sqldelight/databases`),
  enable `verifyMigrations`, add migration verification test, update CLAUDE.md
  ("No hay migraciones" section) with the migration workflow.

## Route declaration
- T1–T5: delegated direct (one writer; 2+ non-trivial files; mapping already done by audit).

## Acceptance criteria
- `./gradlew testDebugUnitTest` and `./gradlew assembleDebug` pass; `assembleStaging` builds.
- No formatter allocation inside composables; transitions ≤ 220 ms.
- `./gradlew verifySqlDelightMigration` (or equivalent task) passes with schema v1 committed.

## Progress / evidence
- Base WIP saved: `3471983 chore: save in-progress UI work` on branch
  `perf/ui-animations-and-db-migrations` (from `main` @ `fd9fcaf`).
- RDD: on (global). Review per work-unit commit via `gentle-ai review assess`.
- T1 (commit: see git log `perf: cache formatters`): RED `./gradlew testDebugUnitTest` -> 6 of 6 FormattersTest failed with kotlin.NotImplementedError (stub); GREEN after implementation: `./gradlew testDebugUnitTest` BUILD SUCCESSFUL, FormattersTest 6 tests 0 failures; `./gradlew testDebugUnitTest assembleDebug` BUILD SUCCESSFUL. Decision: DecimalFormat via ThreadLocal; java.time formatters cached, zone applied per call.
- T2: NavGraph tween 320->200 ms FastOutSlowInEasing (slide + fade kept); bubble press spring NoBouncy/StiffnessMedium via graphicsLayer reading State in lambda; balance header = single tween(200) color animation (AnimatedContent removed, number changes instantly). Checks: `./gradlew testDebugUnitTest assembleDebug` BUILD SUCCESSFUL; `assembleDebug --rerun-tasks` compileDebugKotlin BUILD SUCCESSFUL. Manual feel check on device pending.

## Next step
T3.
