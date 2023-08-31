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

package com.google.samples.apps.nowinandroid.core.data.testdoubles

import com.google.samples.apps.nowinandroid.core.network.NiaNetworkDataSource
import com.google.samples.apps.nowinandroid.core.network.model.NetworkChangeList
import com.google.samples.apps.nowinandroid.core.network.model.NetworkNewsResource
import com.google.samples.apps.nowinandroid.core.network.model.NetworkTopic
import com.google.samples.apps.nowinandroid.core.network.model.NetworkVideoNewsResource
import okhttp3.ResponseBody

class TestNetworkDataSource(
    var getTopicFunc: (suspend (ids: List<String>?) -> List<NetworkTopic>)? = null,
    var getNewsResourcesFunc: (suspend (ids: List<String>?) -> List<NetworkNewsResource>)? = null,
    var getTopicChangeListFunc: (suspend (after: Int?) -> List<NetworkChangeList>)? = null,
    var getNewsResourceChangeListFunc: (suspend (after: Int?) -> List<NetworkChangeList>)? = null,
    var getVideoNewsResourcesFunc: (suspend () -> List<NetworkVideoNewsResource>)? = null,
    var downloadVideoResourceFunc: (suspend () -> ResponseBody)? = null
) : NiaNetworkDataSource {

    override suspend fun getTopics(ids: List<String>?): List<NetworkTopic> =
        requireNotNull(getTopicFunc) { "not set getTopic(ids: List<String>?)" }
            .invoke(ids)

    override suspend fun getNewsResources(ids: List<String>?): List<NetworkNewsResource> =
        requireNotNull(getNewsResourcesFunc) { "not set getNewsResources(ids: List<String>?)" }
            .invoke(ids)

    override suspend fun getTopicChangeList(after: Int?): List<NetworkChangeList> =
        requireNotNull(getTopicChangeListFunc) { "not set getTopicChangeList(after: Int?)" }
            .invoke(after)

    override suspend fun getNewsResourceChangeList(after: Int?): List<NetworkChangeList> =
        requireNotNull(getNewsResourceChangeListFunc) { "not set getNewsResourceChangeList(after: Int?)" }
            .invoke(after)

    override suspend fun getVideoNewsResources(): List<NetworkVideoNewsResource> =
        requireNotNull(getVideoNewsResourcesFunc) { "not set getVideoNewsResources()" }
            .invoke()

    override suspend fun downloadVideoResource(url: String): ResponseBody =
        requireNotNull(downloadVideoResourceFunc) { "not set downloadVideoResource()" }
            .invoke()
}