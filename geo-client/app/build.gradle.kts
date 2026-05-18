plugins {
    alias(libs.plugins.kotlin.jvm)
    id("org.openjfx.javafxplugin") version "0.1.0"
    id("org.beryx.jlink") version "4.0.0"
    application
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    implementation("org.openjfx:javafx-controls:21")
    implementation("org.openjfx:javafx-fxml:21")
    implementation("org.controlsfx:controlsfx:11.2.2")
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-javafx:1.8.1")
    implementation(kotlin("stdlib"))
    implementation(libs.guava)

    implementation("org.apache.poi:poi:5.4.0")
    implementation("org.apache.poi:poi-ooxml:5.4.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

javafx {
    version = "21"
    modules = listOf("javafx.controls", "javafx.fxml")
}

application {
    mainClass.set("org.example.geoapp.MainApp")
}

jlink {
    imageZip.set(project.file("${layout.buildDirectory.get()}/distributions/app.zip"))

    options.set(listOf(
        "--strip-debug",
        "--compress", "2",
        "--no-header-files",
        "--no-man-pages"
    ))

    addExtraDependencies("kotlinx-coroutines-core", "kotlinx-coroutines-javafx")

    launcher {
        name = "GeoApp"
        moduleName = "org.example.geoapp"
        mainClass = "org.example.geoapp.MainApp"
    }

    jpackage {
        installerType = "exe"

        installerOptions.addAll(listOf(
            "--app-version", "1.0.0",
            "--win-menu",
            "--win-shortcut",
            "--win-dir-chooser"
        ))
    }
}


tasks.named<Test>("test") {
    useJUnitPlatform()
}