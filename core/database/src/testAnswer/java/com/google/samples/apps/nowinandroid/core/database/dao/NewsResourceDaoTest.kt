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

package com.google.samples.apps.nowinandroid.core.database.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth
import com.google.samples.apps.nowinandroid.core.database.NiaDatabase
import com.google.samples.apps.nowinandroid.core.database.model.NewsResourceEntity
import com.google.samples.apps.nowinandroid.core.database.model.NewsResourceTopicCrossRef
import com.google.samples.apps.nowinandroid.core.database.model.PopulatedNewsResource
import com.google.samples.apps.nowinandroid.core.database.model.TopicEntity
import com.google.samples.apps.nowinandroid.core.model.data.NewsResourceType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@Suppress("NonAsciiCharacters", "RemoveRedundantBackticks")
@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE) // To avoid "WARNING: No manifest file found at ./AndroidManifest.xml."
class NewsResourceDaoTest {

    private lateinit var newsResourceDao: NewsResourceDao
    private lateinit var topicDao: TopicDao
    private lateinit var db: NiaDatabase

    /**
     * テスト用のデータベースを生成して、
     * テスト対象となる[NewsResourceDao]と[TopicDao]をフィールドに保持します。
     */
    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        // インメモリデータベースとして作成しているが、ファイル名を指定して作成してもRobolectricでテスト可能
        db = Room.inMemoryDatabaseBuilder(
            context,
            NiaDatabase::class.java
        ).build()
        newsResourceDao = db.newsResourceDao()
        topicDao = db.topicDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `getNewsResources_格納したNewsResourceEntityがpublish_dateの降順で取得できること`() = runTest {
        // 実装の方針:
        // 1. テストデータを `upsertNewsResources` を使ってテーブルに格納する
        // 2. 格納したテストデータが `getNewsResources()` を使って取り出す
        // 3. 取り出した結果が、`millisSinceEpoch`の降順に並んでいることを確認する
        val newsResource0 = testNewsResource(id = "0", millisSinceEpoch = 0)
        val newsResource1 = testNewsResource(id = "1", millisSinceEpoch = 3)
        val newsResource2 = testNewsResource(id = "2", millisSinceEpoch = 1)
        val newsResource3 = testNewsResource(id = "3", millisSinceEpoch = 2)
        val newsResourceEntities =
            listOf(newsResource0, newsResource1, newsResource2, newsResource3)
        newsResourceDao.upsertNewsResources(newsResourceEntities)

        val actual = newsResourceDao.getNewsResources().first()

        Truth.assertThat(
            actual
        ).containsExactly(
            PopulatedNewsResource(entity = newsResource1, topics = emptyList()),
            PopulatedNewsResource(entity = newsResource3, topics = emptyList()),
            PopulatedNewsResource(entity = newsResource2, topics = emptyList()),
            PopulatedNewsResource(entity = newsResource0, topics = emptyList()),
        ).inOrder()
    }

    @Test
    fun `getNewsResources(filterTopicIds)_引数に指定したTopicに対応するNewsResourceEntityだけがpublish_dateの降順で取得できること`() {
        runTest {
            // 実装の方針:
            // 1. 次のようなテストデータを用意する
            // - NewsResourceEntity(ID=0): 関連トピック TopicEntity(ID=1) を持つ
            // - NewsResourceEntity(ID=1): 関連トピック TopicEntity(ID=2) を持つ
            // - NewsResourceEntity(ID=2): 関連トピックなし
            // ※ NewsResourceEntityとTopicEntityの関係は関連テーブル(NewsResourceTopicCrossRef)に格納する
            //   | newsResourceId | topicId |
            //   | 0              | 1       |
            //   | 1              | 2       |
            //
            // 2. 用意したテストデータをテーブルに格納する
            // 3. getNewsResources(setOf("2"))を呼び出すと
            //    ID=2のトピックに関連しているID=1のNewsResourceEntityが取得できることを確認する

            // TopicEntityの用意
            val topic1 = testTopicEntity(id = "1", name = "1")
            val topic2 = testTopicEntity(id = "2", name = "2")
            val topicEntities = listOf(topic1, topic2)

            // NewsResourceEntityの用意
            val newsResource0 = testNewsResource(id = "0", millisSinceEpoch = 0)
            val newsResource1 = testNewsResource(id = "1", millisSinceEpoch = 3)
            val newsResource2 = testNewsResource(id = "2", millisSinceEpoch = 1)
            val newsResourceEntities = listOf(newsResource0, newsResource1, newsResource2)

            // 関連テーブルの用意
            // これによって作られる関連テーブルは次の通り
            // | newsResourceId | topicId |
            // | 0              | 1       |
            // | 1              | 2       |
            val newsResourceTopicCrossRefEntities = listOf(
                NewsResourceTopicCrossRef(newsResourceId = "0", topicId = "1"),
                NewsResourceTopicCrossRef(newsResourceId = "1", topicId = "2"),
            )

            // 用意したテストデータをテーブルに格納する
            topicDao.insertOrIgnoreTopics(topicEntities)
            newsResourceDao.upsertNewsResources(newsResourceEntities)
            newsResourceDao.insertOrIgnoreTopicCrossRefEntities(newsResourceTopicCrossRefEntities)

            // topic2に関連しているNewsResourceEntityを取り出す
            val actual = newsResourceDao.getNewsResources(setOf("2")).first()

            // topic2に関連しているNewsResourceEntityはnewsResource1
            Truth.assertThat(
                actual
            ).containsExactly(
                PopulatedNewsResource(entity = newsResource1, topics = listOf(topic2))
            )
        }
    }

    @Test
    fun `deleteNewsResources_引数に指定したIDのNewsResourceEntityが削除されること`() = runTest {
        val newsResource0 = testNewsResource(id = "0", millisSinceEpoch = 0)
        val newsResource1 = testNewsResource(id = "1", millisSinceEpoch = 3)
        val newsResource2 = testNewsResource(id = "2", millisSinceEpoch = 1)
        val newsResource3 = testNewsResource(id = "3", millisSinceEpoch = 2)
        val newsResourceEntities =
            listOf(newsResource0, newsResource1, newsResource2, newsResource3)
        newsResourceDao.upsertNewsResources(newsResourceEntities)

        val toDelete = listOf("0", "2")

        // ====練習問題 ここから ====
        newsResourceDao.deleteNewsResources(toDelete)

        Truth.assertThat(
            newsResourceDao.getNewsResources().first()
        ).containsExactly(
            PopulatedNewsResource(entity = newsResource1, topics = emptyList()),
            PopulatedNewsResource(entity = newsResource3, topics = emptyList()),
        )
        // ====練習問題 ここまで ====
    }

    @Test
    fun `insertOrIgnoreNewsResources_格納済みのレコードと同じidのNewsResourceEntityを格納しようとすると無視されること`() =
        runTest {
            val oldNewsResource1 = testNewsResource(
                id = "1",
                title = "old title",
                millisSinceEpoch = 3,
            )
            val newNewsResource0 = testNewsResource(
                id = "0",
                title = "new title",
                millisSinceEpoch = 0,
            )
            val newNewsResource1 = testNewsResource(
                id = "1",
                title = "new title",
                millisSinceEpoch = 3,
            )

            val oldNewsResourceEntities = listOf(oldNewsResource1)
            val newNewsResourceEntities = listOf(newNewsResource0, newNewsResource1)

            // oldNewsResourceEntities → newNewsResourceEntitiesの順に insertOrIgnoreNewsResources を行う
            newsResourceDao.insertOrIgnoreNewsResources(oldNewsResourceEntities)
            newsResourceDao.insertOrIgnoreNewsResources(newNewsResourceEntities)

            // ====練習問題 ここから ====
            Truth.assertThat(
                newsResourceDao.getNewsResources().first()
            ).containsExactly(
                PopulatedNewsResource(entity = oldNewsResource1, topics = emptyList()),
                PopulatedNewsResource(entity = newNewsResource0, topics = emptyList()),
            )
            // ====練習問題 ここまで ====
        }
}

private fun testTopicEntity(
    id: String = "0",
    name: String
) = TopicEntity(
    id = id,
    name = name,
    shortDescription = "",
    longDescription = "",
    url = "",
    imageUrl = ""
)

private fun testNewsResource(
    id: String = "0",
    millisSinceEpoch: Long = 0,
    title: String = ""
) = NewsResourceEntity(
    id = id,
    title = title,
    content = "",
    url = "",
    headerImageUrl = "",
    publishDate = Instant.fromEpochMilliseconds(millisSinceEpoch),
    type = NewsResourceType.DAC,
)
