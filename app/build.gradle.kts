plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.android)
  alias(libs.plugins.kotlin.compose)
  id("com.google.devtools.ksp")
  id("com.google.dagger.hilt.android")
}

android {
  namespace = "com.larvey.azuracastplayer"
  compileSdk = 35

  defaultConfig {
    applicationId = "com.larvey.azuracastplayer"
    minSdk = 26
    targetSdk = 35
    versionCode = 65
    versionName = "beta-2.0"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  buildTypes {
    release {
      isMinifyEnabled = true
      isShrinkResources = true
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

  implementation(libs.navigation.compose)

  implementation(libs.androidx.room.runtime)
  implementation(libs.haze)
  implementation(libs.haze.materials)

  ksp(libs.room.compiler)

  implementation(libs.room.ktx)

  implementation(
    "com.fasterxml.jackson.module",
    "jackson-module-kotlin",
    "2.11.0"
  )

  implementation(libs.hilt.android)
  ksp(libs.hilt.compiler)

  implementation("io.coil-kt.coil3:coil-compose:3.1.0")
  implementation("io.coil-kt.coil3:coil-network-okhttp:3.1.0")


  implementation("com.github.android:renderscript-intrinsics-replacement-toolkit:b6363490c3")

  implementation("androidx.compose.material3:material3:1.4.0-alpha10")

  implementation("androidx.window:window:1.4.0-rc01")

  implementation("androidx.compose.material3.adaptive:adaptive:1.1.0")
  implementation("androidx.compose.material3.adaptive:adaptive-layout:1.1.0")
  implementation("androidx.compose.material3.adaptive:adaptive-navigation:1.1.0")

  implementation("androidx.compose.material3:material3-window-size-class:1.4.0-alpha10")

  implementation(libs.reorderable)

  implementation(libs.androidx.media3.exoplayer.hls)

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
  implementation(libs.androidx.media3.ui)
  implementation(libs.androidx.media3.common)
  implementation(libs.androidx.media3.session)
  implementation(libs.androidx.datastore.preferences)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.retrofit)
  implementation(libs.converter.gson)
  testImplementation(libs.junit)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.ui.test.junit4)
  debugImplementation(libs.androidx.ui.tooling)
  debugImplementation(libs.androidx.ui.test.manifest)
}