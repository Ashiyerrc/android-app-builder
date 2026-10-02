plugins {
    alias(libs.plugins.kotlin.jvm)
    id("application")
}

application {
    mainClass.set("com.ashiyerrc.appbuilder.ai.ApiServerKt")
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":feature:code-generator"))
    
    // Ktor server
    implementation("io.ktor:ktor-server-core:2.3.0")
    implementation("io.ktor:ktor-server-netty:2.3.0")
    implementation("io.ktor:ktor-server-cors:2.3.0")
    implementation("io.ktor:ktor-server-content-negotiation:2.3.0")
    
    // Serialization
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.0")
    implementation("com.google.code.gson:gson:2.10.1")
    
    // AI APIs - Free tier options
    implementation("com.google.ai.client.generativeai:google-generativeai:0.1.1") // Gemini
    implementation("com.anthropic:anthropic-sdk:0.1.0") // Claude alternative
    
    // HTTP client
    implementation("io.ktor:ktor-client-core:2.3.0")
    implementation("io.ktor:ktor-client-okhttp:2.3.0")
    
    // JSON
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
    
    // Logging
    implementation("io.github.microutils:kotlin-logging:3.0.5")
    implementation("ch.qos.logback:logback-classic:1.4.11")
    
    // Dependency injection
    implementation("org.koin:koin-core:3.4.0")
    implementation("org.koin:koin-ktor:3.4.0")
    
    testImplementation(libs.junit)
    testImplementation("io.ktor:ktor-server-test-host:2.3.0")
}
