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

tasks.register("generateVersionProperties") {
    val outputDir = layout.buildDirectory.dir("generated/resources")
    outputs.dir(outputDir)

    doFirst {
        val file = outputDir.get().file("version.properties").asFile
        file.parentFile.mkdirs()
        file.writeText("version=${project.version}")
    }
}

tasks.processResources {
    dependsOn("generateVersionProperties")
    from(layout.buildDirectory.dir("generated/resources"))
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = "com.github.shanebeee.et.Main"
    }
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

tasks.register<Exec>("jpackageMac") {
    dependsOn(tasks.jar)

    doFirst {
        mkdir(layout.buildDirectory.dir("jpackage"))

        val jarFile = tasks.jar.get().archiveFile.get().asFile

        commandLine(
            "jpackage",
            "--type", "dmg",
            "--input", jarFile.parent,
            "--main-jar", jarFile.name,
            "--main-class", "com.github.shanebeee.et.Main",
            "--name", "EmployeeTracker",
            "--app-version", project.version.toString(),
            "--dest", layout.buildDirectory.dir("jpackage").get().asFile.absolutePath,
            "--icon", "src/main/resources/images/1024.icns"
        )
    }
}
