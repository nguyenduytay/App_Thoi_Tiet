plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.google.gms.google.services) // Plugin Google Services
    alias(libs.plugins.ksp)
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
    implementation(libs.firebase.messaging.ktx)
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

     //xây dựng giao diện theo phong cách Material Design
    implementation("com.google.android.material:material:1.12.0")

    implementation("androidx.viewpager2:viewpager2:1.1.0")


    val room_version = "2.6.1"

    implementation("androidx.room:room-runtime:$room_version")

    // If this project uses any Kotlin source, use Kotlin Symbol Processing (KSP)
    // See Add the KSP plugin to your project
    ksp("androidx.room:room-compiler:$room_version")

    // If this project only uses Java source, use the Java annotationProcessor
    // No additional plugins are necessary
    annotationProcessor("androidx.room:room-compiler:$room_version")

    // optional - Kotlin Extensions and Coroutines support for Room
    implementation("androidx.room:room-ktx:$room_version")

    // optional - RxJava2 support for Room
    implementation("androidx.room:room-rxjava2:$room_version")

    // optional - RxJava3 support for Room
    implementation("androidx.room:room-rxjava3:$room_version")

    // optional - Guava support for Room, including Optional and ListenableFuture
    implementation("androidx.room:room-guava:$room_version")

    // optional - Test helpers
    testImplementation("androidx.room:room-testing:$room_version")

    // optional - Paging 3 Integration
    implementation("androidx.room:room-paging:$room_version")

    implementation("androidx.work:work-runtime-ktx:2.7.1")

    implementation("com.google.gms:google-services:4.3.15")

    implementation("com.google.firebase:firebase-messaging-ktx")

    // Paho MQTT Android Service
    implementation("org.eclipse.paho:org.eclipse.paho.client.mqttv3:1.2.5")
    implementation("org.eclipse.paho:org.eclipse.paho.android.service:1.1.1")

    // Androidx LocalBroadcastManager (bắt buộc)
    implementation("androidx.localbroadcastmanager:localbroadcastmanager:1.1.0")

    implementation("io.netty:netty-all:4.1.68.Final")
    // Hỗ trợ SSL
    implementation("org.conscrypt:conscrypt-android:2.5.2")

    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")
    implementation("androidx.fragment:fragment-ktx:1.6.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

}
