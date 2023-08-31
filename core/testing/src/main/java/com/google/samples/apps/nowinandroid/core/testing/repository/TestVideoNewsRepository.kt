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

package com.google.samples.apps.nowinandroid.core.testing.repository

import com.google.samples.apps.nowinandroid.core.data.repository.VideoNewsRepository
import com.google.samples.apps.nowinandroid.core.model.data.VideoNewsResource

class TestVideoNewsRepository(
    var getVideoNewsResourcesFunc: (suspend () -> List<VideoNewsResource>)? = null,
    var downloadVideoFunc: (suspend (url: String) -> Unit)? = null,

    ) : VideoNewsRepository {
    override suspend fun getVideoNewsResources(): List<VideoNewsResource> =
        requireNotNull(getVideoNewsResourcesFunc?.invoke()) {
            "not set getVideoNewsResources()"
        }

    override suspend fun downloadVideo(url: String) =
        requireNotNull(downloadVideoFunc?.invoke(url)) {
            "not set downloadVideo(url: String)"
        }
}