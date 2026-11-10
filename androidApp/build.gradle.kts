import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeCompiler)
    id("com.google.gms.google-services") version "4.4.4"
    id("com.google.firebase.crashlytics") version "3.0.6"
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_11
    }
}
dependencies {
    implementation(project(":shared"))

    implementation(libs.androidx.activity.compose)

    implementation(libs.compose.uiToolingPreview)
    debugImplementation(libs.compose.uiTooling)
}

val firma = Properties().apply {
    val archivo = rootProject.file("keystore.properties")
    if (archivo.exists()) archivo.inputStream().use { load(it) }
}
val apiUrl = providers.gradleProperty("pharmaApiUrl").orElse("http://10.0.2.2:8080/api/v1/")
val firmaDisponible = firma.isNotEmpty()
tasks.configureEach {
    if (name == "packageRelease" || name == "packageReleaseBundle") {
        val tieneFirma = firmaDisponible
        doFirst {
            check(tieneFirma) { "Configura keystore.properties para generar una entrega firmada." }
        }
    }
}

android {
    signingConfigs {
        if (firma.isNotEmpty()) create("release") {
            storeFile = rootProject.file(firma.getProperty("storeFile"))
            storePassword = firma.getProperty("storePassword")
            keyAlias = firma.getProperty("keyAlias")
            keyPassword = firma.getProperty("keyPassword")
        }
    }
    namespace = "pe.edu.upeu.pharmamobil"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "pe.edu.upeu.pharmamobil"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0.0"
        resValue("string", "pharma_api_url", apiUrl.get())
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        release {
            signingConfig = signingConfigs.findByName("release")
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        resValues = true
        compose = true
    }
}
