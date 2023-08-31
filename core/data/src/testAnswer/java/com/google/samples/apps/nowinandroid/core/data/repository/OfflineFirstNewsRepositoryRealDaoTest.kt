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

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Correspondence
import com.google.common.truth.Truth
import com.google.samples.apps.nowinandroid.core.data.testdoubles.TestNetworkDataSource
import com.google.samples.apps.nowinandroid.core.database.NiaDatabase
import com.google.samples.apps.nowinandroid.core.database.dao.NewsResourceDao
import com.google.samples.apps.nowinandroid.core.database.dao.TopicDao
import com.google.samples.apps.nowinandroid.core.database.model.NewsResourceEntity
import com.google.samples.apps.nowinandroid.core.database.model.NewsResourceTopicCrossRef
import com.google.samples.apps.nowinandroid.core.database.model.TopicEntity
import com.google.samples.apps.nowinandroid.core.model.data.NewsResource
import com.google.samples.apps.nowinandroid.core.model.data.NewsResourceType.Article
import com.google.samples.apps.nowinandroid.core.model.data.NewsResourceType.Event
import com.google.samples.apps.nowinandroid.core.model.data.NewsResourceType.Video
import com.google.samples.apps.nowinandroid.core.model.data.Topic
import com.google.samples.apps.nowinandroid.core.network.model.NetworkNewsResource
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
class OfflineFirstNewsRepositoryRealDaoTest {

    private lateinit var repository: OfflineFirstNewsRepository

    private lateinit var newsResourceDao: NewsResourceDao

    private lateinit var topicDao: TopicDao

    private lateinit var network: TestNetworkDataSource
    private lateinit var db: NiaDatabase

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(
            context, NiaDatabase::class.java
        ).build()
        newsResourceDao = db.newsResourceDao()
        topicDao = db.topicDao()
        network = TestNetworkDataSource()

        repository = OfflineFirstNewsRepository(
            newsResourceDao = newsResourceDao,
            topicDao = topicDao,
            network = network,
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `getNewsResources_DAOに格納されているレコードと同じものが取れること`() = runTest {
        // 実装の方針:
        // 1. newsResourceDao.insertOrIgnoreNewsResources() を使って、テストデータをデータベースに格納する
        //    - NEWS_RESOURCE_1, NEWS_RESOURCE_2, NEWS_RESOURCE_5 を格納する
        // 2. repositoryのgetNewsResources()を呼ぶ
        // 3. その結果が NEWS_RESOURCE_1, NEWS_RESOURCE_2, NEWS_RESOURCE_5 を NewsResource型に変換したものになっていることを確認する

        // newsResourceDao.insertOrIgnoreNewsResources() を使って、テストデータをデータベースに格納する
        newsResourceDao.insertOrIgnoreNewsResources(
            listOf(NEWS_RESOURCE_1, NEWS_RESOURCE_2, NEWS_RESOURCE_5)
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
    fun `pullNewsResources_ネットワークから取得したもので更新されていること`() = runTest {
        // 実装の方針:
        // 1. pullNewsResources()の中で呼ばれる network.getNewsResources() が
        //    テストデータ(`List<NetworkNewsResource>`)を返すように設定する
        // 2. repositoryのpullNewsResourcesを呼び出す
        // 3. repositoryのgetNewsResourcesを呼び出す
        // 4. 「3」の結果が、「1」で設定した値をNewsResource型に変換したものになっていることを確認する
        //    ※テストデータである`NetworkNewsResource`にはtopicのIDしか保持していない。
        //      その場合、DAOに格納されるTopicEntityは、ID以外の値は空文字列になる。

        // pullNewsResources()の中で呼ばれる network.getNewsResources() の戻り値を設定する
        network.getNewsResourcesFunc = {
            listOf(NETWORK_NEWS_RESOURCE_1, NETWORK_NEWS_RESOURCE_2)
        }

        repository.pullNewsResources()

        val actual = repository.getNewsResources().first()

        // NETWORK_NEWS_RESOURCE_1, NETWORK_NEWS_RESOURCE_2 をそれぞれNewsResourceに変換したもの
        // と一致していることを確認する
        // - NewsResourceはTopicの実体を持っているが、TopicのID以外のプロパティは空文字列になる
        // - topicsプロパティ内の順序は順不同なので、Truthにtopicプロパティ内の順序だけ無視する設定を入れる必要がある
        val expected = listOf(
            NewsResource(
                id = "1",
                title = "Title from network 1",
                content = "Content from network 1",
                url = "network1",
                headerImageUrl = "headerImage/network1",
                publishDate = Instant.parse("2022-10-04T23:00:00.000Z"),
                type = Event,
                topics = listOf(createEmptyTopic("1"))
            ),
            NewsResource(
                id = "2",
                title = "Title from network 2",
                content = "Content from network 2",
                url = "network2",
                headerImageUrl = "headerImage/network2",
                publishDate = Instant.parse("2022-10-06T23:00:00.000Z"),
                type = Article,
                topics = listOf(
                    createEmptyTopic("1"),
                    createEmptyTopic("3"),
                    createEmptyTopic("19"),
                )
            )
        )

        Truth.assertThat(actual)
            // topics: List<Topic> 内の順序を無視して比較するための設定
            .comparingElementsUsing(NETWORK_NEWS_RESOURCE_EQUIVALENCE)
            .containsExactlyElementsIn(expected)
    }

    @Test
    fun `getNewsResources(filterTopicIds)_DAOに格納されているレコードのうち、引数に指定したトピックIDに対応するNewsResourceだけが取れること`() =
        runTest {
            // 実装の方針:
            // 1. 次のようなテストデータを用意する
            // - NewsResourceEntity(ID=1): 関連トピック TopicEntity(ID=2) を持つ
            // - NewsResourceEntity(ID=2): 関連トピック TopicEntity(ID=1) を持つ
            // - NewsResourceEntity(ID=5): 関連トピックなし
            // ※ NewsResourceEntityとTopicEntityの関係は関連テーブル(NewsResourceTopicCrossRef)に格納する
            //   | newsResourceId | topicId |
            //   | 2              | 1       |
            //   | 1              | 2       |
            //
            // 2. 用意したテストデータをテーブルに格納する
            // 3. 引数にID=1を指定して、repositoryのgetNewsResources()を呼ぶ
            // 4. その結果が NEWS_RESOURCE_2 を NewsResource型に変換したものになっていることを確認する

            // TopicEntityの用意 (ID=1, 2, 4)
            topicDao.insertOrIgnoreTopics(listOf(TOPIC_1, TOPIC_2))

            // NewsResourceEntityの用意 (ID=1, 2, 5)
            newsResourceDao.insertOrIgnoreNewsResources(
                listOf(NEWS_RESOURCE_1, NEWS_RESOURCE_2, NEWS_RESOURCE_5)
            )

            // 関連テーブルの用意
            // これによって作られる関連テーブルは次の通り
            //   | newsResourceId | topicId |
            //   | 2              | 1       |
            //   | 1              | 2       |
            newsResourceDao.insertOrIgnoreTopicCrossRefEntities(
                listOf(
                    NewsResourceTopicCrossRef(newsResourceId = "2", topicId = "1"),
                    NewsResourceTopicCrossRef(newsResourceId = "1", topicId = "2"),
                )
            )

            // ID=1のトピック(TOPIC_1) に関連している NewsResourceEntityを取り出す
            val actual = repository.getNewsResources(filterTopicIds = setOf("1")).first()

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
    fun `getNewsResources(filterTopicIds)_どのNewsResourceからも参照されていないTopic IDを引数に指定すると空の結果となること`() =
        runTest {
            topicDao.insertOrIgnoreTopics(listOf(TOPIC_1, TOPIC_2, TOPIC_4_DANGLING))
            newsResourceDao.insertOrIgnoreNewsResources(
                listOf(NEWS_RESOURCE_1, NEWS_RESOURCE_2, NEWS_RESOURCE_5)
            )
            newsResourceDao.insertOrIgnoreTopicCrossRefEntities(
                listOf(
                    NewsResourceTopicCrossRef(newsResourceId = "2", topicId = "1"),
                    NewsResourceTopicCrossRef(newsResourceId = "1", topicId = "2"),
                )
            )
            // ヒント: どこからも参照されていないTopic IDは4
            // ====練習問題 ここから ====
            val actual =
                repository.getNewsResources(filterTopicIds = setOf(TOPIC_4_DANGLING.id)).first()

            Truth.assertThat(actual).isEmpty()
            // ====練習問題 ここまで ====
        }

    @Test
    fun `pullNewsResources_DBに存在しなかったTopicが生成されていること`() = runTest {
        // これによってTopic ID 1, 2, 4 が格納される
        topicDao.insertOrIgnoreTopics(listOf(TOPIC_1, TOPIC_2, TOPIC_4_DANGLING))

        network.getNewsResourcesFunc = {
            listOf(NETWORK_NEWS_RESOURCE_1, NETWORK_NEWS_RESOURCE_2)
            // これらから参照されているTopic IDは 1, 3, 19
        }
        repository.pullNewsResources()

        val actualTopicIds: List<String> = topicDao.getTopicEntities()
            .first()
            .map(TopicEntity::id)

        // ====練習問題 ここから ====
        Truth.assertThat(
            actualTopicIds
        ).containsExactly(
            // DAOに投入した初期データの                 Topic = 1, 2, 4
            // ネットワークから取ってきたデータから参照されたTopic = 1, 3, 19
            // マージすると 1, 2, 4, 3, 19
            "1", "2", "4", "3", "19"
        )
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

        /** NewsResource 1から参照されているトピック */
        private val TOPIC_2 = TopicEntity(
            id = "2",
            name = "Topic 2",
            shortDescription = "short description 2",
            longDescription = "long description 2",
            url = "URL 2",
            imageUrl = "image URL 2",
        )

        /** どのNewsResourceからも参照されていないトピック */
        private val TOPIC_4_DANGLING = TopicEntity(
            id = "4",
            name = "Topic 4",
            shortDescription = "short description 4",
            longDescription = "long description 4",
            url = "URL 4",
            imageUrl = "image URL 4",
        )
    }

    private fun createEmptyTopic(id: String) = Topic(
        id = id,
        name = "",
        shortDescription = "",
        longDescription = "",
        url = "",
        imageUrl = "",
    )
}

private fun NewsResource?.equalsIgnoreOrderOfTopics(other: NewsResource?): Boolean {
    if (this == null && other == null) return true
    if (this == null || other == null) return false
    val thisTopics = this.topics.toSet()
    val otherTopics = other.topics.toSet()
    val thisWithoutTopics = this.copy(topics = listOf())
    val otherWithoutTopics = other.copy(topics = listOf())
    return thisWithoutTopics == otherWithoutTopics && thisTopics == otherTopics
}

private val NETWORK_NEWS_RESOURCE_EQUIVALENCE =
    Correspondence.from<NewsResource, NewsResource>(
        { actual, expected -> actual.equalsIgnoreOrderOfTopics(expected) },
        "equals ignore order of topics"
    )