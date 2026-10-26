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

## Route per task
- T1–T3: delegated direct (writer trigger: 2+ non-trivial files).

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

## Next step
Delivery strategy (ask-on-risk) before any PR; ~600 authored lines in total, so a chain decision applies.
