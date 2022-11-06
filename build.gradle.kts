import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.time.LocalDate
import java.time.format.DateTimeFormatter

plugins {
    kotlin("jvm") version "1.7.20"
    id("com.google.devtools.ksp") version("1.7.20-1.0.8")
    id("com.github.johnrengelman.shadow") version "7.1.2"
    application
}

group = "io.vinicius"
version = "1.0"

repositories {
    mavenCentral()
}

dependencies {
    // Others
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")

    // Moshi
    implementation("com.squareup.moshi:moshi-kotlin:1.14.0")
    implementation("com.squareup.moshi:moshi-adapters:1.14.0")
    ksp("com.squareup.moshi:moshi-kotlin-codegen:1.14.0")

    // OkHttp
    implementation(platform("com.squareup.okhttp3:okhttp-bom:4.10.0"))
    implementation("com.squareup.okhttp3:okhttp")
    implementation("com.squareup.okhttp3:logging-interceptor")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "17"
}

application {
    mainClass.set("io.vinicius.rmd.MainKt")
}

tasks.register<Exec>("docker") {
    val calver = LocalDate.now().format(DateTimeFormatter.ofPattern("uu.M.d"))
    workingDir(".")
    executable("docker")
    args("build", "-t", "vegidio/rmd", ".", "--build-arg", "VERSION=$calver")
}