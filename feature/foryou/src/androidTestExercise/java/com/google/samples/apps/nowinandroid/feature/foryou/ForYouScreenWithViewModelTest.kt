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

package com.google.samples.apps.nowinandroid.feature.foryou

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.hasParent
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.printToLog
import com.google.samples.apps.nowinandroid.core.domain.GetFollowableTopicsUseCase
import com.google.samples.apps.nowinandroid.core.domain.GetUserNewsResourcesUseCase
import com.google.samples.apps.nowinandroid.core.model.data.NewsResource
import com.google.samples.apps.nowinandroid.core.model.data.NewsResourceType.Article
import com.google.samples.apps.nowinandroid.core.model.data.Topic
import com.google.samples.apps.nowinandroid.core.testing.repository.TestNewsRepository
import com.google.samples.apps.nowinandroid.core.testing.repository.TestTopicsRepository
import com.google.samples.apps.nowinandroid.core.testing.repository.TestUserDataRepository
import com.google.samples.apps.nowinandroid.core.testing.repository.emptyUserData
import com.google.samples.apps.nowinandroid.core.testing.util.TestSyncStatusMonitor
import kotlinx.datetime.Instant
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@Suppress("NonAsciiCharacters", "RemoveRedundantBackticks")
class ForYouScreenWithViewModelTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val syncStatusMonitor = TestSyncStatusMonitor()
    private val userDataRepository = TestUserDataRepository()
    private val topicsRepository = TestTopicsRepository()
    private val newsRepository = TestNewsRepository()
    private val getUserNewsResourcesUseCase = GetUserNewsResourcesUseCase(
        newsRepository = newsRepository,
        userDataRepository = userDataRepository
    )
    private val getFollowableTopicsUseCase = GetFollowableTopicsUseCase(
        topicsRepository = topicsRepository,
        userDataRepository = userDataRepository
    )
    private lateinit var viewModel: ForYouViewModel

    @Before
    fun setup() {
        userDataRepository.setUserData(emptyUserData)
        topicsRepository.sendTopics(testTopics)
        newsRepository.sendNewsResources(testNewsResource)
        viewModel = ForYouViewModel(
            syncStatusMonitor = syncStatusMonitor,
            userDataRepository = userDataRepository,
            getSaveableNewsResources = getUserNewsResourcesUseCase,
            getFollowableTopics = getFollowableTopicsUseCase
        )
    }

    @Test
    fun showTree_noNews() {
        composeTestRule.setContent {
            ForYouRoute(
                viewModel = viewModel
            )
        }

        composeTestRule.onRoot().printToLog("noNews")
    }

    @Test
    fun showTree_hasNews() {
        composeTestRule.setContent {
            ForYouRoute(
                viewModel = viewModel
            )
        }

        composeTestRule
            .onNode(hasParent(hasText("Headlines")))
            .assertIsOff()
            .performClick()

        composeTestRule.onRoot().printToLog("hasNews")
    }

    @Test
    fun `トピック未選択かつフォロー中のトピックがない場合、フィードが表示されていないこと`() {
        composeTestRule.setContent {
            ForYouRoute(
                viewModel = viewModel
            )
        }

        composeTestRule
            .onAllNodes(hasTestTag("news:expandedCard"))
            .assertCountEquals(0)
    }

    @Test
    fun `Headlinesと書かれたトピックをクリックするとチェック済みになること`() {
        composeTestRule.setContent {
            ForYouRoute(
                viewModel = viewModel
            )
        }

        composeTestRule
            // Headlinesトピックを見つける
            .onNodeWithContentDescription("Headlines")
            // チェックが付いていないことを確認
            .assertIsOff()
            // クリックする
            .performClick()

        composeTestRule
            // Headlinesトピックを見つける
            .onNodeWithContentDescription("Headlines")
            // チェックが付いていることを確認
            .assertIsOn()
    }

    @Test
    fun `Headlinesと書かれたトピックを選択するとDoneボタンがenabled状態になること`() {
        composeTestRule.setContent {
            ForYouRoute(
                viewModel = viewModel
            )
        }

        composeTestRule
            // Doneと書かれたボタンを見つける
            .onNodeWithText("Done")
            // disabled状態であることを確認
            .assertIsNotEnabled()

        composeTestRule
            // Headlinesトピックを見つける
            .onNodeWithContentDescription("Headlines")
            // チェックが付いていないことを確認
            .assertIsOff()
            // クリックする
            .performClick()

        composeTestRule
            // Headlinesトピックを見つける
            .onNodeWithContentDescription("Headlines")
            // チェックが付いていることを確認
            .assertIsOn()

        composeTestRule
            // Doneと書かれたボタンを見つける
            .onNodeWithText("Done")
            // enabled状態であることを確認
            .assertIsEnabled()
    }

    // 練習1
    // オンボーディングセクション内の`Headlines`と書かれたトピックを選択したときに、
    // `Pixel Watch`と書かれた記事がフィードに表示されることを確認する
    @Test
    fun `Headlinesと書かれたトピックを選択したときに、PixelWatchと書かれたフィードが表示されること`() {
        composeTestRule.setContent {
            ForYouRoute(
                viewModel = viewModel
            )
        }

        // TODO:
        //   1. 前提条件が満たされていることを確認するため、`Pixel Watch`というテキストをもつノードがツリー内に**存在しない**ことを確認する
        //   2. オンボーディングセクションの`Headlines`トピックにチェックが付いていないことを確認する
        //   3. オンボーディングセクションの`Headlines`トピックをクリックする
        //   4. `Pixel Watch`というテキストをもつノードが**表示されている**ことを確認する
    }

    @Test
    fun `enabled状態のDoneボタンを押した時に、オンボーディングのセクションが非表示になること`() {
        composeTestRule.setContent {
            ForYouRoute(
                viewModel = viewModel
            )
        }

        // TODO:
        //   1. 前提条件が満たされていることを確認するため、オンボーディングセクションが表示されていることを確認する
        //   2. この時点では`Done`ボタンがdisabled状態であることを確認する
        //   3. `Done`ボタンをenabled状態にする
        //   4. `Done`ボタンをクリックする
        //   5. オンボーディングセクションが**存在しない**ことを確認する
    }
}

private val testTopics = listOf(
    Topic("1", "Headlines", "", "", "", ""),
    Topic("2", "Android Studio", "", "", "", ""),
    Topic("3", "Compose", "", "", "", ""),
)

private val testNewsResource = listOf(
    NewsResource(
        "100",
        "Pixel Watch",
        "",
        "",
        null,
        Instant.parse("2021-11-08T00:00:00.000Z"),
        Article,
        topics = listOf(Topic("1", "Headlines", "", "", "", ""))
    )
)