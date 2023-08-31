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

package com.google.samples.apps.nowinandroid.core.data.repository

import com.google.samples.apps.nowinandroid.core.data.model.asModel
import com.google.samples.apps.nowinandroid.core.model.data.VideoNewsResource
import com.google.samples.apps.nowinandroid.core.network.NiaNetworkDataSource
import com.google.samples.apps.nowinandroid.core.network.model.NetworkVideoNewsResource

class OnMemoryCacheVideoNewsRepository(
    private val networkDataSource: NiaNetworkDataSource,
    private val currentTimeMillsProvider: () -> Long = { System.currentTimeMillis() }
) : VideoNewsRepository {

    private var videoNewsCache = emptyList<VideoNewsResource>()
    private var cacheCreatedAt: Long = 0L

    override suspend fun getVideoNewsResources(): List<VideoNewsResource> {

        if (videoNewsCache.isNotEmpty() && isCacheExpired().not()) {
            return this.videoNewsCache
        }

        val videoNewsResource = networkDataSource
            .getVideoNewsResources()
            .map(NetworkVideoNewsResource::asModel)

        this.videoNewsCache = videoNewsResource
        this.cacheCreatedAt = currentTimeMillsProvider()
        return videoNewsResource
    }

    override suspend fun downloadVideo(url: String) {
        TODO("ハンズオンでは使用しないため未実装")
    }

    private fun isCacheExpired(): Boolean {
        return currentTimeMillsProvider() - cacheCreatedAt > CACHE_EXPIRED_MILLS
    }

    companion object {
        const val CACHE_EXPIRED_MILLS = 1000 * 60 * 10
    }
}