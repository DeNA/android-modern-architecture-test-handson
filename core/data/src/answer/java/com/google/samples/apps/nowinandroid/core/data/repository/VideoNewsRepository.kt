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

import com.google.samples.apps.nowinandroid.core.data.error.ApiException
import com.google.samples.apps.nowinandroid.core.data.model.asModel
import com.google.samples.apps.nowinandroid.core.model.data.VideoNewsResource
import com.google.samples.apps.nowinandroid.core.network.Dispatcher
import com.google.samples.apps.nowinandroid.core.network.NiaDispatchers.IO
import com.google.samples.apps.nowinandroid.core.network.NiaNetworkDataSource
import com.google.samples.apps.nowinandroid.core.network.model.NetworkVideoNewsResource
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import javax.inject.Inject

interface VideoNewsRepository {

    suspend fun getVideoNewsResources(): List<VideoNewsResource>

    suspend fun downloadVideo(url: String)
}

class DefaultVideoNewsRepository @Inject constructor(
    private val networkDataSource: NiaNetworkDataSource,
    @Dispatcher(IO) private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : VideoNewsRepository {

    private val _videoNewsStream = MutableSharedFlow<List<VideoNewsResource>>(replay = 1)
    val videoNewsStream = _videoNewsStream.asSharedFlow()

    private val downloadingUrlSet = mutableSetOf<String>()

    override suspend fun getVideoNewsResources(): List<VideoNewsResource> {

        return try {
            val videoNewsResource = networkDataSource
                .getVideoNewsResources()
                .map(NetworkVideoNewsResource::asModel)

            _videoNewsStream.emit(videoNewsResource)
            videoNewsResource
        } catch (e: HttpException) {
            throw ApiException("エラーレスポンスのメッセージ")
        }
    }

    override suspend fun downloadVideo(url: String) {
        if (downloadingUrlSet.contains(url)) {
            return
        }

        downloadingUrlSet.add(url)
        networkDataSource.downloadVideoResource(url)

        // 保存処理

        downloadingUrlSet.remove(url)
    }

    // downloadVideoのDispatcher差し替え版
    suspend fun downloadVideo2(url: String) = withContext(ioDispatcher) {
        if (downloadingUrlSet.contains(url)) {
            return@withContext
        }

        downloadingUrlSet.add(url)
        networkDataSource.downloadVideoResource(url)

        // 保存処理

        downloadingUrlSet.remove(url)
    }
}

