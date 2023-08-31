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
    id("nowinandroid.android.library")
    id("nowinandroid.android.library.jacoco")
    id("nowinandroid.android.hilt")
    id("kotlinx-serialization")
}

android {
    namespace = "com.google.samples.apps.nowinandroid.core.data"
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            // BitriseでRobolectricによるテストを実行すると
            // org.robolectric:android-all-instrumented:4.1.2_r1-robolectric-r1-i4
            // のダウンロードに頻繁に失敗するので、対象とするSDKを1つに絞る
            all {
               it.systemProperty("robolectric.enabledSdks", defaultConfig.targetSdk ?: 33)
            }
        }
    }
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:model"))
    implementation(project(":core:database"))
    implementation(project(":core:datastore"))
    implementation(project(":core:network"))

    testImplementation(project(":core:testing"))
    testImplementation(project(":core:datastore-test"))
    testImplementation(libs.robolectric)
    testImplementation(libs.room.ktx)
    testImplementation(libs.bundles.mockk)

    implementation(libs.androidx.core.ktx)

    implementation(libs.kotlinx.datetime)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.retrofit.core)
}