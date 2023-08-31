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

package com.google.samples.apps.nowinandroid.core.data.repository

import androidx.datastore.core.DataStoreFactory
import com.google.common.truth.Truth
import com.google.samples.apps.nowinandroid.core.datastore.NiaPreferencesDataSource
import com.google.samples.apps.nowinandroid.core.datastore.UserPreferencesSerializer
import com.google.samples.apps.nowinandroid.core.model.data.DarkThemeConfig
import com.google.samples.apps.nowinandroid.core.model.data.ThemeBrand
import com.google.samples.apps.nowinandroid.core.model.data.UserData
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@Suppress("NonAsciiCharacters", "RemoveRedundantBackticks")
class OfflineFirstUserDataRepositoryTest {

    private lateinit var repository: OfflineFirstUserDataRepository

    private lateinit var niaPreferencesDataSource: NiaPreferencesDataSource

    // 練習: DataStoreのテストを実装する
    @get:Rule
    val tmpFolder: TemporaryFolder = TemporaryFolder.builder().assureDeletion().build()

    @Before
    fun setup() {
        val dataStore = DataStoreFactory.create(serializer = UserPreferencesSerializer()) {
            tmpFolder.newFile("user_preferences_test.pb")
        }

        niaPreferencesDataSource = NiaPreferencesDataSource(dataStore)

        repository = OfflineFirstUserDataRepository(
            niaPreferencesDataSource = niaPreferencesDataSource
        )
    }

    @Test
    fun `ユーザーデータの初期値を取得する`() = runTest {

        Truth.assertThat(repository.userData.first())
            .isEqualTo(
                UserData(
                    bookmarkedNewsResources = emptySet(),
                    followedTopics = emptySet(),
                    themeBrand = ThemeBrand.DEFAULT,
                    darkThemeConfig = DarkThemeConfig.FOLLOW_SYSTEM,
                    shouldHideOnboarding = false
                )
            )
    }

    @Test
    fun `フォローしていたトピックをアンフォローする`() = runTest {

        repository.setFollowedTopicIds(followedTopicIds = setOf("1", "2"))

        Truth.assertThat(repository.userData.first().followedTopics)
            .isEqualTo(setOf("1", "2"))

        repository.toggleFollowedTopicId(followedTopicId = "1", followed = false)

        Truth.assertThat(repository.userData.first().followedTopics)
            .isEqualTo(setOf("2"))
    }

    @Test
    fun `フォローしているトピックを追加する`() = runTest {

        repository.setFollowedTopicIds(followedTopicIds = setOf("1", "2"))

        Truth.assertThat(repository.userData.first().followedTopics)
            .isEqualTo(setOf("1", "2"))

        repository.toggleFollowedTopicId(followedTopicId = "3", followed = true)

        Truth.assertThat(repository.userData.first().followedTopics)
            .isEqualTo(setOf("1", "2", "3"))
    }

    // 以下は実データソースの代わりに、モックを使ってリポジトリのテストを実装した例
    // 結合範囲によって、検証できる範囲が変わることの確認用

    @Test
    fun `フォローしていたトピックをアンフォローする_モック利用時の例`() = runTest {

        val mockDataSource = mockk<NiaPreferencesDataSource>(relaxed = true)
        val mockUsedRepository = OfflineFirstUserDataRepository(
            niaPreferencesDataSource = mockDataSource
        )
        mockUsedRepository.toggleFollowedTopicId(followedTopicId = "1", followed = false)

        coVerify {
            mockDataSource.toggleFollowedTopicId("1", false)
        }
    }
}

