/*
 * Copyright 2021 The Android Open Source Project
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

package com.google.samples.apps.nowinandroid.feature.interests

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.samples.apps.nowinandroid.core.designsystem.component.NiaBackground
import com.google.samples.apps.nowinandroid.core.designsystem.component.NiaLoadingWheel
import com.google.samples.apps.nowinandroid.core.designsystem.theme.NiaTheme
import com.google.samples.apps.nowinandroid.core.domain.model.FollowableTopic
import com.google.samples.apps.nowinandroid.core.model.data.Topic
import com.google.samples.apps.nowinandroid.core.ui.DevicePreviews

@Composable
internal fun InterestsRoute(
    navigateToTopic: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: InterestsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    InterestsScreen(
        uiState = uiState,
        followTopic = viewModel::followTopic,
        navigateToTopic = navigateToTopic,
        modifier = modifier
    )
}

@Composable
internal fun InterestsScreen(
    uiState: InterestsUiState,
    followTopic: (String, Boolean) -> Unit,
    navigateToTopic: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        when (uiState) {
            InterestsUiState.Loading ->
                NiaLoadingWheel(
                    modifier = modifier,
                    contentDesc = stringResource(id = R.string.loading),
                )
            is InterestsUiState.Interests ->
                TopicsTabContent(
                    topics = uiState.topics,
                    onTopicClick = navigateToTopic,
                    onFollowButtonClick = followTopic,
                    modifier = modifier,
                )
            is InterestsUiState.Empty -> InterestsEmptyScreen()
        }
    }
}

@Composable
private fun InterestsEmptyScreen() {
    Text(text = stringResource(id = R.string.empty_header))
}

@DevicePreviews
@Composable
fun InterestsScreenPopulated() {
    val context = LocalContext.current
    NiaTheme {
        NiaBackground {
            InterestsScreen(
                uiState = InterestsUiState.Interests(
                    topics = getPreviewTopics(context).map { FollowableTopic(it, false) }
                ),
                followTopic = { _, _ -> },
                navigateToTopic = {},
            )
        }
    }
}

@DevicePreviews
@Composable
private fun InterestsScreenLoading() {
    NiaTheme {
        NiaBackground {
            InterestsScreen(
                uiState = InterestsUiState.Loading,
                followTopic = { _, _ -> },
                navigateToTopic = {},
            )
        }
    }
}

@DevicePreviews
@Composable
private fun InterestsScreenEmpty() {
    NiaTheme {
        NiaBackground {
            InterestsScreen(
                uiState = InterestsUiState.Empty,
                followTopic = { _, _ -> },
                navigateToTopic = {},
            )
        }
    }
}

fun getPreviewTopics(context: Context): List<Topic> {
    return listOf(
        Topic(
            id = "2",
            name = context.getString(R.string.headlines),
            shortDescription = context.getString(R.string.headlines_short_description),
            longDescription = context.getString(R.string.headlines_long_description),
            imageUrl = "https://firebasestorage.googleapis.com/v0/b/now-in-android.appspot.com/o/img%2Fic_topic_Headlines.svg?alt=media&token=506faab0-617a-4668-9e63-4a2fb996603f",
            url = ""
        ),
        Topic(
            id = "3",
            name = context.getString(R.string.ui),
            shortDescription = context.getString(R.string.ui_short_description),
            longDescription = context.getString(R.string.ui_long_description),
            imageUrl = "https://firebasestorage.googleapis.com/v0/b/now-in-android.appspot.com/o/img%2Fic_topic_UI.svg?alt=media&token=0ee1842b-12e8-435f-87ba-a5bb02c47594",
            url = ""
        ),
        Topic(
            id = "4",
            name = context.getString(R.string.testing),
            shortDescription = context.getString(R.string.testing_short_description),
            longDescription = context.getString(R.string.testing_long_description),
            imageUrl = "https://firebasestorage.googleapis.com/v0/b/now-in-android.appspot.com/o/img%2Fic_topic_Testing.svg?alt=media&token=a11533c4-7cc8-4b11-91a3-806158ebf428",
            url = ""
        ),
    )
}