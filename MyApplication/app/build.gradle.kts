import org.gradle.kotlin.dsl.implementation

configurations.all {
    resolutionStrategy {
        force("org.jetbrains.kotlin:kotlin-stdlib:2.2.21")
        force("org.jetbrains.kotlin:kotlin-reflect:2.2.21")
    }
}

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.chaquopy)
    alias(libs.plugins.ksp)

}

android {
    namespace = "com.example.myapplication"
    compileSdk = 35

    androidResources{
        noCompress+="litertlm"
    }

    defaultConfig {
        applicationId = "com.example.myapplication"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        ndk {
            abiFilters += listOf("arm64-v8a", "x86_64")
        }


    }

    buildTypes {
        release {
            isMinifyEnabled = false
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
    buildFeatures {
        compose = true
    }
}
configure<com.chaquo.python.ChaquopyExtension> {
    defaultConfig {
        buildPython("C:/Users/sjeot/AppData/Local/Programs/Python/Python311/python.exe")
        version = "3.11"
        pip {
            install("pycountry")
            install("requests")
            install("geonamescache")
        }
    }
}
dependencies {

    // Core + Lifecycle
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.ui)
    implementation("androidx.appcompat:appcompat:1.6.1")

    implementation("androidx.navigation:navigation-fragment-ktx:2.5.3")
    implementation("androidx.navigation:navigation-ui-ktx:2.5.3")

    // Compose (via BOM)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material3.adaptive.navigation.suite)

    // Icons
    implementation("androidx.compose.material:material-icons-extended:1.4.3")

    //Navigation
    implementation("androidx.navigation:navigation-compose:2.7.3")
    implementation("androidx.compose.ui:ui:1.5.0")
    implementation("androidx.compose.material3:material3:1.3.0")

    //location
    implementation("com.google.android.gms:play-services-location:21.0.1")

    //long term storage
    implementation("androidx.datastore:datastore-preferences:1.1.1")


    // MediaPipe
    implementation("com.google.mediapipe:tasks-vision:0.10.14")
    //LLM

    implementation("com.google.ai.edge.litertlm:litertlm-android:0.9.0-alpha04")

    //sql db
    implementation("org.postgresql:postgresql:42.7.3")



    //tflite
    implementation("com.google.ai.edge.litert:litert:1.0.1")
    implementation("com.google.ai.edge.litert:litert-support:1.0.1")

    // CameraX
    implementation("androidx.camera:camera-core:1.3.1")
    implementation("androidx.camera:camera-camera2:1.3.1")
    implementation("androidx.camera:camera-lifecycle:1.3.1")
    implementation("androidx.camera:camera-view:1.3.1")

    // Existing dependencies...
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")

    // Networking
    implementation("org.java-websocket:Java-WebSocket:1.5.4")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.compose.material3)
    implementation(libs.play.services.location)
    implementation(libs.androidx.ui)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    //Search
    implementation("androidx.compose.ui:ui:1.5.0")
    implementation("androidx.navigation:navigation-compose:2.7.3")

    //Alarm
    implementation("androidx.work:work-runtime-ktx:2.9.0")
    //implementation("androidx.core:core-ktx:1.12.0")
    //implementation("androidx.activity:activity-compose:1.8.2")
   // implementation("androidx.compose.material3:material3:1.1.2")

    implementation("androidx.room:room-runtime:2.7.1")
    ksp("androidx.room:room-compiler:2.7.1")
    implementation("androidx.room:room-ktx:2.7.1")

    implementation("net.zetetic:android-database-sqlcipher:4.5.4")
    implementation("androidx.sqlite:sqlite-ktx:2.4.0")

}
