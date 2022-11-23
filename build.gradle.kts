import magik.github
import org.gradle.api.attributes.java.TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE
import org.gradle.internal.os.OperatingSystem.*
import java.net.URL

plugins {
    java
    kotlin("jvm") version embeddedKotlinVersion
    id("elect86.magik") version "0.3.1"
    `maven-publish`
    //    id "org.jetbrains.kotlin.kapt" version "1.3.10"
    id("org.jetbrains.dokka") version "1.4.10"
    id("com.github.johnrengelman.shadow") version "6.1.0"
}

// jitpack
group = "com.github.kotlin_graphics"
val moduleName = "$group.assimp"

val kotestVersion = "4.2.5"
val kx = "com.github.kotlin-graphics"
val unsignedVersion = "f2cd9c97"
val koolVersion = "b4ff3661"
val glmVersion = "3466fcde"
val gliVersion = "9c67885f"
val unoVersion = "68d50c59"
val lwjglVersion = "3.2.3"
val lwjglNatives = "natives-" + when (current()) {
    WINDOWS -> "windows"
    LINUX -> "linux"
    else -> "macos"
}

repositories {
    mavenCentral()
    jcenter()
    maven("https://jitpack.io")
    github("kotlin-graphics/mary")
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation(kotlin("reflect"))

//    implementation("$kx:kotlin-unsigned:$unsignedVersion")
//    implementation("$kx:kool:$koolVersion")
//    implementation("$kx:glm:$glmVersion")
//    implementation("$kx:gli:$gliVersion")
//    implementation("$kx:uno-sdk:$unoVersion")
    implementation("kotlin.graphics:unsigned:3.3.31")
//    implementation("kotlin.graphics:glm:0.9.9.1-5")
    implementation("com.github.qiao-coder:glm:0.9.9.1-6")
    implementation("kotlin.graphics:gli:0.8.3.0-18")
    implementation("kotlin.graphics:kool:0.9.71")
//    implementation("kotlin.graphics:uno:0.7.17")
    implementation("com.github.qiao-coder:uno-sdk:0.7.18")

    implementation(platform("org.lwjgl:lwjgl-bom:$lwjglVersion"))
    listOf("", "-glfw", "-jemalloc", "-openal", "-opengl", "-stb").forEach {
        implementation("org.lwjgl", "lwjgl$it")
        runtimeOnly("org.lwjgl", "lwjgl$it", classifier = lwjglNatives)
    }

//    implementation("io.github.microutils:kotlin-logging:1.7.8")
    implementation("io.github.microutils:kotlin-logging-jvm:2.0.3")
    implementation("org.slf4j:slf4j-api:1.7.29")

    testImplementation("org.slf4j:slf4j-simple:1.7.29")

    testImplementation("io.kotest:kotest-runner-junit5-jvm:$kotestVersion")
    testImplementation("io.kotest:kotest-assertions-core-jvm:$kotestVersion")
}

//java { modularity.inferModulePath.set(true) }

tasks {
    dokkaHtml {
        dokkaSourceSets.configureEach {
            sourceLink {
                localDirectory.set(file("src/main/kotlin"))
                remoteUrl.set(URL("https://github.com/kotlin-graphics/assimp/tree/master/src/main/kotlin"))
                remoteLineSuffix.set("#L")
            }
        }
    }

//    withType<KotlinCompile>().all {
//        kotlinOptions {
//            jvmTarget = "11"
//            freeCompilerArgs += listOf("-Xinline-classes", "-Xopt-in=kotlin.RequiresOptIn")
//        }
//        sourceCompatibility = "11"
//    }
    withType<org.jetbrains.kotlin.gradle.dsl.KotlinCompile<*>>().all {
        kotlinOptions {
            freeCompilerArgs += listOf("-opt-in=kotlin.RequiresOptIn")
        }
    }

//    compileJava { // this is needed because we have a separate compile step in this example with the 'module-info.java' is in 'main/java' and the Kotlin code is in 'main/kotlin'
//        options.compilerArgs = listOf("--patch-module", "$moduleName=${sourceSets.main.get().output.asPath}")
//    }

    withType<Test> {
        useJUnitPlatform()
        maxHeapSize = "1g" // set heap size for the test JVM(s)
    }
}

val dokkaJavadocJar by tasks.register<Jar>("dokkaJavadocJar") {
    dependsOn(tasks.dokkaJavadoc)
    from(tasks.dokkaJavadoc.get().outputDirectory.get())
    archiveClassifier.set("javadoc")
}

val dokkaHtmlJar by tasks.register<Jar>("dokkaHtmlJar") {
    dependsOn(tasks.dokkaHtml)
    from(tasks.dokkaHtml.get().outputDirectory.get())
    archiveClassifier.set("html-doc")
}

val sourceJar = task("sourceJar", Jar::class) {
    dependsOn(tasks.classes)
    archiveClassifier.set("sources")
    from(sourceSets.main.get().allSource)
}

artifacts {
    archives(dokkaJavadocJar)
    archives(dokkaHtmlJar)
    archives(sourceJar)
}

publishing {
    publications.create<MavenPublication>("mavenJava") {
        from(components["java"])
        artifact(sourceJar)
    }
    repositories.maven {
        name = "GitHubPackages"
        url = uri("https://maven.pkg.github.com/kotlin-graphics/assimp")
        credentials {
            username = System.getenv("GITHUB_ACTOR")
            password = System.getenv("GITHUB_TOKEN")
        }
    }
}

// == Add access to the 'modular' variant of kotlin("stdlib"): Put this into a buildSrc plugin and reuse it in all your subprojects
configurations.all { attributes.attribute(TARGET_JVM_VERSION_ATTRIBUTE, 8) }