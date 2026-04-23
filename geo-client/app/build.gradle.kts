plugins {
    alias(libs.plugins.kotlin.jvm)
    id("org.openjfx.javafxplugin") version "0.0.14"
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
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation(libs.guava)

    implementation("org.apache.poi:poi:5.2.5")
    implementation("org.apache.poi:poi-ooxml:5.2.5")
    implementation("org.apache.pdfbox:pdfbox:2.0.30")
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
    // Define the main class for the application.
    mainClass = "org.example.geoapp.MainApp"
}

tasks.named<Test>("test") {
    // Use JUnit Platform for unit tests.
    useJUnitPlatform()
}
