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
- [ ] T0a Bottom bar above gesture bar — route: inline (1 file)
- [ ] T0b Thousands separator inside AmountInput — route: delegated (formatter + transformation + tests)
- [ ] T1 History cards — route: delegated (models + screen + tests)
- [ ] T2 Grouped feed bubbles — route: delegated
- [ ] T3 Metrics base + Day + Week — route: delegated
- [ ] T4 Metrics Month + Year — route: delegated

## Acceptance
- `./gradlew test` and `./gradlew assembleDebug` green after each task.
- Each task matches its artboard on the canvas.

## Progress / evidence
- 2026-11-12: branch `feat/ui-redesign-v2` created from main 742fade. RDD: on (global).

## Next step
T0a.
