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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.samples.apps.nowinandroid.core.data.repository.UserDataRepository
import com.google.samples.apps.nowinandroid.core.data.repository.VideoNewsRepository
import com.google.samples.apps.nowinandroid.core.domain.GetFollowableTopicsUseCase
import com.google.samples.apps.nowinandroid.core.model.data.VideoNewsResource
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class VideoNewsViewModel @Inject constructor(
    private val userDataRepository: UserDataRepository,
    private val videoNewsRepository: VideoNewsRepository,
    getFollowableTopics: GetFollowableTopicsUseCase,
) : ViewModel() {

    fun updateFollowedTopic(topicId: String, isBookMarked: Boolean) {
        viewModelScope.launch {
            userDataRepository.toggleFollowedTopicId(topicId, isBookMarked)
        }
    }

    // 以下fetchVideoNewsResourcesのテストに使うコード

    private val _videoNewsUiState = MutableStateFlow(VideoNewsUiState())
    val videoNewsUiState: StateFlow<VideoNewsUiState> = _videoNewsUiState

    fun fetchVideoNewsResources() {
        viewModelScope.launch {
            _videoNewsUiState.update { it.copy(isLoading = true) }

            try {
                val videoNewsResources = videoNewsRepository.getVideoNewsResources()
                _videoNewsUiState.update {
                    it.copy(
                        videoNewsResources = videoNewsResources,
                        errorMessage = null
                    )
                }
            } catch (e: Exception) {
                _videoNewsUiState.update { it.copy(errorMessage = e.message) }
            }
            _videoNewsUiState.update { it.copy(isLoading = false) }
        }
    }

    // 以下fetchVideoNewsResourcesのテストに使うコード

    val onBoardingUiState: StateFlow<OnboardingUiState> =
        userDataRepository.userData
            .map {
                if (it.shouldHideOnboarding) {
                    OnboardingUiState.NotShown
                } else {
                    val followableTopics = getFollowableTopics().first()
                    OnboardingUiState.Shown(topics = followableTopics)
                }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = OnboardingUiState.Loading
            )

    fun dismissOnBoarding() {
        viewModelScope.launch {
            userDataRepository.setShouldHideOnboarding(true)
        }
    }

    data class VideoNewsUiState(
        val isLoading: Boolean = false,
        val videoNewsResources: List<VideoNewsResource> = emptyList(),
        val errorMessage: String? = null
    )
}
