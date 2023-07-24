plugins {
    kotlin("multiplatform") version "1.9.0"
    kotlin("plugin.serialization") version "1.9.0"
}

group = "io.vinicius"
version = "1.0"

repositories {
    mavenCentral()
}

dependencies {
    commonMainImplementation("com.soywiz.korlibs.klock:klock:4.0.2")
    commonMainImplementation("com.squareup.okio:okio:3.4.0")
    commonMainImplementation("io.ktor:ktor-client-content-negotiation:2.3.2")
    commonMainImplementation("io.ktor:ktor-client-core:2.3.2")
    commonMainImplementation("io.ktor:ktor-client-curl:2.3.2")
    commonMainImplementation("io.ktor:ktor-serialization-kotlinx-json:2.3.2")
}

kotlin {
    val hostOs = System.getProperty("os.name")
    val arch = System.getProperty("os.arch")
    val isMingwX64 = hostOs.startsWith("Windows")
    val nativeTarget = when {
        hostOs == "Mac OS X" -> {
            if (arch.contains("aarch64")) {
                macosArm64("native")
            } else {
                macosX64("native")
            }
        }
        hostOs == "Linux" -> linuxX64("native")
        isMingwX64 -> mingwX64("native")
        else -> throw GradleException("Host OS is not supported in Kotlin/Native.")
    }

    nativeTarget.apply {
        binaries {
            executable {
                entryPoint = "io.vinicius.rmd.main"
            }
        }
    }
    sourceSets {
        val nativeMain by getting
        val nativeTest by getting
    }
}