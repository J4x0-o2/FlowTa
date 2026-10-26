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
- [ ] T2 Reset everything: queries + `ResetAllDataUseCase` (DB + DataStore + WorkManager + reseed) + Settings UI; tests.
- [ ] T3 Emulator check of both flows.

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

## Next step
T2.
