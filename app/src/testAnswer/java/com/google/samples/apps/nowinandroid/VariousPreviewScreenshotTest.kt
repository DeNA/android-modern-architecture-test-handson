package com.google.samples.apps.nowinandroid

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.filter
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.unit.Density
import androidx.test.core.app.ApplicationProvider
import com.airbnb.android.showkase.models.Showkase
import com.airbnb.android.showkase.models.ShowkaseBrowserComponent
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import com.github.takahirom.roborazzi.captureScreenRoboImage
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowDisplay
import kotlin.math.roundToInt

@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel7)
class VariousPreviewScreenshotTest(
    private val testCase: TestCase
) {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()
    lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun testDefault() {
        val filePath = "${testCase.showkaseBrowserComponent.componentKey}.png"
        capturePreview(filePath = filePath)
    }

    @Test
    fun testFont2x() {
        val filePath = "${testCase.showkaseBrowserComponent.componentKey}_font2x.png"
        capturePreview(filePath = filePath, fontMagnification = 2f)
    }

    @OptIn(ExperimentalRoborazziApi::class)
    private fun capturePreview(filePath: String, fontMagnification: Float = 1f) {
        setGroupParameterToQualifiers(group = testCase.showkaseBrowserComponent.group)
        val widthDp = testCase.showkaseBrowserComponent.widthDp
        val heightDp = testCase.showkaseBrowserComponent.heightDp
        if (widthDp != null || heightDp != null) {
            setDisplaySize(widthDp, heightDp)
        }
        // Activityを再生成して変更した画面サイズを反映させる
        composeTestRule.activityRule.scenario.recreate()

        composeTestRule.setContent {
            val density = LocalDensity.current
            val customDensity = Density(
                fontScale = density.fontScale * fontMagnification,
                density = density.density,
            )
            CompositionLocalProvider(
                LocalInspectionMode provides true,
                LocalDensity provides customDensity,
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

    private fun setGroupParameterToQualifiers(group: String) {
        if (group != "Default Group") {
            val tags = testCase.showkaseBrowserComponent.group.split("-")
            tags.forEach { tag ->
                RuntimeEnvironment.setQualifiers("+$tag")
            }
        }
    }

    private fun setDisplaySize(widthDp: Int?, heightDp: Int?) {
        val display = ShadowDisplay.getDefaultDisplay()
        val density = context.resources.displayMetrics.density
        widthDp?.let {
            val widthPx = (widthDp * density).roundToInt()
            Shadows.shadowOf(display).setWidth(widthPx)
        }
        heightDp?.let {
            val heightPx = (heightDp * density).roundToInt()
            Shadows.shadowOf(display).setHeight(heightPx)
        }
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
