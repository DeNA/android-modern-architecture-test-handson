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

package com.google.samples.apps.nowinandroid.ui

import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.filterToOne
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.testing.TestNavHostController
import com.google.common.truth.Truth
import com.google.samples.apps.nowinandroid.core.domain.GetUserNewsResourcesUseCase
import com.google.samples.apps.nowinandroid.core.testing.util.TestNetworkMonitor
import com.google.samples.apps.nowinandroid.feature.bookmarks.BookmarksViewModel
import com.google.samples.apps.nowinandroid.feature.bookmarks.navigation.bookmarksRoute
import com.google.samples.apps.nowinandroid.feature.foryou.navigation.forYouNavigationRoute
import com.google.samples.apps.nowinandroid.feature.interests.navigation.interestsRoute
import com.google.samples.apps.nowinandroid.uitesthiltmanifest.HiltComponentActivity
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.mockk
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import com.google.samples.apps.nowinandroid.feature.bookmarks.R as BookmarksR
import com.google.samples.apps.nowinandroid.feature.foryou.R as ForYouR
import com.google.samples.apps.nowinandroid.feature.interests.R as InterestsR

/**
 * [TestNavHostController]を使ったNavigationのテスト。
 * UIコンポーネントをタップした結果、`currentDestination`の値がどう変化したかを確認する。
 */
@Suppress("NonAsciiCharacters", "RemoveRedundantBackticks")
@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@HiltAndroidTest
class TestNavHostControllerNavigationTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @BindValue @get:Rule(order = 1)
    val tmpFolder: TemporaryFolder = TemporaryFolder.builder().assureDeletion().build()

    // HiltComponentActivityは、@AndroidEntryPointアノテーションがついた空のComponentActivity
    @get:Rule(order = 2)
    val composeTestRule = createAndroidComposeRule<HiltComponentActivity>()

    // 必要に応じてViewModelを差し替えても良い
    @BindValue
    val bookmarksViewModel: BookmarksViewModel = BookmarksViewModel(
        userDataRepository = mockk(relaxed = true),
        getSaveableNewsResources = GetUserNewsResourcesUseCase(
            newsRepository = mockk(relaxed = true),
            userDataRepository = mockk(relaxed = true)
        )
    )

    private lateinit var testNavHostController: TestNavHostController
    private lateinit var forYou: String
    private lateinit var saved: String
    private lateinit var interests: String

    @Before
    fun setUp() {
        composeTestRule.activity.apply {
            forYou = getString(ForYouR.string.for_you)
            saved = getString(BookmarksR.string.saved)
            interests = getString(InterestsR.string.interests)
        }

        composeTestRule.setContent {
            val networkMonitor = TestNetworkMonitor()
            val windowSizeClass = calculateWindowSizeClass(activity = composeTestRule.activity)
            testNavHostController = rememberTestNavController()
            NiaApp(
                windowSizeClass = windowSizeClass,
                networkMonitor = networkMonitor,
                appState = rememberNiaAppState(
                    windowSizeClass = windowSizeClass,
                    networkMonitor = networkMonitor,
                    navController = testNavHostController
                )
            )
        }
    }

    @Test
    fun `SavedをタップするとBookmarks画面になっていること`() {
        composeTestRule.apply {
            onNodeWithText(saved).performClick()
            Truth
                .assertThat(testNavHostController.currentDestination?.route)
                .isEqualTo(bookmarksRoute)
        }
    }

    @Test
    fun `Interests画面で個々のトピックをタップするとトピック詳細画面になっていること`() {
        composeTestRule.apply {
            // [TestNavHostController.setCurrentDestination] を使えばいきなり特定の画面から始められる
            // Interests画面に遷移する
            runOnUiThread {
                testNavHostController.setCurrentDestination(destRoute = interestsRoute)
            }
            // `interests:topic`タグ内にある "Android Auto" というテキストを見付け、そこをタップする
            onNodeWithTag("interests:topics")
                .onChildren()
                .filterToOne(hasText("Android Auto"))
                .performClick()

            // destinationが "topic_route/{topicId}" (トピック詳細画面) となることを確認する
            Truth
                .assertThat(testNavHostController.currentDestination?.route)
                .isEqualTo("topic_route/{topicId}")
        }
    }

    // 練習1: ボトムナビゲーションの「Interests」をタップするとInterests画面に遷移すること
    @Test
    fun `InterestsをタップするとInterests画面になっていること`() {
        // ==== 練習問題ここから
        composeTestRule.apply {
            onNodeWithText(interests).performClick()
            Truth
                .assertThat(testNavHostController.currentDestination?.route)
                .isEqualTo(interestsRoute)
        }
        // ==== 練習問題ここまで
    }

    @Test
    fun `初期状態でForYou画面になっていること`() {
        Truth
            .assertThat(testNavHostController.currentDestination?.route)
            .isEqualTo(forYouNavigationRoute)
    }
}

@Composable
private fun rememberTestNavController(): TestNavHostController {
    val context = LocalContext.current
    val navController = remember {
        TestNavHostController(context).apply {
            navigatorProvider.addNavigator(ComposeNavigator())
        }
    }
    return navController
}
