import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

// تحميل بيانات التوقيع من keystore.properties إن وُجد (غير مرفوع إلى git).
val keystorePropsFile = rootProject.file("keystore.properties")
val keystoreProps = Properties().apply {
    if (keystorePropsFile.exists()) keystorePropsFile.inputStream().use { load(it) }
}

android {
    namespace = "com.gnutux.tahakom"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.gnutux.tahakom"
        minSdk = 26
        targetSdk = 36
        versionCode = 9
        versionName = "0.8.0"

        vectorDrawables { useSupportLibrary = true }
        // اللغات المدعومة: الإنجليزية (افتراضي) + العربية
        resourceConfigurations += listOf("en", "ar")
    }

    signingConfigs {
        // يُفعّل التوقيع فقط عند توفّر keystore.properties (محلياً) — وإلا يبني debug عادياً.
        if (keystorePropsFile.exists()) {
            create("release") {
                storeFile = rootProject.file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (keystorePropsFile.exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
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
    // أساسيات أندرويد + دورة الحياة
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat) // per-app locale (تبديل اللغة)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // Jetpack Compose (عبر BOM)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)

    // حقن التبعيات (Hilt)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    // التخزين: Room + DataStore
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.datastore.preferences)

    // الشبكة والتزامن (لوسائل النقل عبر WiFi)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.okhttp)

    // أدوات التطوير
    debugImplementation(libs.androidx.ui.tooling)
}
