plugins {
    id("cpt.android.application")
}

android {
    namespace = "by.vsdev.cpt"

    defaultConfig {
        applicationId = "by.vsdev.cpt"
        versionCode = 1
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }
}

dependencies {
    implementation(project(":app:shell"))
    implementation(project(":core:di"))
    implementation(project(":core:database"))
    implementation(project(":core:secrets"))

    implementation(libs.androidx.activity.compose)
    implementation(libs.koin.android)

    implementation(libs.compose.uiToolingPreview)
    debugImplementation(libs.compose.uiTooling)
}
