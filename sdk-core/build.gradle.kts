plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.kaoage.sdk.core"
    compileSdk = 35

    defaultConfig {
        minSdk = 29
        targetSdk = 35
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildFeatures {
        buildConfig = false
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }

    sourceSets["main"].assets.srcDir(rootProject.layout.projectDirectory.dir("models"))

    packaging {
        resources.excludes += setOf("META-INF/AL2.0", "META-INF/LGPL2.1")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime)
    implementation(libs.androidx.lifecycle.viewmodel)
    implementation(libs.androidx.camera.core)
    implementation(libs.coroutines.android)
    implementation(libs.coroutines.core)
    implementation(libs.coroutines.play.services)
    implementation(libs.mlkit.face.detection)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.tensorflow.lite)

    testImplementation(kotlin("test"))
    testImplementation("org.robolectric:robolectric:4.12.1")
    testImplementation("androidx.test:core:1.6.1")
}

private val ageModel =
    rootProject.layout.projectDirectory.file("models/model_age_nonq.tflite")
private val genderModel =
    rootProject.layout.projectDirectory.file("models/model_gender_nonq.tflite")

val verifyAgeModel by tasks.registering {
    doLast {
        if (!ageModel.asFile.exists()) {
            logger.warn(
                "[kaoage] models/model_age_nonq.tflite missing. Run scripts/download_models.sh to fetch the sanctioned age model."
            )
        }
    }
}

val verifyGenderModel by tasks.registering {
    doLast {
        if (!genderModel.asFile.exists()) {
            logger.warn(
                "[kaoage] models/model_gender_nonq.tflite missing. Run scripts/download_models.sh to fetch the sanctioned gender model."
            )
        }
    }
}

tasks.named("preBuild") {
    dependsOn(verifyAgeModel)
    dependsOn(verifyGenderModel)
}
