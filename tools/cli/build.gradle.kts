plugins {
    alias(libs.plugins.kotlin.jvm)
    id("application")
}

application {
    mainClass.set("com.ashiyerrc.appbuilder.cli.MainKt")
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":tools:code-gen-plugin"))
    
    implementation("com.github.ajalt.clikt:clikt:4.2.0")
    implementation("com.squareup.moshi:moshi-kotlin:${libs.versions.moshi.get()}")
    implementation("com.squareup.okhttp3:okhttp:${libs.versions.okhttp.get()}")
    implementation(libs.timber)
    
    testImplementation(libs.junit)
    testImplementation(libs.kotest.runner)
    testImplementation(libs.kotest.assertions)
}

tasks.named<Jar>("jar") {
    manifest {
        attributes["Main-Class"] = "com.ashiyerrc.appbuilder.cli.MainKt"
    }
    from(sourceSets.main.get().output)
    dependsOn(configurations.runtimeClasspath)
    from({
        configurations.runtimeClasspath.get().filter { it.name.endsWith("jar") }.map { zipTree(it) }
    })
}
