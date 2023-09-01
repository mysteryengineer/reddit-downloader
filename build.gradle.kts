import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.time.LocalDate
import java.time.format.DateTimeFormatter

plugins {
    alias(libs.plugins.kotlin)
    alias(libs.plugins.shadow)
    application
}

group = "io.vinicius"
version = "1.0"

repositories {
    mavenCentral()
}

dependencies {
    // OkHttp
    implementation(platform(libs.okhttp.bom))
    implementation(libs.okhttp)

    // General
    implementation(libs.coroutines.core)
    implementation(libs.mixpanel)
    implementation(libs.mordant)
    implementation(libs.moshi)
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "17"
}

application {
    mainClass.set("io.vinicius.rmd.MainKt")
}

tasks.withType<ShadowJar> {
    archiveFileName.set("rmd.${archiveExtension.get()}")
}

tasks.register<Exec>("docker") {
    val calver = LocalDate.now().format(DateTimeFormatter.ofPattern("uu.M.d"))
    workingDir(".")
    executable("docker")
    args("build", "-t", "vegidio/reddit-media-downloader", ".", "--build-arg", "VERSION=$calver")
}