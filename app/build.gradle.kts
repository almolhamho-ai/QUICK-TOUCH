plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.serialization")
}

// رقم الإصدار يزيد تلقائياً مع كل بناء عبر GitHub Actions (GITHUB_RUN_NUMBER يزيد بكل تشغيل
// تلقائياً من GitHub نفسو)، وبيرجع لقيمة ثابتة 100 بس إذا بنيت المشروع محلياً بدون CI.
// هيك كل APK جديد فيه رقم إصدار أعلى من يلي قبلو، فبيقدر يركب فوق النسخة القديمة بدون
// ما تضطر تحذف التطبيق يدوياً قبل كل تجربة.
val autoVersionCode = System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull() ?: 100

android {
    namespace = "com.example.quickgestures"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.quickgestures"
        minSdk = 26
        targetSdk = 34
        versionCode = autoVersionCode
        versionName = "1.0.$autoVersionCode"
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.02.00")
    implementation(composeBom)

    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
    implementation("androidx.savedstate:savedstate-ktx:1.2.1")
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("androidx.fragment:fragment-ktx:1.6.2")
    implementation("androidx.biometric:biometric:1.1.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")

    debugImplementation("androidx.compose.ui:ui-tooling")
}
