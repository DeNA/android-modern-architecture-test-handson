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
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.time.ZoneId
import java.time.ZonedDateTime

@Suppress("NonAsciiCharacters", "RemoveRedundantBackticks")
class DefaultVideoNewsRepositoryTest {

    // suspend関数のテストを書くのサンプル
    @Test
    fun getVideoNewsResources() = runTest {

        // NetworkDataSourceがテストデータを返すように設定する
        val testNetworkDataSource = TestNetworkDataSource(
            getVideoNewsResourcesFunc = {
                listOf(
                    testNetworkVideoNewsResource,
                    testNetworkVideoNewsResource.copy(
                        id = "id2",
                        publishDate = "2022-12-31T10:15:30-05:00",
                        topics = listOf(NetworkTopic(id = "id2"))
                    )
                )
            })
        val videoNewsRepository = DefaultVideoNewsRepository(testNetworkDataSource)

        // テストしたいsuspend関数の呼び出し
        val actual = videoNewsRepository.getVideoNewsResources()

        val expected = listOf(
            testVideoNewsResource,
            testVideoNewsResource.copy(
                id = "id2",
                publishDate = ZonedDateTime.of(2022, 12, 31, 10, 15, 30, 0, ZoneId.of("-05:00")),
                topics = listOf(Topic(id = "id2"))
            )
        )
        Truth.assertThat(expected).isEqualTo(actual)

    }

    // 練習1: APIからエラーが返ってきた時のテストを書く
    @Test
    fun `getVideoNewsResources_APIからエラーが返ってきた時のテストを書く`() = runTest {

        // NetworkDataSourceがテストデータを返すように設定する
        val testNetworkDataSource = TestNetworkDataSource(
            getVideoNewsResourcesFunc = {
                throw HttpException(
                    Response.error<Any>(
                        400,
                        "{ error: \"エラーレスポンスのメッセージ\" }".toResponseBody()
                    )
                )
            })
        val videoNewsRepository = DefaultVideoNewsRepository(testNetworkDataSource)

        // ====練習問題 ここから ====

        // TODO: テストコードを実装する.
        // getVideoNewsResourcesを呼び出してApiExceptionのメッセージ(ApiException#message)をassertする

        val expectedMessage = "エラーレスポンスのメッセージ" // メッセージの期待値

        // ====練習問題 ここまで ====
    }

    // 練習2: Flowが公開されている実装のテストを書く
    @Test
    fun `getVideoNewsResourcesStream_Flowが公開されている実装のテストを書く`() = runTest {
        val testNetworkDataSource = TestNetworkDataSource(getVideoNewsResourcesFunc = {
            listOf(
                testNetworkVideoNewsResource
            )
        })

        val videoNewsRepository = DefaultVideoNewsRepository(testNetworkDataSource)

        videoNewsRepository.getVideoNewsResources()

        // ====練習問題 ここから ====

        // TODO テストコードを実装する.
        // videoNewsStreamにemitされたデータを期待値と比較する

        val expected = listOf(testVideoNewsResource) // 期待値

        // ====練習問題 ここまで ====
    }

    // 練習3: Flowを継続的にcollectするテストを書く
    @Test
    fun `getVideoNewsResourcesStream_Flowを継続的にcollectするテストを書く`() = runTest {

        // 1回目
        val testNetworkDataSource = TestNetworkDataSource(getVideoNewsResourcesFunc = {
            listOf(
                testNetworkVideoNewsResource
            )
        })
        val videoNewsRepository = DefaultVideoNewsRepository(testNetworkDataSource)

        // ====練習問題 ここから ====

        // TODO テストコードを実装する.
        //  videoNewsStreamをcollectする
        // collectした結果はactualにaddする
        val actual = mutableListOf<List<VideoNewsResource>>()


        // ====練習問題 ここまで ====

        videoNewsRepository.getVideoNewsResources()

        // 2回目
        testNetworkDataSource.getVideoNewsResourcesFunc = {
            listOf(
                testNetworkVideoNewsResource.copy(
                    id = "id2"
                )
            )
        }
        videoNewsRepository.getVideoNewsResources()

        val expected = listOf(
            listOf(testVideoNewsResource),
            listOf(testVideoNewsResource.copy(id = "id2")),
        )

        Truth.assertThat(actual).isEqualTo(expected)
    }

    // 練習4: delayを入れたテストを書く
    @Test
    fun `downloadVideo_delayを入れたテストを書く`() = runTest {

        var downloadedCount = 0

        // ====練習問題 ここから ====

        val testNetworkDataSource = TestNetworkDataSource(downloadVideoResourceFunc = {
            // TODO テストコードを実装する
            // delayとdownloadedCountのincrementをいれる
            "test".toResponseBody()
        })
        val videoNewsRepository = DefaultVideoNewsRepository(testNetworkDataSource)

        // TODO テストコードを実装する
        // downloadedVideoを同じURLで2回よびだす

        // TODO テストコードを実装する
        // delayで中断していたコルーチンを再開する


        // ====練習問題 ここまで ====

        Truth.assertThat(downloadedCount).isEqualTo(1)
    }

    // 練習5: Dispatcherが指定されたテストを書く
    @Test
    fun `downloadVideo_Dispatcherが指定されたテストを書く`() = runTest {

        var downloadCount = 0

        // ====練習問題 ここから ====

        val testNetworkDataSource = TestNetworkDataSource(downloadVideoResourceFunc = {
            // TODO テストコードを実装する.
            //  delayとdownloadedCountのincrementをいれる

            "test".toResponseBody()
        })

        // TODO
        // TestDispatcherを外から渡せるようにDefaultVideoNewsRepositoryを修正する
        val videoNewsRepository = DefaultVideoNewsRepository(
            testNetworkDataSource
        )

        // TODO テストコードを実装する
        // downloadedVideo2を同じURLで2回よびだす

        // TODO テストコードを実装する
        //  コルーチンの実行とdelay分の時間を進める

        // ====練習問題 ここまで ====

        Truth.assertThat(downloadCount).isEqualTo(1)
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