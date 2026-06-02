# FlowTa 💰

**FlowTa** es una app Android de finanzas personales que te permite registrar ingresos, gastos y transferencias de forma rápida e intuitiva. La interfaz está inspirada en el concepto de "chat financiero": cada transacción aparece como una burbuja compacta — ingresos a la derecha en verde, gastos a la izquierda en rojo.

---

## Características

### Registro de transacciones
- **3 tipos**: Ingreso, Gasto, Transferencia interna entre cuentas
- **Categorías personalizables** para cada tipo
- **Fuente de ingreso** opcional (efectivo, transferencia, freelance, etc.)
- **Notas** opcionales por transacción
- Formulario en `ModalBottomSheet` con tabs, sin interrumpir el flujo

### Gestión de cuentas
- Tipos: Efectivo, Cuenta bancaria, Billetera digital, Ahorros, Otro
- **Meta de ahorro** para cuentas tipo Ahorros con barra de progreso
- Balance actualizado automáticamente con cada transacción o transferencia

### Historial
- Vista global con todas las transacciones ordenadas por fecha (más reciente primero)
- Vista por cuenta individual con su historial y progreso de meta
- **Detalle de transacción** al tocar cualquier burbuja: categoría, fuente, cuenta, fecha y nota

### Métricas
- Filtro por **Semana / Mes / Todo el tiempo**
- 4 tarjetas resumen: Ingresos, Gastos, Transferencias, Flujo neto
- **Gráfico de barras** Ingresos vs Gastos por día (Canvas nativo)
- **Gráfico donut** Gastos por categoría con leyenda y porcentajes (Canvas nativo)

### Configuración
- **Tema**: Claro, Oscuro o Sistema
- **Moneda**: COP (Peso colombiano), USD (Dólar), EUR (Euro)
  - El símbolo se propaga reactivamente a toda la app

### Tipos de categorías
- Gestionados desde Perfil: agregar y eliminar chips por categoría
- Defaults pre-cargados en el primer arranque: 5 ingresos, 9 gastos, 2 transferencias, 7 fuentes

---

## Capturas de pantalla

| Pantalla principal | Registro | Detalle |
|---|---|---|
| Lista de transacciones con balance en header | Sheet con 3 tabs (Ingreso / Gasto / Transferencia) | Badge de tipo, monto, campos y fecha |

| Perfil | Métricas | Configuración |
|---|---|---|
| Capital total, cuentas y categorías | Barras + donut por período | Tema y moneda |

---

## Arquitectura

FlowTa sigue **Clean Architecture** con **MVI** (Model-View-Intent) manual en la capa de presentación.

```
app/
└── src/main/java/com/j4x/flowta/
    ├── core/
    │   ├── domain/model/          # Currency, CurrencyCode
    │   └── presentation/
    │       ├── components/        # TransactionBubble, BottomInputBar
    │       ├── navigation/        # NavGraph, Screen
    │       ├── theme/             # Color, Type, Theme
    │       └── LocalCurrencySymbol.kt
    ├── feature/
    │   ├── accounts/
    │   │   ├── data/              # AccountRepositoryImpl, AccountMapper
    │   │   ├── domain/            # Account, AccountRepository, use cases
    │   │   └── presentation/      # AccountViewModel, AccountCard, NewAccountBottomSheet, AccountDetailScreen
    │   ├── metrics/
    │   │   ├── data/              # MetricsRepositoryImpl
    │   │   ├── domain/            # CategoryExpense, DailyFlow, MetricsRepository
    │   │   └── presentation/      # MetricsViewModel, MetricsScreen
    │   ├── profile/
    │   │   ├── data/              # ProfileRepositoryImpl
    │   │   ├── domain/            # ProfileRepository
    │   │   └── presentation/      # ProfileScreen
    │   ├── settings/
    │   │   ├── data/              # SettingsRepositoryImpl (DataStore)
    │   │   ├── domain/            # UserPreferences, AppTheme, SettingsRepository
    │   │   └── presentation/      # SettingsViewModel, SettingsBottomSheet
    │   ├── transactions/
    │   │   ├── data/              # TransactionRepositoryImpl, TransactionMapper
    │   │   ├── domain/            # Transaction, NewTransaction, TransactionRepository, use cases
    │   │   └── presentation/      # TransactionViewModel, AddTransactionViewModel
    │   │                          # TransactionScreen, NewTransactionBottomSheet, TransactionDetailBottomSheet
    │   ├── transfers/
    │   │   ├── data/              # TransferRepositoryImpl, TransferMapper
    │   │   ├── domain/            # InternalTransfer, NewInternalTransfer, TransferRepository
    │   │   └── presentation/      # (pantalla placeholder)
    │   └── types/
    │       ├── data/              # TransactionTypeRepositoryImpl, TransactionTypeMapper
    │       ├── domain/            # TransactionType, TypeCategory, TransactionTypeRepository
    │       └── presentation/      # TypesViewModel, TypesBottomSheet, TypeChip
    └── di/
        ├── DatabaseModule.kt      # SQLDelight queries
        └── RepositoryModule.kt    # Bindings Hilt
```

### Patrón MVI por feature

Cada feature expone tres artefactos de contrato:

```kotlin
data class FeatureState(...)          // Estado inmutable de la UI
sealed interface FeatureIntent { ... } // Acciones que el usuario puede disparar
sealed interface FeatureEffect { ... } // Efectos de una sola vez (navegación, snackbar)
```

El ViewModel expone `StateFlow<FeatureState>` y `Flow<FeatureEffect>` vía `Channel`. La pantalla:
1. Observa el estado con `collectAsStateWithLifecycle()`
2. Colecta efectos con `LaunchedEffect(Unit)`
3. Envía intents con `viewModel.onIntent(...)`

### ModalBottomSheets

Los sheets **no son rutas de NavGraph** — se controlan con estado local en la pantalla padre:

```kotlin
var activeSheet by remember { mutableStateOf<ActiveSheet?>(null) }
// o
var showAddSheet by remember { mutableStateOf(false) }
```

---

## Stack tecnológico

| Herramienta | Versión | Uso |
|---|---|---|
| **AGP** | 8.5.0 | Build del proyecto (⚠️ no actualizar a 9.x) |
| **Kotlin** | 2.0.0 | Lenguaje principal |
| **KSP** | 2.0.0-1.0.24 | Procesador de anotaciones |
| **Jetpack Compose BOM** | 2024.09.00 | UI declarativa |
| **Material 3** | vía BOM | Design system + componentes |
| **Hilt** | 2.51.1 | Inyección de dependencias |
| **SQLDelight** | 2.0.2 | Base de datos SQLite con type-safety |
| **DataStore Preferences** | — | Persistencia de ajustes (tema, moneda) |
| **Navigation Compose** | — | Navegación entre pantallas |
| **Lifecycle / ViewModel** | — | MVI + estado reactivo |
| **Coroutines + Flow** | — | Programación asíncrona reactiva |

### Sin dependencias de charting

Los gráficos de Métricas están implementados con **Canvas nativo de Compose** (`drawRect`, `drawArc`) para mantener cero dependencias externas adicionales.

---

## Base de datos (SQLDelight)

```sql
-- Cuentas
CREATE TABLE account (
    id               TEXT NOT NULL PRIMARY KEY,
    name             TEXT NOT NULL,
    type             TEXT NOT NULL,  -- CASH | BANK | DIGITAL_WALLET | SAVINGS | OTHER
    balance          REAL NOT NULL,
    meta_amount      REAL,           -- solo SAVINGS
    meta_periodicity TEXT,           -- "monthly"
    created_at       INTEGER NOT NULL
);

-- Transacciones
CREATE TABLE user_transaction (
    id                  TEXT NOT NULL PRIMARY KEY,
    amount              REAL NOT NULL,
    flow_type           TEXT NOT NULL,  -- INCOME | EXPENSE
    transaction_type_id TEXT NOT NULL REFERENCES transaction_type(id),
    source_type_id      TEXT REFERENCES transaction_type(id),  -- INCOME_SOURCE, nullable
    account_id          TEXT NOT NULL REFERENCES account(id),
    note                TEXT,
    created_at          INTEGER NOT NULL
);

-- Transferencias internas
CREATE TABLE internal_transfer (
    id               TEXT NOT NULL PRIMARY KEY,
    from_account_id  TEXT NOT NULL REFERENCES account(id),
    to_account_id    TEXT NOT NULL REFERENCES account(id),
    transfer_type_id TEXT REFERENCES transaction_type(id),
    amount           REAL NOT NULL,
    note             TEXT,
    created_at       INTEGER NOT NULL
);

-- Tipos / Categorías
CREATE TABLE transaction_type (
    id         TEXT NOT NULL PRIMARY KEY,
    name       TEXT NOT NULL,
    category   TEXT NOT NULL,  -- INCOME | EXPENSE | RECURRING_EXPENSE | TRANSFER | INCOME_SOURCE
    created_at INTEGER NOT NULL
);
```

> La DB se recrea en cada instalación (app en desarrollo, sin migraciones `.sqm`).

---

## Navegación

```
TransactionScreen  ←── pantalla de inicio
    │
    ├── [BarChart icon] ──► MetricsScreen
    │                           └── [←] volver
    │
    ├── [Person icon] ──► ProfileScreen
    │                         └── [AccountCard tap] ──► AccountDetailScreen
    │                                                        └── [←] volver
    │
    └── Sheets (estado local, fuera del NavGraph):
            ├── NewTransactionBottomSheet  (tap en barra inferior)
            └── TransactionDetailBottomSheet  (tap en burbuja)
```

---

## Moneda reactiva

El símbolo de moneda se propaga mediante `CompositionLocal`:

```kotlin
// core/presentation/LocalCurrencySymbol.kt
val LocalCurrencySymbol = compositionLocalOf { "$" }

// MainActivity.kt
val symbol = CurrencyCode.entries.find { it.displayCode == prefs.currencyCode }?.symbol ?: "$"
CompositionLocalProvider(LocalCurrencySymbol provides symbol) {
    NavGraph()
}

// Cualquier composable
val symbol = LocalCurrencySymbol.current
Text("$symbol${amount}")
```

Cambiar la moneda en Settings actualiza **toda la app instantáneamente** sin recomposición manual.

---

## Construcción y ejecución

### Requisitos
- Android Studio Ladybug o superior
- JDK 17
- Android SDK 35

### Clonar y buildear

```bash
git clone <repo-url>
cd FlowTa
./gradlew assembleDebug
```

### Comandos frecuentes

```bash
./gradlew assembleDebug                         # APK de desarrollo
./gradlew assembleRelease                       # APK de producción (requiere keystore)
./gradlew generateDebugFlowTaDatabaseInterface  # Regenerar código SQLDelight
./gradlew test                                  # Tests unitarios
```

### Instalar en dispositivo

```bash
./gradlew installDebug
# o desde Android Studio: Run > Run 'app'
```

---

## Workarounds de build

`app/build.gradle.kts` incluye dos workarounds necesarios para SQLDelight 2.x + KSP con Gradle 9.2.1:

```kotlin
// 1. Sourceset manual apuntando al output de SQLDelight
sourceSets {
    getByName("debug") {
        java.srcDirs("${layout.buildDirectory.get()}/generated/sqldelight/code/FlowTaDatabase/debug")
    }
}

// 2. Dependencia explícita de la tarea KSP sobre la generación de SQLDelight
afterEvaluate {
    tasks.named("kspDebugKotlin") {
        dependsOn("generateDebugFlowTaDatabaseInterface")
    }
}
```

> **No actualizar AGP a 9.x** — SQLDelight 2.x depende de `BaseExtension` que fue removida en AGP 9.

---

## Tests

La infraestructura de testing está configurada pero los tests están pendientes de escritura:

```kotlin
// test dependencies ya incluidas en build.gradle.kts:
testImplementation("junit:junit")
testImplementation("io.mockk:mockk")
testImplementation("app.cash.turbine:turbine")          // test de Flows
testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test")
testImplementation("app.cash.sqldelight:sqlite-driver") // DB en memoria para tests
```

Los use cases (un método `invoke` por clase) son los candidatos ideales para tests unitarios.

---

## Ícono

Símbolo **$** dorado 3D sobre fondo negro. Generado en todas las densidades estándar:

| Directorio | Tamaño |
|---|---|
| `mipmap-mdpi` | 48×48 px |
| `mipmap-hdpi` | 72×72 px |
| `mipmap-xhdpi` | 96×96 px |
| `mipmap-xxhdpi` | 144×144 px |
| `mipmap-xxxhdpi` | 192×192 px |

Ícono adaptativo (API 26+): foreground PNG 432×432 + background negro `#000000`.

---

## Versión

| Campo | Valor |
|---|---|
| `versionName` | 1.0.0 |
| `versionCode` | 1 |
| `minSdk` | 29 (Android 10) |
| `targetSdk` | 35 (Android 15) |
| `packageName` | `com.j4x.flowta` |

---

## Licencia

Proyecto privado — todos los derechos reservados.
