plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

val numeroBuild = System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull() ?: 1

android {
    namespace = "com.fkarach.arquibancada"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.fkarach.arquibancada"
        minSdk = 26
        targetSdk = 35
        versionCode = numeroBuild
        versionName = "0.1.$numeroBuild"
    }

    // Chave fixa (fica nos segredos do GitHub, fora do código): permite atualizar o app sem desinstalar.
    val caminhoChave = System.getenv("ARQ_KEYSTORE_PATH")
    signingConfigs {
        if (caminhoChave != null) {
            create("pessoal") {
                storeFile = file(caminhoChave)
                storePassword = System.getenv("ARQ_KEYSTORE_PASSWORD")
                keyAlias = "arquibancada"
                keyPassword = System.getenv("ARQ_KEYSTORE_PASSWORD")
            }
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            signingConfig = if (caminhoChave != null) signingConfigs.getByName("pessoal") else signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.glance:glance-appwidget:1.1.1")
    implementation("androidx.work:work-runtime-ktx:2.10.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
}
