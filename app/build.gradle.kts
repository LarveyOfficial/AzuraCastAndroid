plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.android)
  alias(libs.plugins.kotlin.compose)
  id("com.google.devtools.ksp")
  id("com.google.dagger.hilt.android")
}

android {
  namespace = "com.larvey.azuracastplayer"
  compileSdk = 36

  defaultConfig {
    applicationId = "com.larvey.azuracastplayer"
    minSdk = 26
    targetSdk = 36
    // versionCode must strictly increase for the Play Store. CI passes VERSION_CODE
    // computed from the main commit count (+ 75), which is workflow-independent and
    // strictly monotonic across the release AND beta pipelines — unlike
    // GITHUB_RUN_NUMBER, which is per-workflow and resets if a workflow is renamed.
    // Local and PR builds fall back to 75.
    versionCode = System.getenv("VERSION_CODE")?.toIntOrNull() ?: 75
    // versionName is managed by release-please — it is bumped in the release PR.
    // Do not edit the version string by hand, and keep the trailing comment.
    versionName = "1.3.0" // x-release-please-version
    // Beta CI builds relabel the version (e.g. 1.3.0-beta.245) via this env
    // override; stable and local builds keep the release-please-managed value above.
    System.getenv("VERSION_NAME_OVERRIDE")?.takeIf { it.isNotBlank() }?.let { versionName = it }

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  signingConfigs {
    create("release") {
      // Populated from GitHub Secrets in CI (see .github/workflows/release-please.yml).
      // When the keystore env var is absent (e.g. local builds) the release build
      // stays unsigned and this config is not attached (see buildTypes.release below).
      System.getenv("SIGNING_KEYSTORE_PATH")?.let { keystorePath ->
        storeFile = file(keystorePath)
        storeType = "PKCS12"
        storePassword = System.getenv("SIGNING_KEYSTORE_PASSWORD")
        keyAlias = System.getenv("SIGNING_KEY_ALIAS")
        keyPassword = System.getenv("SIGNING_KEY_PASSWORD")
      }
    }
  }

  buildTypes {
    release {
      isMinifyEnabled = true
      isShrinkResources = true
      if (System.getenv("SIGNING_KEYSTORE_PATH") != null) {
        signingConfig = signingConfigs.getByName("release")
      }
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
  composeOptions {
    kotlinCompilerExtensionVersion = "1.5.1"
  }
  packaging {
    resources {
      excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
  }
}

dependencies {

  implementation(libs.androidx.material3.adaptive.navigation.suite.android)

  implementation(libs.androidx.navigation.compose)

  implementation(libs.androidx.room.runtime)
  implementation(libs.haze)
  implementation(libs.haze.materials)

  ksp(libs.room.compiler)

  implementation(libs.room.ktx)

  implementation(libs.hilt.android)
  ksp(libs.hilt.compiler)

  implementation("io.coil-kt.coil3:coil-compose:3.1.0")
  implementation("io.coil-kt.coil3:coil-network-okhttp:3.1.0")

//  implementation("com.github.android:renderscript-intrinsics-replacement-toolkit:b6363490c3")
  implementation(files("libs/renderscript-toolkit-release.aar"))

  implementation("androidx.compose.material3:material3:1.4.0-alpha11")

  implementation("androidx.window:window:1.4.0-rc01")

  implementation("androidx.compose.material3.adaptive:adaptive:1.1.0")
  implementation("androidx.compose.material3.adaptive:adaptive-layout:1.1.0")
  implementation("androidx.compose.material3.adaptive:adaptive-navigation:1.1.0")

  implementation("androidx.compose.material3:material3-window-size-class:1.4.0-alpha11")

  implementation(libs.reorderable)

  implementation(libs.androidx.media3.exoplayer.hls)

  // Google Cast (Chromecast) support.
  implementation(libs.androidx.mediarouter)
  implementation(libs.play.services.cast.framework)

  implementation(libs.androidx.palette)

  implementation(libs.androidx.activity)

  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.material.icons.extended)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.activity.compose)
  implementation(platform(libs.androidx.compose.bom))
  implementation(libs.androidx.ui)
  implementation(libs.androidx.ui.graphics)
  implementation(libs.androidx.ui.tooling.preview)
  implementation(libs.androidx.material3)
  implementation(libs.androidx.media3.exoplayer)
  implementation(libs.androidx.media3.common)
  implementation(libs.androidx.media3.session)
  implementation(libs.androidx.datastore.preferences)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.retrofit)
  implementation(libs.converter.gson)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.mockwebserver)
  testImplementation(libs.truth)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.ui.test.junit4)
  debugImplementation(libs.androidx.ui.tooling)
  debugImplementation(libs.androidx.ui.test.manifest)
}
