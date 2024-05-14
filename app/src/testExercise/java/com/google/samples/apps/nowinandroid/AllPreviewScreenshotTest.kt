package com.google.samples.apps.nowinandroid

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.airbnb.android.showkase.models.Showkase
import com.airbnb.android.showkase.models.ShowkaseBrowserComponent
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
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
    private val showkaseBrowserComponent: ShowkaseBrowserComponent,
) {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun test() {
        composeTestRule.setContent {
            // showkaseBrowserComponent.component() がPreview指定されているComposable関数
            showkaseBrowserComponent.component()
        }
        // ここでスクリーンショットを取得する
        composeTestRule.onRoot().captureRoboImage()
    }

    companion object {
        @ParameterizedRobolectricTestRunner.Parameters
        @JvmStatic
        fun components(): Iterable<Array<*>> = Showkase.getMetadata().componentList.map { arrayOf(it) }
    }
}