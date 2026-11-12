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
- [x] T3 Metrics base + Day + Week — route: delegated (writer trigger: 10+ files). Commit 0753391.
  RED (unresolved MetricsPeriod/Aggregator/...) → GREEN; 66 metrics tests; test + assembleDebug green;
  parent spot check green. Emulator OK (2026-11-12): Day = only "Neto del día"; Week 19–25 oct matches
  history (630,000 / 273,500 / +356,500), delta −11% vs prev (398,500), tiles +5% / +36%, tap D shows
  "Dom 25 · $0 · −$254,000", categories 80/20, avg $39,071, most expensive Domingo, ahorro 57%.
  Decisions: default tab Día; offset in SavedStateHandle; delta % over |previous|; Spanish names in MetricsText.
  Left for T4: Month/Year are NetCard placeholders; income-by-source needs a new query; unused
  incomeVsExpenseByDay SQL + getTotalTransfers/getNetBalance remain. Ahorro value not green-tinted (design was).
  RDD vs fe5e3ad: medium, 2164 lines, due → user GRANTED. Review review-1676d95cabd12491: APPROVED,
  acknowledged, burned. Boundary = 0753391. Advisory: R3-stale-guard-rapid-taps (MetricsViewModel.kt:67-68,
  rapid ‹ taps read a stale canGoBack); R3-day-skips-category-query-untested (ObserveMetricsUseCase.kt:32-37).
- [x] T4 Metrics Month + Year — route: delegated. Commits 2bdc6e7 + fix 1f382e7 (new query incomeBySourceInRange, no schema
  change; removed incomeVsExpenseByDay + getNetBalance/getTotalTransfers). Tests + assembleDebug green.
  Emulator 2026-11-12: Oct 2026 matches history (4,730,000 / 2,422,500 / +2,307,500), ring 49%, +64% vs Sep,
  sources Salario 74% / Freelance 26%; Year 2026 matches (40,580,000 / 24,704,500), ring 39%.
  BUG found on emulator: hash-based category colors collide (Hogar = Alimentación purple; Salud ≈ Ropa orange).
  Reopened → fix 1f382e7: slots by order of all expense types (created_at, id), 12 distinct hues, sources
  8 greens, unknown/no source gray; deleting a category shifts later colors (accepted). RED → GREEN; test
  (296) + assembleDebug green. Emulator re-check: Year 7 categories all distinct; Week shows the same
  colors (Alimentación teal, Transporte green) as Month/Year.
  RDD vs 0753391: medium, 1013 lines, due → user GRANTED. Review review-442aa5b2b79b3a74: APPROVED,
  acknowledged, burned. Boundary = 1f382e7. Advisory: R3-compact-money-rounding-boundary (MetricsText.kt:115,
  999,999 → "$1000 mil" instead of "$1 M"); R3-income-bar-zero-weight (MonthYearMetricsContent.kt:171);
  R3-income-source-tie-order (Transaction.sq:76, equal totals have no tiebreak).

- [x] T5 Review follow-ups (user authorized 2026-11-12) — route: delegated. Commits 48b6b9b (a,b,c),
  d9aff43 (f,g,h), 7217905 (d,e,i,j,k,n), c17a671 (l,m). RED→GREEN per group (h, j, n were already correct:
  guard tests added, no genuine RED). test + assembleDebug green; parent spot check green.
  Decisions: COP paste drops a 1–2 digit fraction; USD/EUR last mark is decimal when the other mark groups;
  "12." parses as 12 in USD/EUR; period guard uses offset + earliest() not UI state; expansion reconciled by
  shared movement id; compact 999,999 → "$1 M".
  Emulator: Week "Ahorro 57%" green; 6 rapid ‹ taps on Year stay at 2026; typing 1234567 / mid-delete / "5.000"
  → "5,000" OK; group expand/collapse OK. Clipboard paste not exercisable via adb (unit tests cover it).
  RDD vs 1f382e7: medium, 376 lines (under budget, final slice) → user GRANTED. Review review-eefe8570b7283e88:
  APPROVED, acknowledged, burned. Boundary = c17a671. Two real warnings introduced by T5 → fix delegated:
  R3-cop-mid-edit-mark-truncates (typing "." mid-number in COP drops digits) and
  R3-expansion-pruned-on-empty-initial-feed (rotation loses expanded groups). Fixed: a8f03cd (typed single
  char never drops digits; paste = growth > 1 char), 45c4168 (no reconcile while feed not loaded). RED (4) →
  GREEN; test + assembleDebug green. Emulator: COP "1,234" + "." mid-number → "1,234"; "." then "5" → "12,345";
  expanded group survives portrait → landscape → portrait.
  Items:
  a) COP paste "1234.56" must not inflate to 123456; b) USD/EUR paste "1,234.56" must become 1234.56;
  c) "12." gives a hint instead of silently disabling submit; d) formatCompactMoney 999,999 → "$1 M";
  e) Week "Ahorro" value tinted green (red if negative); f) prune feed expansion keys not in the feed;
  g) group key survives deleting its oldest movement; h) GroupedBubble category summary boundary at 3;
  i) metrics rapid ‹ taps can't pass the earliest period; j) income-source tiebreak by name;
  k) income bar ignores zero-weight segments; l) float asserts with delta; m) remove unused barFraction;
  n) test that Day skips the category query.

## Acceptance
- `./gradlew test` and `./gradlew assembleDebug` green after each task.
- Each task matches its artboard on the canvas.

## Progress / evidence
- 2026-11-12: branch `feat/ui-redesign-v2` created from main 742fade. RDD: on (global).

## Emulator checks (user rule: test on emulator before confirming)
- 2026-11-12, Pixel_6 AVD, debug APK built from 66752ff (temp worktree, removed after):
  - T0a OK: "Registrar transacción..." sits above the gesture pill (gesture nav mode 2).
  - T2 OK: groups render (×N, total, categories "+2 más", range across days); tap expands, ^ collapses.
  - T1 OK: year cards + month week cards; overspent week (income $0) shows full red bar + caption.
  - T0b OK: typing 373929 shows "$373,929" in-field, no helper text; mid-text delete/insert keeps cursor
    (1234567 → 4×left → del → "124,567" → type 9 → "1,294,567"; separators are skipped by the cursor).
  - Note: history net amounts show no "+" sign (design had "+$640.000"); user confirmed it's fine as is.

## Next step
All tasks (T0a-T5 + fixes) done and emulator-verified (2026-11-12). Last slice c17a671..45c4168
(79 lines) under budget, not reviewed. Branch not pushed. Pending user decision: delivery (push + PR; branch ~5k changed lines incl. tests → chained-PR strategy question).
