plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.ashiyerrc.appbuilder.tools.codegen"
    compileSdk = 34

    defaultConfig {
        minSdk = 24
        targetSdk = 34
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
    implementation(project(":core:common"))
    
    implementation(libs.poet)
    implementation(libs.javapoet)
    implementation(libs.moshi)
    kapt(libs.moshi.codegen)
    
    testImplementation(libs.junit)
    testImplementation(libs.kotest.runner)
}
