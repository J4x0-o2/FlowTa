# Feature: Reset an account / Reset the whole app

## Objective
1. Reset a single account: keep the account, delete its movements, start again from a balance the user enters.
2. Reset everything from Settings: leave the app as freshly installed (all data and preferences).

## Problem / why
The user tested with throwaway data and needs a clean start to track real finances. Today there is no
delete/reset UI; `AccountIntent.DeleteAccount` exists but no screen dispatches it, and `Account.sq`
`delete` would leave orphan transactions that still count in Metrics and History.

## Scope
### Reset account (T1)
- Entry: overflow menu (⋮) in `AccountDetailScreen` top bar → "Reiniciar cuenta".
- Warning dialog: states how many movements will be deleted (transactions + transfers involving the
  account), that it cannot be undone; numeric field "Saldo inicial" default 0; red "Reiniciar" button + "Cancelar".
- Effect, in ONE SQLDelight transaction: delete `user_transaction` rows of the account; delete
  `internal_transfer` rows where it is origin or destination; delete goal contributions tied to the
  account if the schema links them (writer verifies); set the account balance to the entered value.
- Other accounts' stored balances are NOT modified (their balance stays their current truth).

### Reset everything (T2)
- Entry: `SettingsBottomSheet` → "Borrar todos los datos" (destructive style).
- Strong warning dialog listing what is erased (cuentas, movimientos, transferencias, metas,
  categorías personalizadas, preferencias); confirm only after typing `BORRAR`.
- Effect: in ONE transaction delete every row of every table (respecting FK order); clear the
  settings DataStore (theme, currency → defaults); cancel all WorkManager work (goal reminders);
  re-seed default transaction types (same as first launch); navigate to the start screen clearing the back stack.
- App stays open (in-app wipe, not `clearApplicationUserData`, which kills the process).

## Constraints
- No schema change (new queries only → no migration). Follow `CLAUDE.md` conventions
  (MVI, use cases with `operator fun invoke`, ModalBottomSheet/dialog as local state, Snackbar errors).
- UI copy in Spanish (project language).

## TDD
- Mode: strict, enabled (source: user global instructions). Runner: `./gradlew testDebugUnitTest`.

## Tasks
- [x] T1 Reset account: queries + repository + `ResetAccountUseCase` + dialog/menu; tests (SQLite driver).
- [x] T2 Reset everything: queries + `ResetAllDataUseCase` (DB + DataStore + WorkManager + reseed) + Settings UI; tests.
- [x] T3 Emulator check of both flows.
- [x] T4 Review follow-ups (R3 findings: starting-balance thousands, atomic seed, best-effort reset-all, order assertion, ticker zone change).
- [x] T5 One shared currency-aware amount parser for every amount input (typing "5.000" in COP saved $5).

## Route per task
- T1–T5: delegated direct (writer trigger: 2+ non-trivial files).

## Delivery
- Branch `feat/account-reset`, based on `feat/history-weekly-monthly` (unmerged).
- Forecast ~500 authored lines. Delivery strategy (ask-on-risk) is asked before any PR.

## Progress / evidence
### T1 (route: delegated direct writer)
- RED: `AccountResetDataTest` (6 tests) against a `TODO()` stub repository: 6/6 failed with `NotImplementedError`.
  After implementing, 1 test failed because of a wrong expectation in the test itself (account "c" has 2 movements, not 3); test fixed.
- GREEN: `AccountResetDataTest` 6/6, `AccountDetailViewModelTest` 6/6 (VM test written alongside the VM; no separate RED run).
- `./gradlew testDebugUnitTest assembleDebug`: BUILD SUCCESSFUL.
- Goal contributions: `goal_contribution.from_account_id` references the account, so the reset deletes them and
  subtracts their amount (floor 0) from `goal.current_amount` first, inside the same transaction.
- New queries only: `countByAccount`/`deleteByAccount` (transaction, transfer), `deleteByAccount` (contribution),
  `subtractAccountContributions` (goal); `deleteAll` added for T2. No schema change.

### T2 (route: delegated direct writer)
- RED: `ResetAllDataTest` (5) with `TODO()` stubs: 5/5 `NotImplementedError`; `SettingsViewModelResetTest` (5) with
  intents ignored: 3 failed (gate assertion, failure-keeps-dialog, success-emits-effect), 2 trivially passing.
- GREEN: both suites 5/5; full suite 80 tests, 0 failures. `./gradlew testDebugUnitTest assembleDebug`: BUILD SUCCESSFUL.
- Design: `ResetAllDataUseCase` = `AppDataRepository.wipeAll()` (one transaction, children first, accounts last) ->
  `SeedDefaultTypesUseCase` (extracted from `TypesViewModel.seedIfEmpty`, reused by it) -> `SettingsRepository.clear()`
  (DataStore) -> `WorkCanceller.cancelAll()` (`WorkManager.cancelAllWork()`, Hilt-bound, context injected).
  The BORRAR gate lives in `SettingsState.canConfirmResetAll`; success emits `SettingsEffect.DataReset` and the
  screen navigates to Transactions with `popUpTo(0)`. Wipe failures keep the dialog open with an inline error.

### T3 (emulator Pixel_6, real UI via adb input + uiautomator)
- Setup: created account "Banco" (1000) next to existing "Efectivo"; transfer Banco -> Efectivo 300.
- Reset account (Efectivo, balance 60): dialog said "Se borrarán 5 movimientos" (4 transactions + 1 transfer); entered 5000;
  snackbar "Cuenta reiniciada"; Efectivo = $5,000 with "Sin movimientos"; Banco kept $700; History year = $0 / no movements;
  Metrics all $0 / "Sin datos".
- Reset all: confirm button dimmed until BORRAR typed (lowercase "borrar" kept it disabled); after confirm the app stayed open on
  Transactions ("Registrá tu primera transacción", Balance $0); Profile: 0 cuentas, default categories re-seeded (16 chips);
  Settings: theme back to system default, currency back to COP (was switched to USD before wiping).
- `adb logcat -d`: no FATAL / AndroidRuntime entries for com.j4x.flowta.
- Screenshots: scratchpad `reset-account-dialog.png`, `reset-account-after.png`, `reset-all-dialog.png`, `reset-all-after.png`.

### T4 Review follow-ups (route: delegated direct writer; strict TDD, runner `./gradlew testDebugUnitTest`)
- R3-starting-balance-thousands. The existing amount inputs (`NewTransactionBottomSheet`, `NewAccountBottomSheet`) use a Decimal
  keyboard and a bare `toDoubleOrNull()` (only "." decimal, "5,000" is rejected, "5.000" silently becomes 5.0), so there was no
  grouping-aware parser to reuse; the starting-balance field keeps the same keyboard but parses explicitly by currency
  (`parseStartingBalance(text, CurrencyCode)`), never guessing:
  - COP (no decimals): "." and "," are thousands separators only and must be well grouped. "5.000", "5,000" -> 5000, "1.000.000" -> 1000000;
    "5,5", "12.50", "1.000,50", "1.00.000", "1000.000" -> rejected.
  - USD/EUR: one separator followed by 1-2 digits is the decimal ("12.5", "12,34"); both marks -> the last is the decimal
    ("1,234.56", "1.234,56"); repeated marks are grouping ("1.000.000"); a lone mark followed by exactly 3 digits ("5.000", "5,000")
    is ambiguous -> rejected (confirm disabled, "Monto no válido o ambiguo").
  - The dialog shows the interpretation under the field ("Saldo: $5,000"). Known gap: the transaction amount inputs keep their old
    "." only behavior (out of scope, not changed).
  - RED: 4 of 11 `AccountDetailViewModelTest` failed against the old single-separator parser (new signature, old logic). GREEN: 11/11.
- R3-seed-getorthrow-crash. `TransactionTypeRepository.addAll` inserts all defaults in ONE SQLDelight transaction; `SeedDefaultTypesUseCase`
  returns `Result<Unit>` and never throws (CancellationException rethrown). `TypesViewModel` logs a failure. `FlowTaApp.onCreate` now also runs
  the idempotent seed-if-empty on every launch (previously it only ran when the Profile screen created `TypesViewModel`), so a failed
  seed is retried on next launch. RED: `SeedDefaultTypesTest` failing-insert case left partial rows (assertion at the zero-types check). GREEN: 3/3.
- R3-resetall-partial-after-wipe. `ResetAllDataUseCase`: wipe failure -> `Result.failure`, nothing else runs; after a committed wipe,
  seed / clear preferences / cancel work are best effort (logged with `Log.w`, CancellationException rethrown) and the result is success.
  `unitTests.isReturnDefaultValues = true` added so `Log` is safe in JVM tests. RED: 3 new tests failed (settings-clear failure, work-cancel
  failure, cancellation propagation); the seed-failure case passed once seeding returned a Result. GREEN: `ResetAllDataTest` 10/10.
- R3-order-not-asserted. `ResetAllDataTest` records `wipe`, `seed`, `settings.clear`, `work.cancel` events through recording wrappers and asserts the exact order
  (also asserts `[wipe]` only on wipe failure).
- R3-ticker-zone-change. `TimeChangeSignal` (domain) + `BroadcastTimeChangeSignal` (ACTION_TIMEZONE_CHANGED / TIME_CHANGED / DATE_CHANGED via
  application context, resets `TimeZone.setDefault(null)` first) bound in `TimeModule`; `DayTicker.days()` = signal `.onStart{}` `.transformLatest{ loop }`
  `.distinctUntilChanged()`, so a signal re-reads the zone/clock and reschedules the next midnight. Single-arg constructor kept for tests.
  RED: zone-change and clock-jump tests failed with the signal ignored. GREEN: `DayTickerTest` 6/6.
- Goals "reset together with everything" (user decision 2026-10-26): already covered. `ResetAllDataTest.wipeAll_emptiesEveryTable` asserts goals and
  contributions are deleted; `AccountResetDataTest.reset_removesGoalContributionsOfAccount_andTakesThemBackFromGoal` asserts the account's
  contributions are deleted and subtracted from goal progress with a floor of 0. No change needed.
- Verification: `./gradlew testDebugUnitTest assembleDebug`: BUILD SUCCESSFUL, 96 tests, 0 failures.
- Emulator (Pixel_6): created "Banco" ($1,000), ⋮ > Reiniciar cuenta, typed `5.000`: dialog showed "Saldo: $5,000"; confirmed; account header
  showed $5,000. No FATAL / AndroidRuntime entries. Screenshots (scratchpad): `fix-balance-dialog.png`, `fix-balance-after.png`.

### T5 Shared amount parser in every input (route: delegated direct writer; strict TDD, runner `./gradlew testDebugUnitTest`)
- Bug: only the starting-balance dialog parsed by currency; the other inputs used a bare `toDoubleOrNull()` ("5.000" in COP saved $5).
- `parseStartingBalance` moved to `core/presentation/format/AmountParser.kt` as `parseAmount(text, CurrencyCode)` (same rules/cases; its tests moved to `AmountParserTest`).
- New reusable `core/presentation/components/AmountInput.kt`: text field + preview ("Monto: $5,000" / "Saldo: ...") or "Monto no válido o ambiguo"; follows the reactive `LocalCurrencyCode`/`LocalCurrencySymbol`; Decimal keyboard kept.
- Inputs covered: AddTransaction sheet amount (ingreso, gasto, transferencia share one field; `AddTransactionState.parsedAmount/amountInvalid/canSubmit`, `SetCurrency` intent fed from `LocalCurrencyCode`, preserved across tab change/reset; ViewModel submits the parsed amount),
  NewAccountBottomSheet initial balance and savings meta (create disabled while invalid), reset-account starting-balance dialog. Goals: `GoalsScreen` is a stub with no amount input today (nothing to change); any future goal input must use `AmountInput`/`parseAmount`.
- RED: `AmountParserTest` (6) + `AddTransactionViewModelTest` (6 of 7) failed with `NotImplementedError` against a `TODO()` parser (12 failed of 103). GREEN: 103 tests, 0 failures. `./gradlew testDebugUnitTest assembleDebug`: BUILD SUCCESSFUL.
- Emulator (Pixel_6, cold-booted without wipe because `pm install` hung on the snapshot-restored guest): expense `5.000` showed "Monto: $5,000"; submitted; feed "-$5,000", balance $0 (was $5,000); new account "Ahorro" with `1.000.000` showed "Saldo: $1,000,000" and the account card/capital show $1,000,000. No FATAL / AndroidRuntime for com.j4x.flowta. Screenshots (scratchpad): `fix-amount-sheet.png`, `fix-amount-after.png`.

### T6 Review follow-ups (round 2) (route: delegated direct writer; strict TDD, runner `./gradlew testDebugUnitTest`)
- R3-concurrent-seed-duplicates. `TransactionTypeRepository.addAll` replaced by `seedIfEmpty(defaults)`: new SQLDelight query `countAll`; the count and the inserts
  run inside ONE `transaction {}`. `SeedDefaultTypesUseCase` also serializes callers with a process-wide `Mutex` (needed for the JVM driver, which shares one
  connection across threads; on Android the SQLite transaction already serializes). `TypesViewModel` no longer seeds (app start seeds; reset-all re-seeds);
  the Profile list is reactive, so it fills in when the seed lands. RED: `seed_whenCallersOverlapOnAnEmptyDatabase_insertsTheDefaultsExactlyOnce`
  (8 concurrent callers x 30 rounds) failed against the old check-outside-transaction code (AssertionError, duplicated rows). GREEN after the change.
- R3-reset-wipe-throw-escapes-result. `ResetAllDataUseCase` wraps `wipeAll()`: a throw becomes `Result.failure` (CancellationException rethrown), no other step runs.
  RED: `resetAll_whenWipeThrows_returnsFailure_andRunsNoOtherStep` threw out of the use case. GREEN; plus a cancellation-propagation test.
- R3-ticker-noevents-possibly-vacuous. `DayTickerTest` now calls `testScheduler.runCurrent()` before `expectNoEvents()`. Mutation check: with
  `distinctUntilChanged()` removed from `DayTicker`, `days_doesNotRepeatTheSameDate_whenTheSignalFiresWithoutAChange` FAILED (6 tests, 1 failed); restored, 6/6 pass.

## Next step
Delivery strategy (ask-on-risk) before any PR; ~600 authored lines in total, so a chain decision applies.
