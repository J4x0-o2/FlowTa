import app.cash.sqldelight.gradle.SqlDelightWorkerTask
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.sqldelight)
}

// Release signing values come from the git-ignored root local.properties, falling back to env vars
// of the same name. With none set the release build stays unsigned. A partial set or a missing
// keystore file only fails release builds (validateReleaseSigning), so debug builds and unit tests
// keep working. The keystore is shared with MiCuadre: the integration provider is guarded by a
// signature permission, which only matches when both apps carry the same certificate.
val KEYSTORE_FILE_KEY = "FLOWTA_KEYSTORE_FILE"
val KEYSTORE_PASSWORD_KEY = "FLOWTA_KEYSTORE_PASSWORD"
val KEY_ALIAS_KEY = "FLOWTA_KEY_ALIAS"
val KEY_PASSWORD_KEY = "FLOWTA_KEY_PASSWORD"
val signingKeys = listOf(KEYSTORE_FILE_KEY, KEYSTORE_PASSWORD_KEY, KEY_ALIAS_KEY, KEY_PASSWORD_KEY)
val localProps = Properties().apply {
    rootProject.file("local.properties").takeIf { it.isFile }?.inputStream()?.use { load(it) }
}
// Only the path and the alias are trimmed; passwords pass through untouched (empty means absent).
val signingValues: Map<String, String> = signingKeys
    .mapNotNull { key ->
        (localProps.getProperty(key) ?: System.getenv(key))
            ?.let { if (key == KEYSTORE_FILE_KEY || key == KEY_ALIAS_KEY) it.trim() else it }
            ?.takeIf { it.isNotEmpty() }
            ?.let { key to it }
    }
    .toMap()
val releaseKeystore: File? = signingValues[KEYSTORE_FILE_KEY]?.let { rootProject.file(it) }
val signingProblem: String? = when {
    signingValues.isEmpty() -> null
    signingValues.size != signingKeys.size ->
        "Incomplete release signing config; missing: " +
            signingKeys.filter { it !in signingValues }.joinToString()
    releaseKeystore?.isFile != true -> "$KEYSTORE_FILE_KEY points to a missing file: $releaseKeystore"
    else -> null
}
val hasReleaseSigning = signingValues.size == signingKeys.size && signingProblem == null

val validateReleaseSigning by tasks.registering {
    description = "Fails when the release signing config is partial or its keystore file is missing."
    val problem = signingProblem
    doLast { if (problem != null) throw GradleException(problem) }
}

android {
    namespace  = "com.j4x.flowta"
    compileSdk = 35

    defaultConfig {
        applicationId             = "com.j4x.flowta"
        minSdk                    = 29
        targetSdk                 = 35
        versionCode               = 1
        versionName               = "1.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = releaseKeystore
                storePassword = signingValues.getValue(KEYSTORE_PASSWORD_KEY)
                keyAlias = signingValues.getValue(KEY_ALIAS_KEY)
                keyPassword = signingValues.getValue(KEY_PASSWORD_KEY)
            }
        }
    }

    buildTypes {
        release {
            if (hasReleaseSigning) signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        // Release-like build for local performance checks: R8 + resource shrinking, debug signing,
        // installs beside the debug build. Not for distribution.
        create("staging") {
            initWith(getByName("release"))
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += listOf("release")
            applicationIdSuffix = ".staging"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions { jvmTarget = "17" }

    buildFeatures { compose = true }

    testOptions {
        // android.util.Log in code under unit test must not throw "not mocked".
        unitTests.isReturnDefaultValues = true
    }

    sourceSets {
        getByName("debug") {
            java.srcDirs("${layout.buildDirectory.get()}/generated/sqldelight/code/FlowTaDatabase/debug")
        }
        getByName("release") {
            java.srcDirs("${layout.buildDirectory.get()}/generated/sqldelight/code/FlowTaDatabase/release")
        }
        getByName("staging") {
            java.srcDirs("${layout.buildDirectory.get()}/generated/sqldelight/code/FlowTaDatabase/staging")
        }
    }
}

afterEvaluate {
    // AGP creates preReleaseBuild late; `named` fails loudly if it is ever renamed instead of
    // silently shipping an unsigned release.
    tasks.named("preReleaseBuild") { dependsOn(validateReleaseSigning) }
    tasks.named("kspDebugKotlin") {
        dependsOn("generateDebugFlowTaDatabaseInterface")
    }
    tasks.named("kspReleaseKotlin") {
        dependsOn("generateReleaseFlowTaDatabaseInterface")
    }
    tasks.named("kspStagingKotlin") {
        dependsOn("generateStagingFlowTaDatabaseInterface")
    }
}

// SQLDelight schema/migration workers run with an empty environment under Gradle 9, which breaks
// sqlite-jdbc's native extraction on Windows. The shim (see tools/sqlite-worker-shim) fixes the
// temp dir. The plugin finalizes `classpath` itself, so it is extended once the graph is ready.
val sqliteWorkerShim: Configuration by configurations.creating {
    isCanBeConsumed = false
}

dependencies {
    sqliteWorkerShim(project(":tools:sqlite-worker-shim"))
}

tasks.withType<SqlDelightWorkerTask>().configureEach {
    dependsOn(sqliteWorkerShim)
}

gradle.taskGraph.whenReady {
    allTasks.filterIsInstance<SqlDelightWorkerTask>().forEach { it.classpath.from(sqliteWorkerShim) }
}

sqldelight {
    databases {
        create("FlowTaDatabase") {
            packageName.set("com.j4x.flowta.database")
            schemaOutputDirectory.set(file("src/main/sqldelight/databases"))
            verifyMigrations.set(true)
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.profileinstaller)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons)

    implementation(libs.navigation.compose)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.hilt.work)
    ksp(libs.hilt.work.compiler)

    implementation(libs.sqldelight.android.driver)
    implementation(libs.sqldelight.coroutines)

    implementation(libs.datastore.preferences)
    implementation(libs.workmanager.ktx)
    implementation(libs.coroutines.android)

    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.sqldelight.sqlite.driver)

    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)
}
