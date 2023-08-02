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
    // Others
    implementation(libs.mordant)
    implementation(libs.coroutines.core)
    implementation(libs.moshi)

    // OkHttp
    implementation(platform(libs.okhttp.bom))
    implementation(libs.okhttp)
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