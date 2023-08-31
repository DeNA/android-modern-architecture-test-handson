/*
 * Copyright 2023 DeNA Co., Ltd.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package com.google.samples.apps.nowinandroid.core.data.repository

import com.google.common.truth.Truth
import com.google.samples.apps.nowinandroid.core.data.testdoubles.TestNetworkDataSource
import com.google.samples.apps.nowinandroid.core.model.data.Topic
import com.google.samples.apps.nowinandroid.core.model.data.VideoNewsResource
import com.google.samples.apps.nowinandroid.core.network.model.NetworkTopic
import com.google.samples.apps.nowinandroid.core.network.model.NetworkVideoNewsResource
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit.MINUTES

@Suppress("NonAsciiCharacters", "RemoveRedundantBackticks")
class OnMemoryCacheVideoNewsRepositoryTest {

    // 練習1: 不必要なAPIリクエストが行われていないかをテストする
    @Test
    fun `getVideoNewsResources_不必要なAPIリクエストが行われていないかをテストする`() = runTest {

        var networkCallCount = 0

        val testNetworkDataSource = TestNetworkDataSource(getVideoNewsResourcesFunc = {
            networkCallCount++
            listOf(
                testNetworkVideoNewsResource,
            )
        })

        val videoNewsRepository = OnMemoryCacheVideoNewsRepository(testNetworkDataSource,
            currentTimeMillsProvider = {
                System.currentTimeMillis()
            })

        videoNewsRepository.getVideoNewsResources()
        val actual = videoNewsRepository.getVideoNewsResources()

        val expected = listOf(
            testVideoNewsResource,
        )
        Truth.assertThat(expected).isEqualTo(actual)

        Truth.assertThat(networkCallCount).isEqualTo(1)
    }

    // 練習2: キャッシュの有効期限切れのテストを実装する
    @Test
    fun `getVideoNewsResources_キャッシュの有効期限切れのテストを実装する`() = runTest {

        var currentTimeMills = 0L

        val testNetworkDataSource = TestNetworkDataSource(getVideoNewsResourcesFunc = {
            listOf(
                testNetworkVideoNewsResource,
            )
        })

        val videoNewsRepository = OnMemoryCacheVideoNewsRepository(testNetworkDataSource,
            currentTimeMillsProvider = {
                currentTimeMills
            })

        // 1回目のリクエスト
        val firstRequestResult = videoNewsRepository.getVideoNewsResources()
        Truth.assertThat(firstRequestResult.size).isEqualTo(1)

        testNetworkDataSource.getVideoNewsResourcesFunc = {
            listOf(
                testNetworkVideoNewsResource,
                testNetworkVideoNewsResource.copy(
                    id = "id2"
                )
            )
        }
        currentTimeMills = MINUTES.toMillis(10) + 1  // 10分 + 1ミリ秒

        // 2回目のリクエスト
        val actual = videoNewsRepository.getVideoNewsResources()

        Truth.assertThat(actual.size).isEqualTo(2)
    }

    private val testNetworkVideoNewsResource = NetworkVideoNewsResource(
        id = "id",
        title = "title",
        content = "content",
        headerImageUrl = "https://host/header.png",
        previewMovieUrl = "https://host/full_movie.mp4",
        fullMovieUrl = "https://host/full_movie.mp4",
        publishDate = "2022-12-31T10:15:30+09:00",
        topics = listOf(NetworkTopic(id = "id"))
    )

    private val testVideoNewsResource = VideoNewsResource(
        id = "id",
        title = "title",
        content = "content",
        headerImageUrl = "https://host/header.png",
        previewMovieUrl = "https://host/full_movie.mp4",
        fullMovieUrl = "https://host/full_movie.mp4",
        publishDate = ZonedDateTime.of(2022, 12, 31, 10, 15, 30, 0, ZoneId.of("+09:00")),
        topics = listOf(Topic(id = "id"))
    )
}