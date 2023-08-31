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
package com.google.samples.apps.nowinandroid.feature.foryou

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.hasParent
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.printToLog
import com.google.common.truth.Truth
import com.google.samples.apps.nowinandroid.core.domain.model.FollowableTopic
import com.google.samples.apps.nowinandroid.core.domain.model.previewUserNewsResources
import com.google.samples.apps.nowinandroid.core.model.data.Topic
import com.google.samples.apps.nowinandroid.core.ui.NewsFeedUiState
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@Suppress("NonAsciiCharacters", "RemoveRedundantBackticks")
class ForYouScreenTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private lateinit var onboardingTitle: String

    private val doneButtonMatcher by lazy {
        hasText(
            composeTestRule.activity.resources.getString(R.string.done)
        )
    }

    @Before
    fun setup() {
        composeTestRule.activity.apply {
            onboardingTitle = getString(R.string.onboarding_guidance_title)
        }
    }

    @Test
    fun `Loading中にCircularProgressIndicatorが存在していること`() {
        composeTestRule.setContent {
            BoxWithConstraints {
                ForYouScreen(
                    isSyncing = false,
                    onboardingUiState = OnboardingUiState.Loading,
                    feedState = NewsFeedUiState.Loading,
                    onTopicCheckedChanged = { _, _ -> },
                    saveFollowedTopics = {},
                    onNewsResourcesCheckedChanged = { _, _ -> }
                )
            }
        }

        composeTestRule
            // Loading for you…というcontentDescriptionをもつノードを探す
            .onNodeWithContentDescription("Loading for you…")
            // ツリー上に存在していることを確認する
            .assertExists()
    }

    @Test
    fun `初期状態ではDoneボタンがdisableになっていること`() {
        composeTestRule.setContent {
            ForYouScreen(
                isSyncing = false,
                onboardingUiState = OnboardingUiState.Shown(topics = testTopics),
                feedState = NewsFeedUiState.Success(emptyList()),
                onTopicCheckedChanged = { _, _ -> },
                saveFollowedTopics = {},
                onNewsResourcesCheckedChanged = { _, _ -> }
            )
        }

        composeTestRule
            // Doneと書かれたノードを探す
            .onNodeWithText("Done")
            // disabledである(=enabledではない)ことを確認する
            .assertIsNotEnabled()
    }

    // 練習問題1
    // オンボーディングセクション中の`Headlines`と書かれたトピックがチェックされていないことを確認する
    @Test
    fun `Headlinesと書かれたトピックがチェックされていないこと`() {
        composeTestRule.setContent {
            ForYouScreen(
                isSyncing = false,
                onboardingUiState = OnboardingUiState.Shown(topics = testTopics),
                feedState = NewsFeedUiState.Success(emptyList()),
                onTopicCheckedChanged = { _, _ -> },
                saveFollowedTopics = {},
                onNewsResourcesCheckedChanged = { _, _ -> }
            )
        }

        // TODO:
        //   1. セマンティックツリーの構造をログ出力メソッドを使って確認する
        //   2. Headlineと書かれたトピックに対応するノードを見付ける
        //   3. 見付けたノードがチェックされていないことを確認する
    }

    // 練習問題2
    // 以下のように`SingleTopicButton`が配置されている。
    // それをクリックしたときにonClickアクションが呼ばれることを確認する
    @Test
    fun `SingleTopicButtonを押した時にonClickが呼ばれること`() {
        var onClickCalled = false
        composeTestRule.setContent {
            BoxWithConstraints {
                SingleTopicButton(
                    name = "UI",
                    topicId = "TOPIC_ID_1",
                    imageUrl = "",
                    isSelected = false,
                    onClick = { _, _ ->
                        onClickCalled = true
                    }
                )
            }
        }

        // TODO:
        //   1. セマンティックツリーの構造をログ出力メソッドを使って確認する
        //   2. 上記SingleTopicButtonに対応するノードを見付ける
        //   3. 見付けたノードをクリックする
        //   4. onClick引数に指定されているλ式が呼び出されたことを確認する
    }

    // 練習問題3
    // 1つめのフィードが画面上に表示されており、2つ目のフィードまでスクロールできることを確認する
    @Test
    fun `クリック可能なフィードの1つ目が画面上に表示されていて、2つ目までスクロールできること`() {
        composeTestRule.setContent {
            ForYouScreen(
                isSyncing = false,
                onboardingUiState = OnboardingUiState.NotShown,
                feedState = NewsFeedUiState.Success(
                    feed = previewUserNewsResources
                ),
                onTopicCheckedChanged = { _, _ -> },
                saveFollowedTopics = {},
                onNewsResourcesCheckedChanged = { _, _ -> }
            )
        }

        // TODO:
        //   1. セマンティックツリーの構造をログ出力メソッドを使って確認する
        //   2. `Android Basics with Compose`というテキストを含むノードを探し、クリック可能であることを検証する
        //   3. `performScrollToNode()`を使って、
        //        `Thanks for helping us reach 1M YouTube Subscribers`というテキストを含むノードが画面に表示されるまでスクロールする
        //   4. `Thanks for helping us reach 1M YouTube Subscribers`というテキストを含むノードが画面上に表示されていることと、クリック可能であることを検証する
    }

    @Test
    fun circularProgressIndicator_whenScreenIsSyncing_exists() {
        composeTestRule.setContent {
            BoxWithConstraints {
                ForYouScreen(
                    isSyncing = true,
                    onboardingUiState = OnboardingUiState.NotShown,
                    feedState = NewsFeedUiState.Success(previewUserNewsResources),
                    onTopicCheckedChanged = { _, _ -> },
                    saveFollowedTopics = {},
                    onNewsResourcesCheckedChanged = { _, _ -> }
                )
            }
        }

        composeTestRule.onRoot().printToLog("Log")

        composeTestRule
            .onNodeWithContentDescription(
                composeTestRule.activity.resources.getString(R.string.for_you_loading)
            )
            .assertExists()
    }

    @Test
    fun topicHeadlines_whenNoSelected_checkedOff() {
        composeTestRule.setContent {
            BoxWithConstraints {
                ForYouScreen(
                    isSyncing = false,
                    onboardingUiState =
                    OnboardingUiState.Shown(
                        topics = testTopics,
                    ),
                    feedState = NewsFeedUiState.Success(
                        feed = emptyList()
                    ),
                    onTopicCheckedChanged = { _, _ -> },
                    saveFollowedTopics = {},
                    onNewsResourcesCheckedChanged = { _, _ -> }
                )
            }
        }

        composeTestRule
            .onNode(hasParent(hasText("Headlines")))
            .assertIsOff()
    }

    @Test
    fun topicSelector_whenSomeTopicsSelected_showsTopicChipsAndEnabledDoneButton() {
        composeTestRule.setContent {
            BoxWithConstraints {
                ForYouScreen(
                    isSyncing = false,
                    onboardingUiState =
                    OnboardingUiState.Shown(
                        // Follow one topic
                        topics = testTopics.mapIndexed { index, testTopic ->
                            testTopic.copy(isFollowed = index == 1)
                        }
                    ),
                    feedState = NewsFeedUiState.Success(
                        feed = emptyList()
                    ),
                    onTopicCheckedChanged = { _, _ -> },
                    saveFollowedTopics = {},
                    onNewsResourcesCheckedChanged = { _, _ -> }
                )
            }
        }

        testTopics.forEach { testTopic ->
            composeTestRule
                .onNodeWithText(testTopic.topic.name)
                .assertExists()
                .assertHasClickAction()
        }

        // Scroll until the Done button is visible
        composeTestRule
            .onAllNodes(hasScrollToNodeAction())
            .onFirst()
            .performScrollToNode(doneButtonMatcher)

        composeTestRule
            .onNode(doneButtonMatcher)
            .assertExists()
            .assertIsEnabled()
            .assertHasClickAction()
    }

    @Test
    fun feed_whenInterestsSelectedAndLoading_showsLoadingIndicator() {
        composeTestRule.setContent {
            BoxWithConstraints {
                ForYouScreen(
                    isSyncing = false,
                    onboardingUiState =
                    OnboardingUiState.Shown(topics = testTopics),
                    feedState = NewsFeedUiState.Loading,
                    onTopicCheckedChanged = { _, _ -> },
                    saveFollowedTopics = {},
                    onNewsResourcesCheckedChanged = { _, _ -> }
                )
            }
        }

        composeTestRule
            .onNodeWithContentDescription(
                composeTestRule.activity.resources.getString(R.string.for_you_loading)
            )
            .assertExists()
    }

    @Test
    fun feed_whenNoInterestsSelectionAndLoading_showsLoadingIndicator() {
        composeTestRule.setContent {
            BoxWithConstraints {
                ForYouScreen(
                    isSyncing = false,
                    onboardingUiState = OnboardingUiState.NotShown,
                    feedState = NewsFeedUiState.Loading,
                    onTopicCheckedChanged = { _, _ -> },
                    saveFollowedTopics = {},
                    onNewsResourcesCheckedChanged = { _, _ -> }
                )
            }
        }

        composeTestRule
            .onNodeWithContentDescription(
                composeTestRule.activity.resources.getString(R.string.for_you_loading)
            )
            .assertExists()
    }
}

private val testTopic = Topic(
    id = "",
    name = "",
    shortDescription = "",
    longDescription = "",
    url = "",
    imageUrl = ""
)
private val testTopics = listOf(
    FollowableTopic(
        topic = testTopic.copy(id = "0", name = "Headlines"),
        isFollowed = false
    ),
    FollowableTopic(
        topic = testTopic.copy(id = "1", name = "UI"),
        isFollowed = false
    ),
    FollowableTopic(
        topic = testTopic.copy(id = "2", name = "Tools"),
        isFollowed = false
    ),
)
