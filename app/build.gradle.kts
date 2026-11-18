import com.github.gradle.node.npm.task.NpmTask

plugins {
    application
    alias(libs.plugins.node)
    alias(libs.plugins.shadow)
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.jetty.ee11.servlet)
    implementation(libs.freemarker)
    implementation(libs.hikaricp)
    implementation(libs.slf4j.api)
    implementation(libs.flyway.core)
    implementation(libs.flyway.database.postgresql)
    implementation(libs.postgresql)
    implementation(libs.jackson.databind)
    
    runtimeOnly(libs.logback.classic)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

application {
    mainClass = "org.example.App"
}

node {
    download = true
    version = "22.12.0"
    nodeProjectDir = file("src/main/frontend")
}

val buildFrontend = tasks.register<NpmTask>("buildFrontend") {
    dependsOn(tasks.npmInstall)
    args = listOf("run", "build")

    inputs.dir("src/main/frontend/src")
    inputs.file("src/main/frontend/package.json")
    inputs.file("src/main/frontend/vite.config.js")
    outputs.dir(layout.buildDirectory.dir("frontend"))
}

tasks.processResources {
    from(buildFrontend) {
        into("static/assets")
    }
}

tasks.shadowJar {
    mergeServiceFiles()
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}
