plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

val baseApplicationId = providers.gradleProperty("redpanda.applicationId").get()
val appVersionName = providers.gradleProperty("redpanda.versionName").get()
val appVersionCode = providers.gradleProperty("redpanda.versionCode").get().toInt()

android {
    namespace = "com.etozhesandy.redpanda"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = baseApplicationId
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = appVersionCode
        versionName = appVersionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    buildFeatures {
        compose = true
    }
}

androidComponents {
    onVariants { variant ->
        variant.outputs.forEach { output ->
            val fileName = "RedPanda-${output.versionName.orNull ?: appVersionName}-${variant.buildType}.apk"
            (output as? com.android.build.api.variant.impl.VariantOutputImpl)?.outputFileName?.set(fileName)
        }
    }
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:common"))
    implementation(project(":core:storage"))
    implementation(project(":core:archive"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:navigation"))
    implementation(project(":core:settings"))
    implementation(project(":core:security"))

    implementation(project(":features:home"))
    implementation(project(":features:importer"))
    implementation(project(":features:dialogs"))
    implementation(project(":features:profile"))
    implementation(project(":features:chat"))
    implementation(project(":features:favorites"))
    implementation(project(":features:settings"))
    implementation(project(":features:lock"))

    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)
    implementation(libs.material)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.hilt.work)

    implementation(libs.coil.compose)
    implementation(libs.coil.video)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}
