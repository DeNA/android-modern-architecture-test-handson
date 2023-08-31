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

import com.google.common.truth.Truth
import com.google.samples.apps.nowinandroid.core.data.error.ApiException
import com.google.samples.apps.nowinandroid.core.data.repository.TopicsRepository
import com.google.samples.apps.nowinandroid.core.data.repository.UserDataRepository
import com.google.samples.apps.nowinandroid.core.datastore.userPreferences
import com.google.samples.apps.nowinandroid.core.domain.GetFollowableTopicsUseCase
import com.google.samples.apps.nowinandroid.core.domain.model.FollowableTopic
import com.google.samples.apps.nowinandroid.core.model.data.Topic
import com.google.samples.apps.nowinandroid.core.model.data.UserData
import com.google.samples.apps.nowinandroid.core.model.data.VideoNewsResource
import com.google.samples.apps.nowinandroid.core.testing.repository.TestTopicsRepository
import com.google.samples.apps.nowinandroid.core.testing.repository.TestUserDataRepository
import com.google.samples.apps.nowinandroid.core.testing.repository.TestVideoNewsRepository
import com.google.samples.apps.nowinandroid.core.testing.repository.createTestUserRepository
import com.google.samples.apps.nowinandroid.core.testing.repository.emptyUserData
import com.google.samples.apps.nowinandroid.feature.foryou.VideoNewsViewModel.VideoNewsUiState
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toCollection
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.time.ZoneId
import java.time.ZonedDateTime

@Suppress("NonAsciiCharacters", "RemoveRedundantBackticks")
class VideoNewsViewModelTest {

    val testTopicRepository = mockk<TopicsRepository>() {
        every {
            getTopics()
        } returns flow {
            emit(listOf(sampleTopic1, sampleTopic2))
        }
    }

    // 練習1: ViewModelからのsuspend関数呼び出しをテストする
    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `suspend関数呼び出しをテストする`() {

        val testUserDataRepository = mockk<UserDataRepository>(relaxed = true)

        val getFollowableTopicsUseCase = GetFollowableTopicsUseCase(
            topicsRepository = TestTopicsRepository(),
            userDataRepository = testUserDataRepository
        )

        val viewModel = VideoNewsViewModel(
            testUserDataRepository,
            TestVideoNewsRepository(),
            getFollowableTopicsUseCase
        )

        viewModel.updateFollowedTopic("ID", true)

        coVerify {
            testUserDataRepository.toggleFollowedTopicId("ID", true)
        }
    }

    // 練習2: データ取得の結果、UI Stateが更新されることをテストする
    @Test
    fun `fetchVideoNewsResources_データ取得の結果、UI Stateが更新されることをテストする`() {

        val testUserDataRepository = TestUserDataRepository()

        val testVideoNewsRepository = TestVideoNewsRepository(
            getVideoNewsResourcesFunc = {
                listOf(testVideoNewsResource, testVideoNewsResource2)
            })

        val getFollowableTopicsUseCase = GetFollowableTopicsUseCase(
            topicsRepository = TestTopicsRepository(),
            userDataRepository = testUserDataRepository
        )

        val viewModel = VideoNewsViewModel(
            testUserDataRepository,
            testVideoNewsRepository,
            getFollowableTopicsUseCase,
        )

        viewModel.fetchVideoNewsResources()

        val expectedVideoNewsResources = listOf(testVideoNewsResource, testVideoNewsResource2)

        Truth.assertThat(viewModel.videoNewsUiState.value).isEqualTo(
            VideoNewsUiState(
                isLoading = false,
                videoNewsResources = expectedVideoNewsResources,
                errorMessage = null
            )
        )
    }

    // 練習3: 通信中のローディングをテストする
    @Test
    fun `fetchVideoNewsResources_通信中のローディングをテストする`() = runTest {

        val testUserDataRepository = TestUserDataRepository()

        val testVideoNewsRepository = TestVideoNewsRepository(
            getVideoNewsResourcesFunc = {
                delay(10)
                listOf(testVideoNewsResource, testVideoNewsResource2)
            })

        val getFollowableTopicsUseCase = GetFollowableTopicsUseCase(
            topicsRepository = TestTopicsRepository(),
            userDataRepository = testUserDataRepository
        )

        val viewModel = VideoNewsViewModel(
            testUserDataRepository,
            testVideoNewsRepository,
            getFollowableTopicsUseCase,
        )

        viewModel.fetchVideoNewsResources()

        Truth.assertThat(viewModel.videoNewsUiState.value.isLoading).isEqualTo(true)

        advanceUntilIdle()

        Truth.assertThat(viewModel.videoNewsUiState.value).isEqualTo(
            VideoNewsUiState(
                isLoading = false,
                videoNewsResources = listOf(testVideoNewsResource, testVideoNewsResource2),
                errorMessage = null
            )
        )
    }

    // 練習4: ViewModel内のエラーハンドリングをテストする
    @Test
    fun `fetchVideoNewsResources_ViewModel内のエラーハンドリングをテストする`() = runTest {

        val testUserDataRepository = TestUserDataRepository()

        val testVideoNewsRepository = TestVideoNewsRepository(
            getVideoNewsResourcesFunc = {
                throw ApiException("エラー発生")
            })

        val getFollowableTopicsUseCase = GetFollowableTopicsUseCase(
            topicsRepository = TestTopicsRepository(),
            userDataRepository = testUserDataRepository
        )

        val viewModel = VideoNewsViewModel(
            testUserDataRepository,
            testVideoNewsRepository,
            getFollowableTopicsUseCase,
        )

        viewModel.fetchVideoNewsResources()

        Truth.assertThat(viewModel.videoNewsUiState.value).isEqualTo(
            VideoNewsUiState(
                isLoading = false,
                videoNewsResources = emptyList(),
                errorMessage = "エラー発生"
            )
        )
    }

    // 練習5: データ更新の結果、Flowの更新を受け取ってUI Stateが変更される実装をテストする

    @Test
    fun `データ更新の結果、Flowの更新を受け取ってUI Stateが変更される実装をテストする`() = runTest {

        // 初期状態のユーザーをFakeのリポジトリにセットしておく
        // 初期状態のユーザーはshouldHideOnboardingがfalse
        val testUserDataRepository = TestUserDataRepository()
        testUserDataRepository.setUserData(emptyUserData)

        val getFollowableTopicsUseCase = GetFollowableTopicsUseCase(
            topicsRepository = testTopicRepository,
            userDataRepository = testUserDataRepository
        )

        val viewModel = VideoNewsViewModel(
            testUserDataRepository,
            TestVideoNewsRepository(),
            getFollowableTopicsUseCase,
        )

        // 初期状態
        Truth.assertThat(viewModel.onBoardingUiState.value).isEqualTo(OnboardingUiState.Loading)

        // onBoardingUiStateはSharingStarted.WhileSubscribedでホット化されているため、
        // collectをして変更が流れるようにする
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.onBoardingUiState.collect()
        }

        // OnboardingUiState.Shownに変更される
        Truth.assertThat(viewModel.onBoardingUiState.value)
            .isEqualTo(
                OnboardingUiState.Shown(
                    listOf(
                        FollowableTopic(sampleTopic1, false),
                        FollowableTopic(sampleTopic2, false)
                    )
                )
            )

        //　ユーザーのインタラクションでチュートリアルを非表示にする
        viewModel.dismissOnBoarding()

        // OnboardingUiState.NotShownに変更される
        Truth.assertThat(viewModel.onBoardingUiState.value).isEqualTo(OnboardingUiState.NotShown)
    }

    @Test
    fun `onBoardingUiState_Fake以外のテストダブルを利用する`() = runTest {

        val userFlow = MutableSharedFlow<UserData>(replay = 1)
        val testUserDataRepository = mockk<UserDataRepository>(relaxed = true) {
            every {
                userData
            } returns userFlow
        }
        userFlow.emit(emptyUserData)

        val getFollowableTopicsUseCase = GetFollowableTopicsUseCase(
            topicsRepository = testTopicRepository,
            userDataRepository = testUserDataRepository
        )

        val viewModel = VideoNewsViewModel(
            testUserDataRepository,
            TestVideoNewsRepository(),
            getFollowableTopicsUseCase,
        )

        Truth.assertThat(viewModel.onBoardingUiState.value).isEqualTo(OnboardingUiState.Loading)

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.onBoardingUiState.collect()
        }

        Truth.assertThat(viewModel.onBoardingUiState.value)
            .isEqualTo(
                OnboardingUiState.Shown(
                    listOf(
                        FollowableTopic(sampleTopic1, false),
                        FollowableTopic(sampleTopic2, false)
                    )
                )
            )

        viewModel.dismissOnBoarding()

        coVerify {
            testUserDataRepository.setShouldHideOnboarding(true)
        }

        userFlow.emit(emptyUserData.copy(shouldHideOnboarding = true))

        Truth.assertThat(viewModel.onBoardingUiState.value).isEqualTo(OnboardingUiState.NotShown)
    }

    @get:Rule
    val tmpFolder: TemporaryFolder = TemporaryFolder.builder().assureDeletion().build()

    @Test
    fun `onBoardingUiState_ViewModelとデータレイヤーの実装を結合する`() = runTest {

        // DataStoreに初期状態のユーザーを登録する
        val userPreferences = userPreferences {
            shouldHideOnboarding = false
        }
        val testUserDataRepository = createTestUserRepository(
            tmpFolder.newFile("user.pb"),
            coroutineScope = this + UnconfinedTestDispatcher(testScheduler),
            initialData = userPreferences
        )

        val getFollowableTopicsUseCase = GetFollowableTopicsUseCase(
            topicsRepository = testTopicRepository,
            userDataRepository = testUserDataRepository
        )

        val viewModel = VideoNewsViewModel(
            testUserDataRepository,
            TestVideoNewsRepository(),
            getFollowableTopicsUseCase,
        )

        // 初期状態
        Truth.assertThat(viewModel.onBoardingUiState.value).isEqualTo(OnboardingUiState.Loading)

        // onBoardingUiStateはSharingStarted.WhileSubscribedでホット化されているため、
        // collectをして変更が流れるようにする
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.onBoardingUiState.collect()
        }

        // OnboardingUiState.Shownに変更される
        Truth.assertThat(viewModel.onBoardingUiState.value)
            .isEqualTo(
                OnboardingUiState.Shown(
                    listOf(
                        FollowableTopic(sampleTopic1, false),
                        FollowableTopic(sampleTopic2, false)
                    )
                )
            )

        //　ユーザーのインタラクションでチュートリアルを非表示にする
        viewModel.dismissOnBoarding()

        // OnboardingUiState.NotShownに変更される
        Truth.assertThat(viewModel.onBoardingUiState.value).isEqualTo(OnboardingUiState.NotShown)
    }

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

    private val testVideoNewsResource2 = VideoNewsResource(
        id = "id2",
        title = "title2",
        content = "content2",
        headerImageUrl = "https://host/header2.png",
        previewMovieUrl = "https://host/full_movie2.mp4",
        fullMovieUrl = "https://host/full_movie2.mp4",
        publishDate = ZonedDateTime.of(2022, 12, 31, 10, 15, 30, 0, ZoneId.of("+09:00")),
        topics = listOf(Topic(id = "id2"))
    )

    private val sampleTopic1 =
        Topic(
            id = "0",
            name = "Headlines",
            shortDescription = "",
            longDescription = "long description",
            url = "URL",
            imageUrl = "image URL",
        )
    private val sampleTopic2 = Topic(
        id = "1",
        name = "UI",
        shortDescription = "",
        longDescription = "long description",
        url = "URL",
        imageUrl = "image URL",
    )
}