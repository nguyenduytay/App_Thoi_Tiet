plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.google.gms.google.services) // Plugin Google Services
}

android {
    namespace = "com.example.weather2"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.weather2"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
    buildFeatures{
        viewBinding = true
        dataBinding = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    implementation("com.github.Dimezis:BlurView:version-2.0.6")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.constraintlayout:constraintlayout-compose:1.1.1")
//cài đặt layout load
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")

    // add realtime database
    implementation("com.google.firebase:firebase-database:21.0.0")

    //add cloud firestore
    implementation("com.google.firebase:firebase-firestore:25.1.2")

    // FirebaseUI for Firebase Realtime Database
    implementation("com.firebaseui:firebase-ui-database:8.0.2")

    // FirebaseUI for Cloud Firestore
    implementation("com.firebaseui:firebase-ui-firestore:8.0.2")

    // FirebaseUI for Firebase Auth
    implementation("com.firebaseui:firebase-ui-auth:8.0.2")

    // FirebaseUI for Cloud Storage
    implementation("com.firebaseui:firebase-ui-storage:8.0.2")

    implementation(platform("com.google.firebase:firebase-bom:32.2.0")) // Cập nhật BOM để đồng bộ phiên bản
    implementation("com.google.firebase:firebase-storage")
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")

    // Thêm Firebase App Check nếu thiếu
    implementation("com.google.firebase:firebase-appcheck-playintegrity:16.0.0")

}
