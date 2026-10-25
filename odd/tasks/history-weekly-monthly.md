# Feature: History — weekly, monthly and yearly totals

## Objective
Show income + expense totals per week and per month, plus the yearly total, modeled on MiCuadre's
History (year screen → month drill-down by week).

## Problem / why
The user needs to track their economy by calendar period. `MetricsScreen` only offers rolling
7/30-day windows and groups days in UTC, so period totals near midnight (Colombia, UTC-5) land on
the wrong day/week/month.

## Scope
- New route `History` (year) and `HistoryMonth/{year}/{month}` (weeks of a month).
- Entry: calendar icon in the `TransactionScreen` top bar, next to Metrics and Profile.
- Totals: income, expense, net (= income − expense), movement count. Transfers excluded.
- Weeks Monday–Sunday, clipped to the month. Current month lists weeks up to the one containing today.
- Grouping in local time (`ZoneId.systemDefault()`), done in Kotlin, not SQL.
- Year stepper: previous disabled when no earlier data; next disabled at current year.
- Months without movements are omitted; empty year shows "Aún no hay movimientos en {year}".
- Bars per card (income green, expense red) scaled to the best sibling (month of year / week of month).
- Approved mockup: https://claude.ai/artifact/XMb2TpbDL8dByieAwLEXxz

## Constraints
- No schema change (new queries only → no migration).
- Follow project conventions in `CLAUDE.md` (MVI State/Intent, use cases with `operator fun invoke`,
  `LocalCurrencySymbol`, `Icons.AutoMirrored`, theme colors so dark mode works).
- UI copy in Spanish (existing project language).

## TDD
- Mode: strict, enabled (source: user global instructions "Strict TDD Mode: enabled").
- Runner: `./gradlew testDebugUnitTest` (JAVA_HOME = Android Studio jbr).

## Tasks
- [x] T1 Domain: `HistoryAggregator` (year → months, month → weeks) + models, pure Kotlin, unit-tested
      (week clipping, current month cut, local-zone boundaries, omitted empty months, bar fractions).
- [ ] T2 Data: `Transaction.sq` query for transactions in a range + earliest date; repository and use cases
      `ObserveYearHistoryUseCase` / `ObserveMonthHistoryUseCase`; tests with SQLite driver.
- [ ] T3 Presentation: `HistoryViewModel`, `HistoryScreen`, `HistoryMonthScreen`, routes, top-bar icon;
      `assembleDebug` green.

## Route per task
- T1–T3: delegated direct (writer trigger: 2+ non-trivial files per task).

## Delivery
- Branch `feat/history-weekly-monthly`, based on `perf/ui-animations-and-db-migrations` (unmerged).
- Strategy: ask-on-risk. Forecast ~700 authored lines across 3 work-unit commits.

## Progress / evidence
_(filled per task: commit, checks, review outcome; a task's own hash is recorded in the next task's entry, since the doc ships inside the commit)_

### T1 Domain (done)
- Commit: `feat(history): add period aggregator for weekly and monthly totals` (hash recorded in T2 entry).
- Files: `feature/history/domain/model/History.kt`, `HistoryAggregatorTest.kt`.
- RED: with models and a `TODO()` aggregator, `./gradlew testDebugUnitTest --tests '*HistoryAggregatorTest*'`: 17 tests, 16 failed (`kotlin.NotImplementedError`); only the pure `PeriodTotals.net` test passed.
- GREEN: `./gradlew testDebugUnitTest`: BUILD SUCCESSFUL (all unit tests).
- Refactor: none needed.

## Next step
T2.
