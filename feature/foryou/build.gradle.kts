/*
 * Copyright 2022 The Android Open Source Project
 * Modification Copyright 2023 DeNA Co., Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
plugins {
    id("nowinandroid.android.feature")
    id("nowinandroid.android.library.compose")
    id("nowinandroid.android.library.jacoco")
    alias(libs.plugins.ksp)
    alias(libs.plugins.roborazzi)
}

android {
    namespace = "com.google.samples.apps.nowinandroid.feature.foryou"
    testOptions {
        // TODO: Convert it as a convention plugin once Flamingo goes out (https://github.com/android/nowinandroid/issues/523)
        managedDevices {
            devices {
                maybeCreate<com.android.build.api.dsl.ManagedVirtualDevice>("pixel4api30").apply {
                    device = "Pixel 4"
                    apiLevel = 30
                    // ATDs currently support only API level 30.
                    systemImageSource = "aosp-atd"
                }
            }
        }
        unitTests {
            all {
                it.systemProperty(
                    "roborazzi.output.dir",
                    rootProject.file("screenshots").absolutePath
                )
            }
            isIncludeAndroidResources = true
        }
    }
}

ksp {
    arg("skipPrivatePreviews", "true")
}

dependencies {
    implementation(libs.kotlinx.datetime)

    implementation(libs.kotlinx.coroutines.android)

    implementation(libs.accompanist.flowlayout)

    testImplementation(libs.bundles.mockk)

    implementation(libs.showkase)
    ksp(libs.showkase.processor)

    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
    testImplementation(libs.robolectric)

    testImplementation(libs.coil.kt.test)
}

roborazzi {
    outputDir.set(rootProject.file("screenshots"))
}