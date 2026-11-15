plugins {
    id("com.android.application")
    kotlin("android")
    id("org.jetbrains.kotlin.plugin.compose")
}
android {
    namespace = "app.anotherme.demo"
    compileSdk = 35
    defaultConfig {
        applicationId = "app.anotherme.demo"
        minSdk = 29
        targetSdk = 35
        versionCode = 1
        versionName = "0.1-demo"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true }
    // Keep generated demo credentials inside the writable checkout, never a personal keystore.
    signingConfigs.getByName("debug") {
        storeFile = rootProject.layout.projectDirectory.file(".local/debug.keystore").asFile
        storeFile?.parentFile?.mkdirs()
    }
}
dependencies {
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.compose.ui:ui:1.7.8")
    implementation("androidx.compose.foundation:foundation:1.7.8")
    implementation("androidx.compose.material3:material3:1.3.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.7")
    implementation("androidx.datastore:datastore-preferences:1.1.4")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4:1.7.8")
    debugImplementation("androidx.compose.ui:ui-test-manifest:1.7.8")
}

val generateDemoDebugKeystore by tasks.registering(Exec::class) {
    val key = rootProject.file(".local/debug.keystore")
    onlyIf { !key.exists() }
    executable = "${System.getProperty("java.home")}/bin/keytool"
    args("-genkeypair", "-noprompt", "-keystore", key.absolutePath,
        "-storetype", "JKS", "-storepass", "android", "-keypass", "android",
        "-alias", "androiddebugkey", "-keyalg", "RSA", "-keysize", "2048",
        "-validity", "10000", "-dname", "CN=Android Debug,O=Android,C=US")
}
tasks.matching { it.name.startsWith("validateSigningDebug") }.configureEach {
    dependsOn(generateDemoDebugKeystore)
}
