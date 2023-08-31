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

package com.google.samples.apps.nowinandroid.ui

import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.filterToOne
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.google.samples.apps.nowinandroid.MainActivity
import com.google.samples.apps.nowinandroid.R
import com.google.samples.apps.nowinandroid.core.testing.util.TestNetworkMonitor
import com.google.samples.apps.nowinandroid.uitesthiltmanifest.HiltComponentActivity
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import com.google.samples.apps.nowinandroid.feature.bookmarks.R as BookmarksR
import com.google.samples.apps.nowinandroid.feature.foryou.R as FeatureForyouR
import com.google.samples.apps.nowinandroid.feature.interests.R as FeatureInterestsR
import com.google.samples.apps.nowinandroid.feature.settings.R as SettingsR

/**
 * 実Activityと結合してNavigation Flowをテストします。
 */
@Suppress("NonAsciiCharacters", "RemoveRedundantBackticks")
@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@HiltAndroidTest
class RealNavHostControllerNavigationTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    /**
     * [com.google.samples.apps.nowinandroid.core.datastore.test.TestDataStoreModule]
     * で必要な [TemporaryFolder] を用意する。
     */
    @BindValue @get:Rule(order = 1)
    val tmpFolder: TemporaryFolder = TemporaryFolder.builder().assureDeletion().build()

    /**
     * [MainActivity] を使ってcomposeをテストする。
     */
    @get:Rule(order = 2)
    val composeTestRule = createAndroidComposeRule<HiltComponentActivity>()

    // The strings used for matching in these tests
    private lateinit var forYou: String
    private lateinit var interests: String
    private lateinit var sampleTopic: String
    private lateinit var appName: String
    private lateinit var saved: String
    private lateinit var settings: String
    private lateinit var ok: String

    @Before
    fun setup() {
        composeTestRule.activity.apply {
            forYou = getString(FeatureForyouR.string.for_you)
            interests = getString(FeatureInterestsR.string.interests)
            sampleTopic = "Headlines"
            appName = getString(R.string.app_name)
            saved = getString(BookmarksR.string.saved)
            settings = getString(SettingsR.string.top_app_bar_action_icon_description)
            ok = getString(SettingsR.string.dismiss_dialog_button_text)
        }
        composeTestRule.setContent {
            val networkMonitor = TestNetworkMonitor()
            val windowSizeClass = calculateWindowSizeClass(activity = composeTestRule.activity)
            NiaApp(
                windowSizeClass = windowSizeClass,
                networkMonitor = networkMonitor,
            )
        }
    }

    @Test
    fun `Savedタブをタップすると、TopAppBarにSavedと表示されること`() {
        composeTestRule.apply {
            // NiaBottomBarにある`Saved`をクリックする
            onNode(hasText(saved) and hasAnyAncestor(hasTestTag("NiaBottomBar")))
                .performClick()

            // NiaTopAppBarに`Saved`というテキストが表示されていることを確認する
            // 確認方法①: この方法だと直接の子供しか探せない
            onNodeWithTag("NiaTopAppBar")
                .onChildren()
                .filterToOne(hasText(saved)).assertIsDisplayed()

            // 確認方法②: この方法だと、子孫にtextがあっても探せるので汎用性が高い
            onNode(hasText(saved) and hasAnyAncestor(hasTestTag("NiaTopAppBar")))
                .assertIsDisplayed()
        }
    }

    @Test
    fun `Interests画面で個々のトピックをタップするとトピック詳細画面になっていること`() {
        composeTestRule.apply {
            // NiaBottomBarのInterestsをタップしてInterests画面に遷移する
            onNode(hasText(interests) and hasAnyAncestor(hasTestTag("NiaBottomBar")))
                .performClick()
            // トピックのリストから`Android Auto`をタップし、そのトピックの詳細画面に遷移する
            onNodeWithText("Android Auto").performClick()
            // トピックの詳細にに表示されるテキスト
            // 「The latest news on Android Automotive OS and Android Auto.」
            // が表示されていることを確認する。
            //
            // 「ViewModelを結合してComposeをテストする」で触れたように、
            // プロダクトコード側に「news:expandedCard」テストタグを付けて、そちらを条件にassertするとより安定度が増す。
            onNodeWithText("The latest news on Android Automotive OS and Android Auto.").assertIsDisplayed()
        }
    }

    @Test
    fun `Settingsアイコンをタップすると、Settingsダイアログが表示されること`() {
        composeTestRule.apply {
            // 歯車アイコン (contentDescriptionが"Settings") をタップする
            onNodeWithContentDescription(settings).performClick()
            // ダイアログ内に "Settings" が表示されていることを確認する
            onNode(hasText(settings) and hasAnyAncestor(isDialog()))
                .assertIsDisplayed()
        }
    }

    // 練習1: ボトムナビゲーションの「Interests」をタップするとInterests画面に遷移すること
    @Test
    fun `Interestsタブをタップすると、TopAppBarにInterestsと表示されること`() {
        // ==== 練習問題ここから
        composeTestRule.apply {
            onNode(hasText(interests) and hasAnyAncestor(hasTestTag("NiaBottomBar")))
                .performClick()
            onNode(hasText(interests) and hasAnyAncestor(hasTestTag("NiaTopAppBar")))
                .assertIsDisplayed()
        }
        // ==== 練習問題ここまで
    }

    // 練習2: Interestsタブでトピック詳細画面を表示した状態でForYouタブに遷移し、再度Interestsタブに遷移すると、
    //       トピック詳細画面が表示されたままになっていること
    @Test
    fun `Interestsタブの詳細画面から別タブに遷移し、Interestsタブに戻ると、詳細画面が表示されたままになっていること`() {
        composeTestRule.apply {
            // ==== 練習問題ここから
            // NiaBottomBarのInterestsをタップしてInterests画面に遷移する
            onNode(hasText(interests) and hasAnyAncestor(hasTestTag("NiaBottomBar")))
                .performClick()
            // トピックのリストから`Android Auto`をタップし、そのトピックの詳細画面に遷移する
            onNodeWithText("Android Auto").performClick()
            // NiaBottomBarのForYouをタップしてForYou画面に遷移する
            onNode(hasText(forYou) and hasAnyAncestor(hasTestTag("NiaBottomBar")))
                .performClick()
            // NiaBottomBarのInterestsをタップしてInterests画面に遷移する
            onNode(hasText(interests) and hasAnyAncestor(hasTestTag("NiaBottomBar")))
                .performClick()
            // ==== 練習問題ここまで
            // トピックの詳細にしか存在しないテストタグ`news:expandedCard`のノードが表示されていることを確認する
            onNodeWithTag("news:expandedCard").assertIsDisplayed()
        }
    }
}
