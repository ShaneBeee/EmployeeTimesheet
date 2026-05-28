plugins {
    id("java")
    id("application")
}

group = "com.github.shanebeee"
version = "1.0.0"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

application {
    mainClass = "com.github.shanebeee.et.Main"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.google.code.gson:gson:2.11.0")
    implementation("com.formdev:flatlaf:3.5.4")
    implementation("com.formdev:flatlaf-extras:3.5.4")
    implementation("com.itextpdf:kernel:8.0.5")
    implementation("com.itextpdf:layout:8.0.5")
    implementation("com.itextpdf:io:8.0.5")
    implementation("org.slf4j:slf4j-simple:2.0.13")
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = "com.github.shanebeee.et.Main"
    }
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}
