import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.androidx.navigation.safeargs) // safeArgs
    id("com.google.gms.google-services") //firebase

    //hilt
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
}

// local.properties에서 BASE_URL 읽기
val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) {
        file.inputStream().use { load(it) }
    }
}

android {
    namespace = "com.baek.diract"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.baek.diract"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        val baseUrl = localProperties.getProperty("BASE_URL")
            ?: error("BASE_URL is missing in local.properties")

        buildConfigField(
            "String",
            "BASE_URL",
            "\"$baseUrl\""
        )

        val webClientId = localProperties.getProperty("WEB_CLIENT_ID")
            ?: error("WEB_CLIENT_ID is missing in local.properties")

        buildConfigField(
            "String",
            "WEB_CLIENT_ID",
            "\"$webClientId\""
        )
    }
    signingConfigs {
        create("release") {
            val props = Properties().apply {
                load(rootProject.file("keystore.properties").inputStream())
            }
            storeFile = file(props["storeFile"] as String)
            storePassword = props["storePassword"] as String
            keyAlias = props["keyAlias"] as String
            keyPassword = props["keyPassword"] as String
        }
    }


    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
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
    kotlinOptions {
        jvmTarget = "11"
    }

    //뷰바인딩
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.splashscreen)

    //credential manager (Google 로그인)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.google.googleid)
    implementation(libs.androidx.swiperefreshlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    //네비게이션 의존성
    implementation(libs.androidx.navigation.fragment)
    implementation(libs.androidx.navigation.ui)

    //firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.messaging)

    //hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)
    implementation(libs.hilt.navigation.fragment)

    //glide
    implementation(libs.glide)
    ksp(libs.glide.ksp)

    //retrofit + okhttp
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp.logging.interceptor)

    //datastore
    implementation(libs.datastore.preferences)

    //media3
    implementation(libs.media3.transformer)
    implementation(libs.media3.effect)
    implementation(libs.media3.common)
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.ui)
}
