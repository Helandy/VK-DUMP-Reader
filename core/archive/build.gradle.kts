plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.etozhesandy.redpanda.core.archive"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

dependencies {
    api(project(":core:model"))
    implementation(project(":core:common"))
    implementation(project(":core:storage"))

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.documentfile)
    implementation(libs.jsoup)
    implementation(libs.zip4j)
    implementation(libs.junrar)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)

    testImplementation(libs.junit)
}

/**
 * Developer tool, not part of any check: runs the real extraction and format detection over a
 * folder of real archives and prints what each one was recognised as. See
 * `ArchiveDetectionMatrix` for why it exists.
 *
 *     ./gradlew :core:archive:detectionMatrix -Parchives="/path/to/exampl"
 *
 * Without `-Parchives` it passes immediately, so it stays harmless anywhere it gets run.
 */
tasks.register<Test>("detectionMatrix") {
    group = "verification"
    description = "Prints the detected layout of every archive in -Parchives=<dir>."
    val unitTest = tasks.named<Test>("testDebugUnitTest")
    testClassesDirs = files(unitTest.map { it.testClassesDirs })
    classpath = files(unitTest.map { it.classpath })
    filter { includeTestsMatching("*ArchiveDetectionMatrix*") }
    systemProperty("redpanda.archives", providers.gradleProperty("archives").getOrElse(""))
    testLogging { showStandardStreams = true }
    // The corpus on disk is the input, and Gradle cannot see it change.
    outputs.upToDateWhen { false }
}
