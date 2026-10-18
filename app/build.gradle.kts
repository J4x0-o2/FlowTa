plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.sqldelight)
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

    buildTypes {
        release {
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

sqldelight {
    databases {
        create("FlowTaDatabase") {
            packageName.set("com.j4x.flowta.database")
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
