# FlowTa — Contexto para Claude

App Android de finanzas personales. Transacciones como tarjetas compactas. Ingresos a la derecha en verde, gastos a la izquierda en rojo oscuro.

**Docs completos**: `C:\Users\Fresa\Documents\Programacion\012-FlowTa\Docs\`
**Figmas**: `C:\Users\Fresa\Documents\Programacion\012-FlowTa\Figma\` (18 imágenes)
**Ícono fuente**: `C:\Users\Fresa\Documents\Programacion\012-FlowTa\Icon.png`

---

## Estado del proyecto

| Fase | Estado |
|---|---|
| Scaffolding | ✅ COMPLETO |
| F01 Settings | ✅ COMPLETO |
| F02 Cuentas | ✅ COMPLETO |
| F03 Tipos | ✅ COMPLETO |
| F04 Registro (3 tabs) | ✅ COMPLETO |
| F05 TransactionScreen | ✅ COMPLETO |
| F06 Detalle transacción | ✅ COMPLETO |
| F08 Historial por cuenta | ✅ COMPLETO |
| F09 Perfil completo | ✅ COMPLETO |
| Métricas | ✅ COMPLETO |
| Historial semanal/mensual/anual | ✅ COMPLETO |
| Moneda reactiva | ✅ COMPLETO |
| Snackbars de error | ✅ COMPLETO |
| Ícono personalizado | ✅ COMPLETO |

**Todo el proyecto está en BUILD SUCCESSFUL sin warnings.**

---

## Stack

```
AGP 8.5.0 · Kotlin 2.0.0 · KSP 2.0.0-1.0.24
Hilt 2.51.1 · SQLDelight 2.0.2 · Compose BOM 2024.09.00
Package: com.j4x.flowta · minSdk 29 · compileSdk 35
```

**⚠️ NO actualizar AGP a 9.x** — SQLDelight 2.x usa `BaseExtension` removida en AGP 9.

---

## Workarounds activos (no tocar)

`app/build.gradle.kts` contiene workarounds para SQLDelight + KSP con Gradle 9.2.1:
- `sourceSets` debug/release apuntando al output de SQLDelight
- `afterEvaluate { kspDebugKotlin.dependsOn(...) }`

Mappers usan subpaquetes generados por SQLDelight:
- `com.j4x.flowta.database.transaction.SelectAll`
- `com.j4x.flowta.database.internalTransfer.SelectAll`

---

## Schema SQLDelight — estado final

```sql
account:           id, name, type, balance, meta_amount?, meta_periodicity?, created_at
user_transaction:  id, amount, flow_type, transaction_type_id, source_type_id?, account_id, note, created_at
internal_transfer: id, from_account_id, to_account_id, transfer_type_id?, amount, note, created_at
transaction_type:  id, name, category, created_at
  -- categories: INCOME | EXPENSE | RECURRING_EXPENSE | TRANSFER | INCOME_SOURCE
```

Queries métricas en `Transaction.sq`: `totalByCategoryInRange`, `incomeVsExpenseByDay`, `netBalance`
Query transferencias en `InternalTransfer.sq`: `totalAmountInRange`

## Migraciones

Versión actual del schema: **1** (baseline de los dispositivos existentes). Snapshot congelado en `app/src/main/sqldelight/databases/1.db`; `verifyMigrations` está activo.

Para cambiar el schema:
1. Editar el `.sq` y crear `app/src/main/sqldelight/migrations/N.sqm` (N = versión actual) con los `ALTER TABLE` necesarios.
2. Regenerar el snapshot `N+1.db` con `./gradlew generateDebugFlowTaDatabaseSchema`.
3. Correr `./gradlew verifySqlDelightMigration` (migraciones + snapshots deben coincidir con los `CREATE`).
4. Extender `MigrationSnapshotTest` para migrar desde el snapshot anterior (`Schema.migrate`).
5. Nunca editar un `.db` ya commiteado.

Nota (Windows): los workers de Gradle 9 arrancan solo con `SystemRoot` en el entorno, así que `sqlite-jdbc` intentaría extraer su DLL en `C:\Windows` (`AccessDeniedException`). El módulo `tools/sqlite-worker-shim` (solo de build, no va en el APK) se agrega al classpath de las tareas de SQLDelight desde `app/build.gradle.kts` y redirige la extracción a `~/.gradle/sqlite-native`. No correr `generate*Schema` y `verify*Migration` en la misma invocación (el plugin no declara la dependencia entre ellas).

---

## Lo que hay construido

### F01 — Settings
- `SettingsBottomSheet` — toggle tema oscuro + selector moneda (COP/USD/EUR con fondo índigo activo)
- `MainActivity` — tema reactivo + `LocalCurrencySymbol` provisto via `CompositionLocalProvider`
- Abre desde ⚙ en header de `ProfileScreen`

### F02 — Cuentas
- `NewAccountBottomSheet` — Nombre + Tipo dropdown + Saldo inicial + Meta (solo SAVINGS)
- `AccountCard` — ícono + nombre + tipo + balance + chevron; `LinearProgressIndicator` para SAVINGS con meta
- `AccountViewModel` — GetAccounts + AddAccount + DeleteAccount + `totalCapital`

### F03 — Tipos de transacción
- `TypesBottomSheet(title, category, types, chipColor, ...)` — reutilizable por categoría
- `TypeChip(name, color, onDelete?)` — chip coloreado reutilizable
- `TypesViewModel` — observa INCOME + EXPENSE + TRANSFER; `seedIfEmpty()` siembra defaults
- Defaults: 5 INCOME, 9 EXPENSE, 2 TRANSFER, 7 INCOME_SOURCE

### F04 — Registro de transacción
- `NewTransactionBottomSheet` — 3 tabs: ↙ Ingreso (verde) / ↗ Gasto (rojo) / ⇄ Transferencia (índigo)
- `AddTransactionViewModel` — carga accounts + 4 categorías reactivas; submit despacha a `AddTransactionUseCase` (INCOME/EXPENSE) o `ExecuteInternalTransferUseCase` (TRANSFER)
- `AddTransactionContract` — `TransactionTab` enum + `AddTransactionState` + intents + effects
- Tab Ingreso: Monto + Categoría + Fuente(INCOME_SOURCE, opcional) + Cuenta + Nota
- Tab Gasto: Monto + Categoría + Cuenta + toggle "Agregar nota"
- Tab Transferencia: Monto + Categoría(opcional) + Desde + Hacia(excluye origen) + Nota
- Botón Registrar deshabilitado hasta que los campos requeridos tengan valor

### F05 — TransactionScreen (pantalla principal)
- `TopAppBar` con "FlowTa" bold + "Balance: $X" (verde/rojo) + íconos BarChart→Métricas y Person→Perfil
- `BottomInputBar` — barra clickeable "Registrar transacción..." (reemplaza FAB)
- `TransactionBubble` — ícono dirección (↙/↗) + monto con signo + fecha/hora ("29 may · 2:55 PM")
- `LazyColumn` con `reverseLayout = true`
- Snackbar para errores de registro (ej: saldo insuficiente en transferencias)

### F06 — Detalle de transacción
- `TransactionDetailBottomSheet` — badge coloreado tipo (↙ Ingreso / ↗ Gasto) + monto grande
- Filas: Categoría, Fuente (solo INCOME), Cuenta, Fecha completa, Nota (si tiene)
- Botón "× Cerrar" al pie
- Se abre al tocar cualquier burbuja en `TransactionScreen` o `AccountDetailScreen`

### F08 — Historial por cuenta
- `AccountDetailViewModel` — lee `accountId` de `SavedStateHandle`; carga cuenta + transacciones reactivamente
- `AccountDetailScreen` — TopAppBar con nombre + balance coloreado; card de progreso para SAVINGS; lista de burbujas; detail sheet al tocar
- `SavingsProgressCard` — barra de progreso + meta + % completado (solo cuentas SAVINGS con meta)

### F09 — Perfil completo
- `ProfileScreen` — Capital Total + Mis Cuentas + Categorías (INCOME/EXPENSE/TRANSFER) + Cerrar sesión
- Botón "Cerrar sesión" con `AlertDialog` de confirmación → navega a pantalla principal limpiando back stack
- `activeSheet: ActiveSheet?` controla todos los ModalBottomSheets
- Snackbar para errores de cuentas (`AccountEffect.ShowError`)

### Métricas
- `MetricsScreen` — tabs Semana/Mes/Todo (rango reactivo via `flatMapLatest`)
- 4 tarjetas resumen: Ingresos ↙ verde, Gastos ↗ rojo, Transferencias ↔ índigo, Flujo neto
- Gráfico de barras Canvas — verde/rojo por día (máx 14 días) + leyenda
- Gráfico donut Canvas — gastos por categoría con leyenda y porcentajes
- `MetricsViewModel` — `_selectedRange` → `flatMapLatest` → `combine(flows, categories, transfers)`

### Historial (semanal / mensual / anual)
- `HistoryScreen` — stepper de año, "Neto del año" + tiles Ingresos/Gastos, tarjetas "Por mes" (solo meses con movimientos, más nuevo primero, "· en curso")
- `HistoryMonthScreen` — "Neto del mes" + tiles Ingresos/Gastos/Movimientos, tarjetas "Por semana" (lun–dom recortadas al mes, no clickeables)
- Barras verde/roja escaladas al mayor ingreso/gasto entre hermanos (meses del año / semanas del mes)
- `HistoryAggregator` (dominio puro) agrupa por fecha LOCAL (`ZoneId`) en Kotlin, no en SQL; transferencias excluidas
- Queries nuevas en `Transaction.sq`: `selectMovementsInRange` (`[from, to)`), `earliestCreatedAt`; sin cambio de schema
- `ObserveYearHistoryUseCase(year)` / `ObserveMonthHistoryUseCase(year, month)`; `HistoryViewModel` (año en `SavedStateHandle`) y `HistoryMonthViewModel` (args de navegación)

### Moneda reactiva
- `LocalCurrencySymbol` — `compositionLocalOf { "$" }` en `core/presentation/`
- Provisto en `MainActivity` leyendo `CurrencyCode` de `SettingsViewModel`
- Consumido en: `TransactionBubble`, `TransactionScreen`, `AccountDetailScreen`, `TransactionDetailBottomSheet`, `AccountCard`, `ProfileScreen` (CapitalTotal), `MetricsScreen`

### Ícono personalizado
- Símbolo $ dorado 3D sobre fondo negro
- Generado con PowerShell + System.Drawing a todas las densidades (48–192px PNG)
- Ícono adaptativo: foreground `drawable/ic_launcher_foreground.png` (432×432), background negro `#000000`

---

## Navegación

```
TransactionScreen (start)
  ├── → MetricsScreen       (BarChart icon en header)
  ├── → HistoryScreen       (CalendarMonth icon en header)
  │     └── → HistoryMonthScreen  (tap en tarjeta de mes; ruta history_month/{year}/{month})
  ├── → ProfileScreen       (Person icon en header)
  │     └── → AccountDetailScreen  (tap en AccountCard)
  └── ModalBottomSheets (estado local, no rutas):
        NewTransactionBottomSheet · TransactionDetailBottomSheet
```

Sin BottomNavigation — navegación por iconos en headers + back arrows.

---

## Convenciones de código

- MVI manual: `FeatureState` · `FeatureIntent` · `FeatureEffect` por feature
- Casos de uso: `suspend operator fun invoke(...)` un método público
- Mappers: `fun EntityDb.toDomain(): DomainModel`
- Screens: sufijo `Screen`, componentes reutilizables sin sufijo
- ModalBottomSheets: no son rutas de NavGraph — estado local o Effect
- `SavedStateHandle` para parámetros de navegación en ViewModels de detalle
- Snackbars: `SnackbarHostState` en el Scaffold de cada pantalla, `showSnackbar()` en `LaunchedEffect`
- Iconos deprecated: usar siempre `Icons.AutoMirrored.Filled.*` para íconos direccionales
- Commits: `feat:` `fix:` `refactor:` `test:` `chore:`

---

## Decisiones resueltas

1. **INCOME_SOURCE** — sin sección en Perfil, se gestiona inline en F04 (tab Ingreso)
2. **meta_periodicity** — fijo "monthly", no hay selector
3. **Nota en Gasto** — toggle "Agregar nota" que expande el campo
4. **Ícono adaptativo** — background negro + foreground PNG (no vector) porque el ícono es raster
5. **Moneda** — `CompositionLocal` en lugar de prop-drilling; el símbolo se deriva del `currencyCode` en DataStore

---

## Pendiente

- Tests unitarios (infraestructura lista: Turbine + MockK + SQLDelight SQLite driver en test deps)
- `ExampleInstrumentedTest.kt` vaciado — no hay `androidTestImplementation` en build.gradle.kts

---

## Comandos útiles

```bash
./gradlew assembleDebug                              # build completo
./gradlew generateDebugFlowTaDatabaseInterface       # regenerar SQLDelight
./gradlew test                                       # unit tests
```
