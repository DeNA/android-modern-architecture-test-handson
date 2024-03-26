package com.google.samples.apps.nowinandroid

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.filter
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onFirst
import com.airbnb.android.showkase.models.Showkase
import com.airbnb.android.showkase.models.ShowkaseBrowserComponent
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import com.github.takahirom.roborazzi.captureScreenRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel7)
class AllPreviewScreenshotTest(
    private val testCase: TestCase
) {
    @get:Rule
    val composeTestRule = createComposeRule()

    @OptIn(ExperimentalRoborazziApi::class)
    @Test
    fun test() {
        val filePath = "${testCase.showkaseBrowserComponent.componentKey}.png"
        composeTestRule.setContent {
            CompositionLocalProvider(
                LocalInspectionMode provides true,
            ) {
                testCase.showkaseBrowserComponent.component()
            }
        }
        kotlin.runCatching {
            // 複数Windowがある場合、子コンポーネントがいる最初のものをとってくる
            composeTestRule.onAllNodes(isRoot())
                .filter(hasAnyChild())
                .onFirst()
                .assertExists() // 念のため存在していることをassertしている。assertに失敗したらnullを返し、captureScreenRoboImage()を使って全画面スクリーンショットを撮る
        }.getOrNull()?.captureRoboImage(filePath) ?: captureScreenRoboImage(filePath)
    }

    fun hasAnyChild(): SemanticsMatcher {
        return SemanticsMatcher("hasAnyChildThat") {
            it.children.isNotEmpty()
        }
    }

    companion object {
        class TestCase(
            val showkaseBrowserComponent: ShowkaseBrowserComponent
        ) {
            override fun toString() = showkaseBrowserComponent.componentKey
        }


        @ParameterizedRobolectricTestRunner.Parameters(name = "[{index}] {0}")
        @JvmStatic
        fun components(): Iterable<Array<*>> = Showkase.getMetadata().componentList.map {
            arrayOf(TestCase(it))
        }
    }
}
