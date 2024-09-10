package com.google.samples.apps.nowinandroid

import android.content.Context
import android.content.res.Configuration
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.Density
import androidx.test.core.app.ApplicationProvider
import com.github.takahirom.roborazzi.ComposePreviewTester
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.captureRoboImage
import com.google.samples.apps.nowinandroid.core.ui.DelayedPreview
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows
import org.robolectric.shadows.ShadowDisplay
import sergio.sastre.composable.preview.scanner.android.AndroidComposablePreviewScanner
import sergio.sastre.composable.preview.scanner.android.AndroidPreviewInfo
import sergio.sastre.composable.preview.scanner.android.screenshotid.AndroidPreviewScreenshotIdBuilder
import sergio.sastre.composable.preview.scanner.core.preview.ComposablePreview
import sergio.sastre.composable.preview.scanner.core.preview.getAnnotation
import kotlin.math.roundToInt

@OptIn(ExperimentalRoborazziApi::class)
class MyComposePreviewTester : ComposePreviewTester<AndroidPreviewInfo> {
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()
    override fun options(): ComposePreviewTester.Options {
        val testLifecycleOptions = ComposePreviewTester.Options.JUnit4TestLifecycleOptions(
            testRuleFactory = { composeTestRule }
        )
        return super.options().copy(testLifecycleOptions = testLifecycleOptions)
    }

    override fun previews(): List<ComposablePreview<AndroidPreviewInfo>> {
        val options = options()
        return AndroidComposablePreviewScanner()
            .scanPackageTrees(*options.scanOptions.packages.toTypedArray())
            .includeAnnotationInfoForAllOf(DelayedPreview::class.java)
            .getPreviews()
    }

    override fun test(preview: ComposablePreview<AndroidPreviewInfo>) {
        val delay = preview.getAnnotation<DelayedPreview>()?.delay ?: 0L
        val previewScannerFileName =
            AndroidPreviewScreenshotIdBuilder(preview).build()
        val fileName =
            if (delay == 0L) previewScannerFileName else "${previewScannerFileName}_delay$delay"
        val filePath = "$fileName.png"
        preview.myApplyToRobolectricConfiguration()
        composeTestRule.activityRule.scenario.recreate()
        composeTestRule.apply {
            try {
                if (delay != 0L) {
                    mainClock.autoAdvance = false
                }
                setContent {
                    ApplyToCompositionLocal(preview) {
                        preview()
                    }
                }
                if (delay != 0L) {
                    mainClock.advanceTimeBy(delay)
                }
                onRoot().captureRoboImage(filePath = filePath)
            } finally {
                mainClock.autoAdvance = true
            }
        }
    }
}

@Composable
fun ApplyToCompositionLocal(
    preview: ComposablePreview<AndroidPreviewInfo>,
    content: @Composable () -> Unit
) {
    val fontScale = preview.previewInfo.fontScale
    val density = LocalDensity.current
    val customDensity =
        Density(density = density.density, fontScale = density.fontScale * fontScale)
    CompositionLocalProvider(LocalDensity provides customDensity) {
        content()
    }

}


fun ComposablePreview<AndroidPreviewInfo>.myApplyToRobolectricConfiguration() {
    val preview = this
    // ナイトモード
    when (preview.previewInfo.uiMode and Configuration.UI_MODE_NIGHT_MASK) {
        Configuration.UI_MODE_NIGHT_YES -> RuntimeEnvironment.setQualifiers("+night")
        Configuration.UI_MODE_NIGHT_NO -> RuntimeEnvironment.setQualifiers("+notnight")
        else -> { /* do nothing */
        }
    }

    // 画面サイズ
    if (preview.previewInfo.widthDp != -1 && preview.previewInfo.heightDp != -1) {
        setDisplaySize(preview.previewInfo.widthDp, preview.previewInfo.heightDp)
    }
}

private fun setDisplaySize(widthDp: Int, heightDp: Int) {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val display = ShadowDisplay.getDefaultDisplay()
    val density = context.resources.displayMetrics.density
    widthDp.let {
        val widthPx = (widthDp * density).roundToInt()
        Shadows.shadowOf(display).setWidth(widthPx)
    }
    heightDp.let {
        val heightPx = (heightDp * density).roundToInt()
        Shadows.shadowOf(display).setHeight(heightPx)
    }
}