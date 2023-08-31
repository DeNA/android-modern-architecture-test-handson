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

import android.database.sqlite.SQLiteException
import com.google.common.truth.Truth
import com.google.samples.apps.nowinandroid.core.data.testdoubles.TestNetworkDataSource
import com.google.samples.apps.nowinandroid.core.database.dao.NewsResourceDao
import com.google.samples.apps.nowinandroid.core.database.dao.TopicDao
import com.google.samples.apps.nowinandroid.core.database.model.NewsResourceEntity
import com.google.samples.apps.nowinandroid.core.database.model.NewsResourceTopicCrossRef
import com.google.samples.apps.nowinandroid.core.database.model.PopulatedNewsResource
import com.google.samples.apps.nowinandroid.core.database.model.TopicEntity
import com.google.samples.apps.nowinandroid.core.model.data.NewsResource
import com.google.samples.apps.nowinandroid.core.model.data.NewsResourceType.Article
import com.google.samples.apps.nowinandroid.core.model.data.NewsResourceType.Event
import com.google.samples.apps.nowinandroid.core.model.data.NewsResourceType.Video
import com.google.samples.apps.nowinandroid.core.model.data.Topic
import com.google.samples.apps.nowinandroid.core.network.model.NetworkNewsResource
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import org.junit.Before
import org.junit.Test
import kotlin.test.assertFailsWith

@Suppress("NonAsciiCharacters", "RemoveRedundantBackticks")
class OfflineFirstNewsRepositoryMockDaoTest {

    private lateinit var repository: OfflineFirstNewsRepository

    private lateinit var newsResourceDao: NewsResourceDao

    private lateinit var topicDao: TopicDao

    private lateinit var network: TestNetworkDataSource

    @Before
    fun setup() {
        newsResourceDao = mockk(relaxed = true)
        topicDao = mockk(relaxed = true)
        network = TestNetworkDataSource()

        repository = OfflineFirstNewsRepository(
            newsResourceDao = newsResourceDao,
            topicDao = topicDao,
            network = network,
        )
    }

    @Test
    fun `getNewsResources_DAOのgetNewsResources由来のものが取得できること`() = runTest {
        // 実装の方針:
        // 1. newsResourceDao.getNewsResources() が呼び出されたときにテストデータが返ってくるようにセットアップする
        //    - NEWS_RESOURCE_1, NEWS_RESOURCE_2, NEWS_RESOURCE_5 をPopulatedNewsResourceにしたものを返す
        // 2. repositoryのgetNewsResources()を呼ぶ
        // 3. その結果が NEWS_RESOURCE_1, NEWS_RESOURCE_2, NEWS_RESOURCE_5 を NewsResource型に変換したものになっていることを確認する

        // MockKを使って newsResourceDao.getNewsResources() の戻り値を設定する
        every {
            newsResourceDao.getNewsResources()
        } returns MutableStateFlow(
            listOf(
                PopulatedNewsResource(entity = NEWS_RESOURCE_1, topics = emptyList()),
                PopulatedNewsResource(entity = NEWS_RESOURCE_2, topics = emptyList()),
                PopulatedNewsResource(entity = NEWS_RESOURCE_5, topics = emptyList()),
            )
        )

        // repositoryのgetNewsResources()を呼ぶ
        val actual: List<NewsResource> = repository.getNewsResources().first()

        // その結果が NEWS_RESOURCE_1, NEWS_RESOURCE_2, NEWS_RESOURCE_5 を NewsResource型に変換したものになっていることを確認する
        val expected = listOf(
            NewsResource(
                id = "1",
                title = "news for Topic 2",
                content = "Hilt",
                url = "url",
                headerImageUrl = "headerImageUrl",
                type = Video,
                publishDate = Instant.fromEpochMilliseconds(1),
                topics = listOf()
            ),
            NewsResource(
                id = "2",
                title = "news for Topic 1",
                content = "Espresso",
                url = "url",
                headerImageUrl = "headerImageUrl",
                type = Video,
                publishDate = Instant.fromEpochMilliseconds(2),
                topics = listOf()
            ),
            NewsResource(
                id = "5",
                title = "news w/o Topic",
                content = "Truth",
                url = "url",
                headerImageUrl = "headerImageUrl",
                type = Article,
                publishDate = Instant.fromEpochMilliseconds(3),
                topics = listOf()
            ),
        )

        Truth.assertThat(actual).containsExactlyElementsIn(expected)
    }

    @Test
    fun `pullNewsResources_ネットワークから取得したものがtopicDaoのinsertOrIgnoreTopicsに渡されていること`() = runTest {
        // 実装の方針:
        // 1. pullNewsResources()の中で呼ばれる network.getNewsResources() が
        //    テストデータ(`List<NetworkNewsResource>`)を返すように設定する
        // 2. repositoryのpullNewsResourcesを呼び出す
        // 3. topicDao.insertOrIgnoreTopics()が呼び出されていることを確認する。
        //    そのときの引数が、テストデータに含まれるTopicに対応していることを確認する。
        //    ※テストデータである`NetworkNewsResource`にはtopicのIDしか保持していない。
        //      その場合、DAOに格納されるTopicEntityは、ID以外の値は空文字列になる。
        network.getNewsResourcesFunc = {
            listOf(
                // 関連トピック: 1
                NETWORK_NEWS_RESOURCE_1,
                // 関連トピック: 1, 3, 19
                NETWORK_NEWS_RESOURCE_2
            )
        }

        repository.pullNewsResources()

        val slot = slot<List<TopicEntity>>()
        coVerify {
            topicDao.insertOrIgnoreTopics(capture(slot))
        }

        val actual = slot.captured
        val expected = listOf(
            createEmptyTopicEntity("1"), createEmptyTopicEntity("3"), createEmptyTopicEntity("19")
        )

        Truth.assertThat(actual).containsExactlyElementsIn(expected)
    }

    @Test
    fun `pullNewsResources_ネットワークから取得したものがnewsResourceDaoのinsertOrIgnoreTopicCrossRefEntitiesに渡されていること`() =
        runTest {
            // pullNewsResources()の中で呼ばれる network.getNewsResources() の戻り値を設定する
            network.getNewsResourcesFunc = {
                listOf(
                    // 関連トピック: 1
                    NETWORK_NEWS_RESOURCE_1,
                    // 関連トピック: 1, 3, 19
                    NETWORK_NEWS_RESOURCE_2
                )
            }

            repository.pullNewsResources()

            val actual = slot<List<NewsResourceTopicCrossRef>>()
            coVerify {
                newsResourceDao.insertOrIgnoreTopicCrossRefEntities(capture(actual))
            }

            val expected = listOf(
                NewsResourceTopicCrossRef(newsResourceId = "1", topicId = "1"),
                NewsResourceTopicCrossRef(newsResourceId = "2", topicId = "1"),
                NewsResourceTopicCrossRef(newsResourceId = "2", topicId = "3"),
                NewsResourceTopicCrossRef(newsResourceId = "2", topicId = "19"),
            )

            Truth.assertThat(actual.captured).containsExactlyElementsIn(expected)
        }

    @Test
    fun `pullNewsResources_topicDaoのinsertOrIgnoreTopicsで例外が発生したらそのまま上位にスローされること`() = runTest {
        // 実装の方針:
        // 1. pullNewsResources()の中で呼ばれる network.getNewsResources() が
        //    テストデータ(`List<NetworkNewsResource>`)を返すように設定する
        // 2. topicDao.insertOrIgnoreTopics()が常に例外SQLiteExceptionを発生するように
        //    MockKを使ってセットアップする
        // 3. repositoryのpullNewsResourcesを呼び出す
        // 4. 例外が発生することを確認する
        network.getNewsResourcesFunc = {
            listOf(
                // 関連トピック: 1
                NETWORK_NEWS_RESOURCE_1,
                // 関連トピック: 1, 3, 19
                NETWORK_NEWS_RESOURCE_2
            )
        }

        coEvery {
            topicDao.insertOrIgnoreTopics(any())
        } throws SQLiteException("Unknown Error")

        assertFailsWith<SQLiteException> {
            repository.pullNewsResources()
        }
    }

    @Test
    fun `getNewsResources(filterTopicIds)_DAOのgetNewsResources(filterTopicIds)由来のものが取得できること`() =
        runTest {
            every {
                newsResourceDao.getNewsResources(setOf(TOPIC_1.id))
            } returns MutableStateFlow(
                listOf(
                    PopulatedNewsResource(entity = NEWS_RESOURCE_2, topics = listOf(TOPIC_1))
                )
            )

            val actual = repository.getNewsResources(filterTopicIds = setOf(TOPIC_1.id)).first()

            // TOPIC_1に関連しているNewsResourceEntityはNEWS_RESOURCE_2なので、
            // NEWS_RESOURCE_2をNewsResourceに詰め替えたものと一致することを確認する
            val expected = NewsResource(
                id = "2",
                title = "news for Topic 1",
                content = "Espresso",
                url = "url",
                headerImageUrl = "headerImageUrl",
                type = Video,
                publishDate = Instant.fromEpochMilliseconds(2),
                topics = listOf(
                    // TOPIC_1をTopicに詰め替えたもの
                    Topic(
                        id = "1",
                        name = "Topic 1",
                        shortDescription = "short description 1",
                        longDescription = "long description 1",
                        url = "URL 1",
                        imageUrl = "image URL 1",
                    )
                )
            )

            // ====練習問題 ここから ====
            Truth.assertThat(actual).containsExactly(expected)
            // ====練習問題 ここまで ====
        }

    @Test
    fun `pullNewsResources_ネットワークから取得したものがnewsResourceDaoのupsertNewsResourcesに渡されていること`() =
        runTest {
            // pullNewsResources()の中で呼ばれる network.getNewsResources() の戻り値を設定する
            network.getNewsResourcesFunc = {
                listOf(
                    // 関連トピック: 1
                    NETWORK_NEWS_RESOURCE_1,
                    // 関連トピック: 1, 3, 19
                    NETWORK_NEWS_RESOURCE_2
                )
            }

            repository.pullNewsResources()

            val expected = listOf(
                NewsResourceEntity(
                    id = "1",
                    title = "Title from network 1",
                    content = "Content from network 1",
                    url = "network1",
                    headerImageUrl = "headerImage/network1",
                    publishDate = Instant.parse("2022-10-04T23:00:00.000Z"),
                    type = Event,
                ),
                NewsResourceEntity(
                    id = "2",
                    title = "Title from network 2",
                    content = "Content from network 2",
                    url = "network2",
                    headerImageUrl = "headerImage/network2",
                    publishDate = Instant.parse("2022-10-06T23:00:00.000Z"),
                    type = Article,
                )
            )

            // ====練習問題 ここから ====
            coVerify {
                newsResourceDao.upsertNewsResources(expected)
            }
            // ====練習問題 ここまで ====
        }

    companion object {
        private val NETWORK_NEWS_RESOURCE_1 = NetworkNewsResource(
            id = "1",
            title = "Title from network 1",
            content = "Content from network 1",
            url = "network1",
            headerImageUrl = "headerImage/network1",
            publishDate = Instant.parse("2022-10-04T23:00:00.000Z"),
            type = Event,
            topics = listOf("1")
        )
        private val NETWORK_NEWS_RESOURCE_2 = NetworkNewsResource(
            id = "2",
            title = "Title from network 2",
            content = "Content from network 2",
            url = "network2",
            headerImageUrl = "headerImage/network2",
            publishDate = Instant.parse("2022-10-06T23:00:00.000Z"),
            type = Article,
            topics = listOf("1", "3", "19"),
        )

        private val NEWS_RESOURCE_1 = NewsResourceEntity(
            id = "1",
            title = "news for Topic 2",
            content = "Hilt",
            url = "url",
            headerImageUrl = "headerImageUrl",
            type = Video,
            publishDate = Instant.fromEpochMilliseconds(1),
        )
        private val NEWS_RESOURCE_2 = NewsResourceEntity(
            id = "2",
            title = "news for Topic 1",
            content = "Espresso",
            url = "url",
            headerImageUrl = "headerImageUrl",
            type = Video,
            publishDate = Instant.fromEpochMilliseconds(2),
        )
        private val NEWS_RESOURCE_5 = NewsResourceEntity(
            id = "5",
            title = "news w/o Topic",
            content = "Truth",
            url = "url",
            headerImageUrl = "headerImageUrl",
            type = Article,
            publishDate = Instant.fromEpochMilliseconds(3),
        )

        /** NewsResource 2から参照されているトピック */
        private val TOPIC_1 = TopicEntity(
            id = "1",
            name = "Topic 1",
            shortDescription = "short description 1",
            longDescription = "long description 1",
            url = "URL 1",
            imageUrl = "image URL 1",
        )
    }
}

private fun createEmptyTopicEntity(id: String) = TopicEntity(
    id = id,
    name = "",
    shortDescription = "",
    longDescription = "",
    url = "",
    imageUrl = "",
)
