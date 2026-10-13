plugins {
    id("com.android.application")
}

android {
    namespace = "ch.cclerc.muguet.android"
    compileSdk = 36

    defaultConfig {
        applicationId = "ch.cclerc.muguet.android"
        minSdk = 28
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
        ndk {
            abiFilters += listOf("arm64-v8a")
        }
    }

    sourceSets["main"].jniLibs.srcDirs("src/main/jniLibs")
}

val buildRustAndroid by tasks.registering(Exec::class) {
    group = "build"
    description = "Compile the Muguet Rust native library for Android arm64."
    workingDir = rootProject.projectDir
    commandLine("bash", "scripts/build-android.sh")
    inputs.files(fileTree("src") { include("**/*.rs") }, file("Cargo.toml"), file("Cargo.lock"))
    outputs.file("app/src/main/jniLibs/arm64-v8a/libmuguet.so")
}

tasks.named("preBuild").configure {
    dependsOn(buildRustAndroid)
}
