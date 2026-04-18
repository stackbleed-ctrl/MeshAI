plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("com.google.dagger.hilt.android")
    id("kotlin-kapt")
}
dependencies {
    implementation(project(":core:model"))
    kapt("com.google.dagger:hilt-android-compiler:2.50")
}
