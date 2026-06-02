# FlowTa

## Qué es este proyecto
FlowTa es una app Android de finanzas personales con interfaz de chat. Las transacciones económicas se registran y visualizan como burbujas de conversación: ingresos a la derecha en verde, egresos a la izquierda en rojo. El usuario gestiona cuentas propias, registra movimientos, hace transferencias internas y define metas de ahorro.

## El problema que resuelve
La mayoría de apps de finanzas personales tienen interfaces densas y complejas que disuaden el uso diario. FlowTa adopta el paradigma visual del chat — familiar e inmediato — para que registrar una transacción sea tan natural como mandar un mensaje.

## Usuario objetivo
Usuario individual que quiere llevar control de sus finanzas personales sin fricción, principalmente en español latinoamericano (COP como moneda por defecto).

---

## Stack tecnológico

| Capa | Tecnología | Razón |
|---|---|---|
| UI / Framework | Kotlin + Jetpack Compose | Declarativo, compatible con MVI, theming dark/light nativo |
| Base de datos | SQLDelight 2.x | Type-safe, flows reactivos, queries validadas en compilación |
| Preferencias | DataStore Preferences | Reemplazo oficial de SharedPreferences, reactivo |
| Arquitectura | Clean Architecture + MVI manual | Lógica de negocio aislada, estado predecible, sin librería externa |
| DI | Hilt | Framework oficial de Google, compatible con Compose + WorkManager |
| Notificaciones | WorkManager + NotificationManager | Persiste tras cierre de app y reinicio del dispositivo |

**Versiones clave**: AGP 8.5.0, Kotlin 2.0.0, KSP 2.0.0-1.0.24, Compose BOM 2024.09.00, SQLDelight 2.0.2, Hilt 2.51.1

---

## Arquitectura

### Patrón
Clean Architecture + MVI con organización por features. Un solo módulo Gradle.

**Dirección de dependencia**: `Presentation → Domain ← Data`. El Domain no conoce ni la UI ni la persistencia.

### Capas

- **Domain** — modelos, interfaces de repositorios, casos de uso. Cero imports de Android, SQLDelight, Hilt o Compose.
- **Data** — implementaciones de repositorios, queries SQLDelight, mappers DB→Domain, DataStore.
- **Presentation** — ViewModels, Contracts (State/Intent/Effect), Screens Compose.

### MVI manual
Cada feature tiene un `FeatureContract.kt` con tres clases:
```kotlin
data class FeatureState(...)          // único objeto inmutable de UI
sealed interface FeatureIntent { ... } // acciones del usuario
sealed interface FeatureEffect { ... } // efectos únicos: navegación, toasts
```

### Estructura de carpetas
```
app/src/main/java/com/j4x/flowta/
├── core/
│   ├── data/db/            FlowTaDatabase, DriverFactory
│   ├── domain/model/       Currency, CurrencyCode
│   ├── presentation/
│   │   ├── navigation/     NavGraph, Screen
│   │   ├── theme/          Color, Theme, Type
│   │   └── components/     TransactionBubble, BottomInputBar
│   └── receiver/           BootReceiver
├── feature/
│   ├── transactions/       F04, F05, F06
│   ├── accounts/           F02, F08
│   ├── transfers/          F07
│   ├── types/              F03
│   ├── settings/           F01
│   ├── profile/            F09
│   ├── goals/              F10, F11, F12
│   └── metrics/            F13, F14
├── di/                     DatabaseModule, RepositoryModule
└── FlowTaApp.kt / MainActivity.kt

app/src/main/sqldelight/com/j4x/flowta/database/
    Account.sq, Transaction.sq, TransactionType.sq,
    InternalTransfer.sq, Goal.sq, GoalContribution.sq
```

---

## Plan de features (MVP)

| ID | Feature | Complejidad | Estado |
|---|---|---|---|
| F01 | Settings (tema, moneda, idioma) | Baja | Skeleton |
| F02 | Gestión de cuentas | Media | Skeleton |
| F03 | Tipos de transacción | Baja | Skeleton |
| F04 | Registro de transacción (operación atómica) | Alta | Skeleton |
| F05 | Historial global (chat) | Alta | Skeleton |
| F06 | Detalle de transacción (overlay) | Media | Skeleton |
| F07 | Transferencias internas | Alta | Skeleton |
| F08 | Historial interno por cuenta | Baja | Skeleton |
| F09 | Perfil (capital + cuentas + transferencias) | Media | Skeleton |

**Orden de construcción**: F01 → F02 → F03 → F04 → F05 → F06 → F07 → F08 → F09

---

## Esquema de base de datos

| Tabla | Descripción |
|---|---|
| `account` | Cuentas del usuario (CASH/BANK/DIGITAL_WALLET/SAVINGS/OTHER) |
| `transaction_type` | Categorías (INCOME/EXPENSE/RECURRING_EXPENSE) |
| `user_transaction` | Transacciones — FK a account y transaction_type |
| `internal_transfer` | Transferencias entre cuentas propias |
| `goal` | Metas de ahorro |
| `goal_contribution` | Aportes a metas — FK a goal y account |

Las operaciones que afectan dos tablas (registrar transacción, ejecutar transferencia, aportar a meta) usan `database.transaction { }` de SQLDelight para atomicidad.

---

## Cómo trabajar en este proyecto

### Arrancar el proyecto
```bash
./gradlew assembleDebug
```
O usar el botón Run en Android Studio con un dispositivo/emulador conectado (API 29+).

### Correr tests
```bash
./gradlew test              # unit tests
./gradlew connectedAndroidTest  # tests instrumentados
```

### Estrategia de git
- `main` — siempre estable
- `develop` — integración continua
- `feature/nombre-descriptivo` — una rama por feature

Convención de commits: `feat:`, `fix:`, `refactor:`, `test:`, `chore:`

---

## Decisiones importantes y sus razones

1. **SQLDelight sobre Room**: Las queries complejas de métricas (JOINs, GROUP BY, SUM condicional) son más expresivas y mantenibles en SQL puro. SQLDelight las valida en compilación eliminando errores en runtime.

2. **MVI manual sin librería**: Tres clases (State, Intent, Effect) son suficientes. Añadir una librería (Orbit, MVI Kotlin) agrega una abstracción innecesaria para un solo desarrollador.

3. **Un solo módulo Gradle**: Multi-módulo resuelve problemas de build time y encapsulación en equipos grandes. Para este proyecto agrega fricción sin beneficio real.

4. **`minSdk = 29`**: Garantiza acceso a `AppCompatDelegate.setApplicationLocales()` vía compat library y APIs de DataStore estables. Excluye Android 9 y anteriores (~3% de dispositivos en 2024).

5. **`dynamicColor = false` por defecto**: El esquema de colores semántico (verde ingresos, rojo egresos) es parte del diseño del producto — el Material You dinámico podría romper este contrato visual.

6. **Package `com.j4x.flowta`**: Identidad del desarrollador (j4x), no `com.flowta.app` como en los docs iniciales.

7. **AGP 8.5.0 en lugar de 9.0.1**: SQLDelight 2.x usa `BaseExtension` (removida en AGP 9.0) para detectar la versión de SQLite. No existe versión de SQLDelight compatible con AGP 9.x al momento del scaffolding. Se usa AGP 8.5.0 hasta que SQLDelight publique soporte.

8. **Workaround SQLDelight + KSP**: SQLDelight 2.0.2 no registra sus sources generados en el classpath de KSP automáticamente con Gradle 9.2.1. Solución: `sourceSets.debug.java.srcDirs` explícito + `kspDebugKotlin.dependsOn(generateDebugFlowTaDatabaseInterface)` en `app/build.gradle.kts`.

9. **Tipos generados SQLDelight**: Queries con nombre duplicado entre `.sq` files generan clases en subpaquetes. Ej: `Transaction.sq#selectAll` → `com.j4x.flowta.database.transaction.SelectAll`, `InternalTransfer.sq#selectAll` → `com.j4x.flowta.database.internalTransfer.SelectAll`. Tipos únicos van al paquete raíz.

## Advertencias conocidas

- **SQLDelight exige el esquema completo desde el inicio.** Cambios de esquema requieren archivos `.sqm` de migración versionados.
- **MVI manual = boilerplate repetitivo.** Cada feature tiene su Contract — es predecible pero es código.
- **Sin módulos Gradle, la disciplina de imports es manual.** Nada impide técnicamente que una pantalla importe un repositorio directamente.

## Decisiones abiertas

- ¿El usuario puede editar o eliminar transacciones registradas? (F04/F05/F06 asumen solo creación)
- ¿Hay tipos de transacción predeterminados al primer arranque? (F03 asume que el usuario los crea)
- ¿Desde dónde se accede a Metas? (F09/F10 — desde Perfil o desde la barra de navegación principal)
