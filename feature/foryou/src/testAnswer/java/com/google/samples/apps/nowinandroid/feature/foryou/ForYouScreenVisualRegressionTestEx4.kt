package com.google.samples.apps.nowinandroid.feature.foryou

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import com.google.samples.apps.nowinandroid.core.domain.GetFollowableTopicsUseCase
import com.google.samples.apps.nowinandroid.core.domain.GetUserNewsResourcesUseCase
import com.google.samples.apps.nowinandroid.core.model.data.NewsResource
import com.google.samples.apps.nowinandroid.core.model.data.NewsResourceType
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
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@Suppress("NonAsciiCharacters", "TestFunctionName")
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel7)
class ForYouScreenVisualRegressionTestEx4 {
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
    fun Headlinesと書かれたトピックを選択するとDoneボタンがenabled状態になること() {
        composeTestRule.setContent {
            ForYouRoute(
                viewModel = viewModel
            )
        }
        // Content Descriptionに `Headlines` と書かれているコンポーネントをクリックする
        composeTestRule.onNodeWithContentDescription("Headlines").performClick()
        composeTestRule.onRoot().captureRoboImage()
    }
}

private val testTopics = listOf(
    Topic("1", "Headlines", "", "", "", "https://example.com/img/Headlines.svg"),
    Topic("2", "Android Studio", "", "", "", "https://example.com/img/Android-Studio.svg"),
    Topic("3", "Compose", "", "", "", "https://example.com/img/Compose.svg"),
)

private val testNewsResource = listOf(
    NewsResource(
        "100",
        "Pixel Watch",
        "",
        "",
        null,
        Instant.parse("2021-11-08T00:00:00.000Z"),
        NewsResourceType.Article,
        topics = listOf(Topic("1", "Headlines", "", "", "", ""))
    )
)