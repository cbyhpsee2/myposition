// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("com.android.application") version "8.2.2" apply false
    id("org.jetbrains.kotlin.android") version "1.9.22" apply false
    id("com.google.gms.google-services") version "4.4.1" apply false
    id("org.jetbrains.compose") version "1.5.12" apply false
}
// Top-level build file where you can add configuration options common to all sub-projects/modules.

buildscript {
    repositories {
        google()
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
    dependencies {
        classpath("com.android.tools.build:gradle:8.2.2")
        classpath(kotlin("gradle-plugin:1.9.22"))
        classpath("org.jetbrains.compose:compose-gradle-plugin:1.5.12")
        // NOTE: Do not place your application dependencies here; they belong
        // in the individual module build.gradle files
    }
}
