plugins { id("com.android.application"); kotlin("android"); kotlin("plugin.serialization"); id("org.jetbrains.kotlin.plugin.compose") }
android {
 namespace="dev.coddy.android"
 compileSdk=36
 ndkVersion="28.2.13676358"
 defaultConfig {
  applicationId="dev.coddy.android";minSdk=26;targetSdk=36;versionCode=1;versionName="0.1.0"
  testInstrumentationRunner="androidx.test.runner.AndroidJUnitRunner"
  ndk { abiFilters += listOf("arm64-v8a","x86_64") }
  externalNativeBuild { cmake { arguments += listOf("-DANDROID_STL=c++_shared"); cppFlags += "-std=c++17" } }
 }
 buildFeatures { compose=true }
 externalNativeBuild { cmake { path=file("src/main/cpp/CMakeLists.txt");version="3.22.1" } }
 compileOptions { sourceCompatibility=JavaVersion.VERSION_21;targetCompatibility=JavaVersion.VERSION_21 }
 kotlin { jvmToolchain(21) }
 packaging { resources.excludes += setOf("META-INF/AL2.0","META-INF/LGPL2.1") }
}
dependencies {
 implementation(project(":core"))
 implementation(platform("androidx.compose:compose-bom:2025.10.01"))
 implementation("androidx.activity:activity-compose:1.11.0")
 implementation("androidx.compose.material3:material3")
 implementation("androidx.compose.material:material-icons-extended")
 implementation("androidx.compose.ui:ui-tooling-preview")
 implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.4")
 implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.4")
 implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
 implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.1")
 implementation("androidx.core:core-ktx:1.17.0")
 debugImplementation("androidx.compose.ui:ui-tooling")
 androidTestImplementation(platform("androidx.compose:compose-bom:2025.10.01"))
 androidTestImplementation("androidx.compose.ui:ui-test-junit4")
 androidTestImplementation("androidx.test.ext:junit:1.3.0")
 androidTestImplementation("androidx.test:runner:1.7.0")
 debugImplementation("androidx.compose.ui:ui-test-manifest")
}
