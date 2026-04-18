plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("com.google.dagger.hilt.android")
    id("kotlin-kapt")
    id("org.jetbrains.kotlin.plugin.serialization")
}
dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:protocol"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    kapt("com.google.dagger:hilt-android-compiler:2.50")
}
