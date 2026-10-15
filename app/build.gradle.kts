import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
}

// Read AWS configuration from the project-root aws.properties file.
val awsProperties = Properties().apply {
    val awsFile = rootProject.file("aws.properties")

    require(awsFile.exists()) {
        "aws.properties was not found in the project root."
    }

    awsFile.inputStream().use { load(it) }
}

val awsAccessKey = awsProperties.getProperty("AWS_ACCESS_KEY_ID")
    ?: error("AWS_ACCESS_KEY_ID is missing from aws.properties")

val awsSecretKey = awsProperties.getProperty("AWS_SECRET_ACCESS_KEY")
    ?: error("AWS_SECRET_ACCESS_KEY is missing from aws.properties")

val awsRegion = awsProperties.getProperty("AWS_REGION", "eu-north-1")
val awsBucket = awsProperties.getProperty("AWS_S3_BUCKET", "basmallah")

android {
    namespace = "com.example.unidash"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.unidash"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner =
            "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField(
            "String",
            "AWS_ACCESS_KEY_ID",
            "\"$awsAccessKey\""
        )
        buildConfigField(
            "String",
            "AWS_SECRET_ACCESS_KEY",
            "\"$awsSecretKey\""
        )
        buildConfigField(
            "String",
            "AWS_REGION",
            "\"$awsRegion\""
        )
        buildConfigField(
            "String",
            "AWS_S3_BUCKET",
            "\"$awsBucket\""
        )
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.material)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)

    implementation(platform("com.google.firebase:firebase-bom:34.19.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-database")

    // AWS SDK for Kotlin: Amazon S3
    implementation("aws.sdk.kotlin:s3:1.9.15")
}
