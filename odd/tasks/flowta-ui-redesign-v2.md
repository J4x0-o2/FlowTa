# FlowTa UI redesign v2

## Objective
Ship the approved UI adjustments: two quick fixes (bottom bar insets, in-field thousands
separator) plus the redesigned history cards, grouped feed bubbles and calendar-period metrics.

## Problem / Why
- The "Registrar transacción..." bar overlaps the Android gesture bar (edge-to-edge without insets).
- `AmountInput` shows the formatted amount as helper text below the field instead of in the field.
- History cards use two thin bars scaled against the biggest sibling period: hard to read.
- The feed repeats bubbles of the same type; metrics use rolling ranges and lack a Day view.

## Design source
Canvas (approved 2026-11-12): https://claude.ai/artifact/AryAEEc6vtroWWFsPMfefW
Artboards: FeedGrouped, FeedExpanded, Main (Metrics Day), MetricsWeek, MetricsMonth,
HistoryCardsYear, HistoryCardsMonth. History A/B/C artboards are NOT the chosen design.

## Scope (authorized)
- T0a: `BottomInputBar` respects navigation-bar insets (background still drawn behind the gesture bar).
- T0b: `AmountInput` formats thousands inside the field while typing (VisualTransformation);
  no helper text below. Stored value stays raw. COP: thousands only; USD/EUR keep decimals.
  Applies to every `AmountInput` caller (transaction, new account balance + goal, account detail).
- T1: History year/month cards: net + "Ingresos"/"Gastos" text rows + ONE bar "Gastaste el X%
  de lo que ganaste"; overspent = full red bar "Gastaste $X más de lo que ganaste". Each period
  stands alone (no cross-period scaling).
- T2: Feed grouping: 2+ consecutive movements of the same flow type collapse into one bubble
  (×N, total, categories, time range); another type breaks the streak; tap expands.
- T3: Metrics base: tabs Día/Semana/Mes/Año over calendar periods with ‹ › stepper.
  Day = net only. Week = net + delta vs previous week, income/expense tiles, income-vs-expense
  bars per day (tap shows that day's income and expenses), expenses by category, cards
  Promedio diario / Día más caro / Ahorro %.
- T4: Metrics Month (net, savings ring, expenses by category donut, income by source; NO
  "Neto acumulado") and Year (same layout as Month over the year). Fix net icon always TrendingDown.

## Constraints
- No schema change (stable category colors derived from category id).
- Do not upgrade AGP; keep SQLDelight workarounds. JAVA_HOME = Android Studio jbr.
- Conventional commits, one work-unit commit per task on `feat/ui-redesign-v2`. No push/PR without user.

## TDD
- Mode: strict (source: user global CLAUDE.md "Strict TDD Mode: enabled").
- Runner: `./gradlew test` (focused: `./gradlew testDebugUnitTest --tests <pattern>`).

## Checklist
- [x] T0a Bottom bar above gesture bar — route: inline (1 file). Commit 8a41345. assembleDebug green.
  No unit test possible (pure inset modifier, no androidTest setup) — TDD N/A, disclosed.
  RDD assess: medium, review_due=false (under_budget).
- [x] T0b Thousands separator inside AmountInput — route: delegated (writer trigger: 4 files). Commit a438881.
  RED (unresolved refs) → GREEN; AmountGroupingTest 11 tests; test + assembleDebug green; parent spot check green.
  Decisions: raw value has no grouping chars; COP digits only; USD/EUR one decimal mark (normalized '.'),
  max 2 decimals; pasted "1,234.56" in USD becomes 1.23 (accepted edge). Error text only when invalid.
  RDD assess: medium, review_due=false (under_budget, 301 lines).
- [x] T1 History cards — route: delegated (writer trigger: 4 files). Commit 444e13c.
  RED (unresolved SpendShare) → GREEN; History tests + test + assembleDebug green; parent spot check green.
  SpendShare sealed (None/Within/Over); cross-period scaling removed; FlowColors.spentFill added.
  Follow-up: HistoryAggregator.barFraction now unused (left with its test).
  RDD assess: medium, review_due=true (slice_budget_reached, 478 lines vs main) → user GRANTED.
  Review lineage review-42f4d5fd41569b47 (main..444e13c): APPROVED, acknowledged, authority burned.
  Reviewed boundary is now 444e13c. Advisory (non-blocking) follow-ups:
  - R3-cop-paste-decimal-inflates (AmountGrouping.kt:24): pasting "1234.56" in COP strips the mark → 123456.
  - R3-paste-grouped-usd-truncates (AmountGrouping.kt:40-43): pasting "1,234.56" in USD → 1.23.
  - R3-trailing-mark-silent-disabled (AmountInput.kt:36): "12." disables submit with no hint.
  - R3-float-exact-equality-tests (HistoryModelsTest.kt:130-132): use a delta for float asserts.
- [x] T2 Grouped feed bubbles — route: delegated (writer trigger: 7 files). Commit fe5e3ad.
  RED (unresolved groupFeed/bubbleRange) → GREEN; FeedGroupingTest 11 + Formatters 2 + CategorySummary 2;
  test + assembleDebug green; parent spot check green. Not run on device.
  Decisions: group key = group_<oldest id>; expansion in rememberSaveable; 12h time range; >3 categories
  → "A · B · +N más"; transfers keep no-op tap. RDD assess vs 444e13c: medium, 509 lines, review due → user GRANTED.
  Review lineage review-cc46b1f5d988ebde (444e13c..fe5e3ad): APPROVED, acknowledged, burned. Boundary = fe5e3ad.
  Advisory follow-ups: R3-stale-expansion-keys (TransactionScreen.kt:62-64, saved keys never pruned);
  R3-key-anchor-deletion (FeedGrouping.kt:46, deleting the oldest item changes the group key → collapses);
  R3-summary-boundary (GroupedBubble.kt:167-170, category summary edge at exactly 3).
- [ ] T3 Metrics base + Day + Week — route: delegated
- [ ] T4 Metrics Month + Year — route: delegated

## Acceptance
- `./gradlew test` and `./gradlew assembleDebug` green after each task.
- Each task matches its artboard on the canvas.

## Progress / evidence
- 2026-11-12: branch `feat/ui-redesign-v2` created from main 742fade. RDD: on (global).

## Next step
T0a.
