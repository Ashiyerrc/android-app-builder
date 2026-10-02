plugins {
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    implementation(libs.timber)
    
    testImplementation(libs.junit)
    testImplementation(libs.kotest.runner)
    testImplementation(libs.kotest.assertions)
}
