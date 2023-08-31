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

package com.google.samples.apps.nowinandroid.core.database.dao

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth
import com.google.samples.apps.nowinandroid.core.database.DatabaseMigrations
import com.google.samples.apps.nowinandroid.core.database.NiaDatabase
import com.google.samples.apps.nowinandroid.core.database.model.TopicEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NiaDatabaseMigrationTest {
    private val TEST_DB = "migration-test"

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        NiaDatabase::class.java,
        listOf(
            DatabaseMigrations.Schema2to3(),
            DatabaseMigrations.Schema10to11(),
            DatabaseMigrations.Schema11to12()
        )
    )

    /**
     * バージョン8からバージョン11までのマイグレーションをテストする。
     *
     * - バージョン8のauthorsテーブルのcreateSql (8.jsonより):
     *   - "CREATE TABLE IF NOT EXISTS `${TABLE_NAME}` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `image_url` TEXT NOT NULL, `twitter` TEXT NOT NULL DEFAULT '', `medium_page` TEXT NOT NULL DEFAULT '', PRIMARY KEY(`id`))"
     * - バージョン11のauthorsテーブルのcreateSql (11.jsonより):
     *   - "CREATE TABLE IF NOT EXISTS `${TABLE_NAME}` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `image_url` TEXT NOT NULL, `twitter` TEXT NOT NULL DEFAULT '', `medium_page` TEXT NOT NULL DEFAULT '', `bio` TEXT NOT NULL DEFAULT '', PRIMARY KEY(`id`))"
     *
     * 差分を見ると`bio`カラムが増えていることがわかるので、マイグレーション後にbioカラムがデフォルト値""で埋まっていることを確認する。
     */
    @Test
    fun migrate8to11() {
        helper.createDatabase(TEST_DB, 8).use { db ->
            // バージョン8のデータベースのauthorsテーブルにレコードを格納する。
            val values = ContentValues().apply {
                put("id", "1")
                put("name", "DeNA Tech members")
                put("image_url", "https://example.com/image.png")
                put("twitter", "@DeNAxTech")
                put("medium_page", "https://example.com/swet/")
            }
            db.insert("authors", SQLiteDatabase.CONFLICT_IGNORE, values)
        }

        // バージョン11のデータベースに移行する。
        // MigrationTestHelperが最低限のバリデーションをしてくれる
        helper.runMigrationsAndValidate(TEST_DB, 11, true).use { db ->
            // 移行後のauthorsテーブルについて
            // - name と image_urlカラムの値が、移行前と同じ値になっていることを確認する
            // - 移行後に新しく作られたbioカラムの値が""になっていることを確認する
            db.query("SELECT * FROM `authors` WHERE `name` = ?", arrayOf("DeNA Tech members"))
                .use { cursor ->
                    cursor.moveToNext()
                    val nameIndex = cursor.getColumnIndex("name")
                    val imageUrlIndex = cursor.getColumnIndex("image_url")
                    val bioIndex = cursor.getColumnIndex("bio")

                    Truth.assertThat(cursor.getString(nameIndex)).isEqualTo("DeNA Tech members")
                    Truth.assertThat(cursor.getString(imageUrlIndex))
                        .isEqualTo("https://example.com/image.png")
                    Truth.assertThat(cursor.getString(bioIndex)).isEmpty()
                }
        }
    }

    /**
     * バージョン8から最新バージョン(バージョン12)までのマイグレーションをテストする。
     * topicsテーブルに格納されたデータがそのまま残っていることを確認する。
     */
    @Test
    fun migrate8to12() = runTest {
        helper.createDatabase(TEST_DB, 8).use { db ->
            val topicsValues = ContentValues().apply {
                put("id", "2")
                put("name", "Topic 2")
                put("shortDescription", "Topic 2 short")
                put("longDescription", "Topic 2 long")
                put("url", "https://topic.example.com/2")
                put("imageUrl", "https://example.com/image2.png")
            }
            db.insert("topics", SQLiteDatabase.CONFLICT_IGNORE, topicsValues)
        }

        Room.databaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            NiaDatabase::class.java,
            TEST_DB
        ).build().apply {
            try {
                val topicDao = topicDao()
                val topicEntities = topicDao.getTopicEntities().first()
                Truth.assertThat(topicEntities)
                    .containsExactly(
                        TopicEntity(
                            id = "2",
                            name = "Topic 2",
                            shortDescription = "Topic 2 short",
                            longDescription = "Topic 2 long",
                            url = "https://topic.example.com/2",
                            imageUrl = "https://example.com/image2.png"
                        )
                    )
            } finally {
                close()
            }
        }
    }

    /**
     * バージョン2からバージョン3までのマイグレーションをテストする。
     *
     * - バージョン2のtopicsテーブルのcreateSql (2.jsonより):
     *   - "CREATE TABLE IF NOT EXISTS `${TABLE_NAME}` (`id` INTEGER NOT NULL, `name` TEXT NOT NULL, `description` TEXT NOT NULL, PRIMARY KEY(`id`))"
     * - バージョン3のtopicsテーブルのcreateSql (3.jsonより):
     *   - "CREATE TABLE IF NOT EXISTS `${TABLE_NAME}` (`id` INTEGER NOT NULL, `name` TEXT NOT NULL, `shortDescription` TEXT NOT NULL, `longDescription` TEXT NOT NULL DEFAULT '', `url` TEXT NOT NULL DEFAULT '', `imageUrl` TEXT NOT NULL DEFAULT '', PRIMARY KEY(`id`))"
     *
     */
    @Test
    fun migrate2to3() {
        helper.createDatabase(TEST_DB, 2).use { db ->
            //バージョン2のデータベースのtopicsテーブルにレコードを格納する
            val values = ContentValues().apply {
                put("id", "1")
                put("name", "swet members")
                put("description", "very short description")
            }
            db.insert("topics", SQLiteDatabase.CONFLICT_IGNORE, values)
        }

        // バージョン3のデータベースに移行する
        // MigrationTestHelperが最低限のバリデーションをしてくれる
        // 移行後のtopicsテーブルについて、shortDescriptionカラムの値が、移行前のdescriptionカラムの値と同じになっていることを確認する
        // ==== 練習問題 ここから
        helper.runMigrationsAndValidate(TEST_DB, 3, true).use { db: SupportSQLiteDatabase ->
            db.query("SELECT * FROM `topics` WHERE `name` = ?", arrayOf("swet members"))
                .use { cursor ->
                    cursor.moveToNext()
                    val nameIndex = cursor.getColumnIndex("name")
                    val shortDescriptionIndex = cursor.getColumnIndex("shortDescription")
                    val longDescriptionIndex = cursor.getColumnIndex("longDescription")

                    Truth.assertThat(cursor.getString(nameIndex)).isEqualTo("swet members")
                    Truth.assertThat(cursor.getString(shortDescriptionIndex))
                        .isEqualTo("very short description")
                    Truth.assertThat(cursor.getString(longDescriptionIndex)).isEmpty()
                }
        }
        // ==== 練習問題 ここまで
    }
}
