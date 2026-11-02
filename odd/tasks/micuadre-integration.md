# MiCuadre integration (FlowTa side)

Counterpart: `MiCuadre/odd/tasks/flowta-integration.md` (MiCuadre side). Engram mirror: `odd/micuadre-integration/tasks`.

## Objective

Every trip finished in MiCuadre is recorded in FlowTa as an income, in the FlowTa account the user
assigned to that trip's payment method (cash or transfer), with no duplicates and no lost entries.

## Problem / why

The user drives with MiCuadre and manages money in FlowTa. Today every trip income must be typed
twice. The user wants a single place with full control of all income and expenses.

## Agreed design (approved 2026-10-18, refined 2026-11-02)

1. MiCuadre has a Settings toggle "Send income to FlowTa" (default off).
2. On enable, MiCuadre checks FlowTa is installed; if not, it offers to install it.
3. **Connect handshake**: MiCuadre opens a FlowTa screen where the user picks
   - the income type (e.g. a "Trips" INCOME type),
   - one FlowTa account for **CASH** trips and one for **TRANSFER** trips.
   FlowTa stores this link and returns OK. MiCuadre never sees FlowTa ids.
4. Each finished trip is sent through a FlowTa ContentProvider guarded by a **signature**
   permission. FlowTa routes it to the account mapped to its payment method.
5. FlowTa dedupes by `UNIQUE(source_app, external_id)`; a resend is a harmless `DUPLICATE`.

### Shared contract v1 (`ContentResolver.call`)

- Authority: `${applicationId}.integration` (so `com.j4x.flowta.integration`, `.staging` variant differs).
- Permission: `com.j4x.flowta.permission.RECORD_INCOME`, `protectionLevel="signature"`.
- Method `recordIncome`, extras:
  `contract_version=1`, `source_app` (caller package), `external_id` (MiCuadre trip id as string),
  `payment_method` (`CASH` | `TRANSFER`), `amount_minor` (Long, COP x 100), `occurred_at` (epoch ms,
  trip `endedAt`), `note` (e.g. "MiCuadre · Uber").
- Response bundle `status`: `OK` | `DUPLICATE` | `NOT_LINKED` | `INVALID`.
  `NOT_LINKED` covers "never connected" and "mapped account/type was deleted".
- Method `linkStatus` → `LINKED` | `NOT_LINKED` (lets MiCuadre show "reconnect").
- Connect screen: explicit intent action `com.j4x.flowta.action.CONNECT_SOURCE`, same permission;
  returns `RESULT_OK` once the mapping is saved.

### Facts from exploration (2026-11-02)

- FlowTa amounts are `Double` whole units; MiCuadre `amountMinor` is pesos x 100 → `amount = amount_minor / 100.0`.
- `TransactionRepositoryImpl.add` inserts with random UUID + `System.currentTimeMillis()` and updates
  the account balance in one transaction; the integration needs `created_at = occurred_at` and the
  source columns, so it gets its own repository method that keeps the same balance logic.
- Type and account ids are random UUIDs → the mapping must live in FlowTa.
- No `migrations/` yet; `1.db` snapshot + `verifyMigrations` + `MigrationSnapshotTest` exist.
- ContentProvider is not Hilt-injectable → `@EntryPoint` over the `SingletonComponent` database.
- **Release build is unsigned**; staging uses the debug key. A signature permission only works if
  both apps share the same signing key (see open decision D1).

## Scope

In: schema migration, link storage, record-income use case, provider, connect screen, unlink UI, docs.
Out (v1): backfilling trips finished before connecting; propagating later edits/deletes of a trip in
MiCuadre; other source apps; non-COP currency.

## Open decisions

- **D1 — signing key. RESOLVED 2026-11-02:** user approved signing FlowTa release with the same
  keystore as MiCuadre (`~/.android-keys/micuadre-upload.jks`), configured from `local.properties`.
  Debug builds already share `~/.android/debug.keystore`. Backup copy on the user's Desktop.

## TDD

Mode: strict (on), source: user global config. Runner: `./gradlew testDebugUnitTest`
(JUnit4 + MockK + Turbine + SQLDelight JVM sqlite driver).

## Tasks

Route per task is recorded as `[route: …]`.

- [x] **F1 Release signing** — `signingConfigs.release` from `local.properties`
      (`FLOWTA_KEYSTORE_FILE`, `_KEYSTORE_PASSWORD`, `_KEY_ALIAS`, `_KEY_PASSWORD`), fail fast on partial
      config, unsigned when absent. Depends on D1. Check: `assembleRelease` + `apksigner verify --print-certs`
      shows the same SHA-1 as MiCuadre. [route: inline — single build file, pattern copied from MiCuadre]
      Evidence 2026-11-02: both release APKs SHA-1 `71:54:82:13:3D:05:21:F1:37:E8:1B:46:74:A2:12:3E:65:86:6D:4A`.
- [x] **F2 Migration 1 → 2** — `migrations/1.sqm`: `user_transaction` gets `source_app TEXT`,
      `external_id TEXT` + `CREATE UNIQUE INDEX` on both; new table
      `integration_link(source_app TEXT PK, income_type_id TEXT NOT NULL, cash_account_id TEXT NOT NULL,
      transfer_account_id TEXT NOT NULL, created_at INTEGER NOT NULL)`. Update `.sq`, generate `2.db`,
      `verifySqlDelightMigration`, extend `MigrationSnapshotTest` (1 → 2 keeps existing rows). [route: delegated — 4+ files: .sq, .sqm, snapshot, tests]
      Evidence 2026-11-02: RED `table user_transaction has no column named source_app` (5/8 db tests);
      GREEN generateDebugFlowTaDatabaseSchema, verifySqlDelightMigration (~8 min, CPU-bound ObjectDiffer),
      testDebugUnitTest (114 tests), assembleDebug; parent re-ran the 8 db tests: green.
- [ ] **F3 Record external income (domain + data)** — `RecordExternalIncomeUseCase` returning
      `Recorded | Duplicate | NotLinked | Invalid`; repository inserts with `created_at = occurredAt`,
      source columns and balance update atomically; duplicate detected by the unique key. Tests over
      the JVM sqlite driver: routes CASH/TRANSFER, duplicate no double balance, deleted account → NotLinked,
      amount ≤ 0 → Invalid. [route: delegated]
- [ ] **F4 IntegrationProvider** — `call()`-only provider, `@EntryPoint` for the use case and link
      repository, manifest `<permission>` + `<provider>` (exported, guarded). Bundle ↔ domain mapping
      tested as pure functions. [route: delegated]
- [ ] **F5 Connect screen** — `ConnectSourceActivity` (Compose, theme-aware): income type picker
      (INCOME types, create-new shortcut), CASH account, TRANSFER account; Save → upsert link →
      `RESULT_OK`; Cancel → `RESULT_CANCELED`. ViewModel tests. [route: delegated]
- [ ] **F6 Connected apps / unlink** — in Profile/Settings, show the MiCuadre link (accounts per
      method) with "Disconnect" (deletes the link → future calls get `NOT_LINKED`). [route: delegated]
- [ ] **F7 Docs** — CLAUDE.md (schema v2, contract, signing), README section. [route: inline]

## Acceptance criteria

- Finishing a cash trip in MiCuadre adds an income in the CASH-mapped account with the trip time;
  a transfer trip goes to the TRANSFER-mapped account; balances update.
- Sending the same trip twice changes nothing the second time.
- An app signed with another key cannot call the provider or open the connect screen.
- Existing users upgrade from schema 1 with all data intact.

## Progress

- 2026-11-02: plan written; exploration done.
- 2026-11-02: D1 resolved; F1 done (61d6b79). Branch renamed to `feat/micuadre-integration`.
- 2026-11-02: F2 done (schema v2, 47fdd9a). Review (F1+F2, 4 lenses, risk high): granted → approved,
  acknowledged (lineage review-2d2d63d3f2a7136c); reviewed boundary = 47fdd9a. Non-blocking follow-ups
  to fold into F3: rename `DatabaseSchemaTest.schemaVersion_isOne_untilFirstMigrationIsAdded` (asserts 2);
  the unique index only dedupes when BOTH source_app and external_id are non-null → F3 must reject
  external rows missing either.

## Next step

F3 (record external income) → F4 → F5 → F6, then MiCuadre side.
