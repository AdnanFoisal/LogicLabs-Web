package com.logiclabs.core.bridge.platform

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

actual fun currentTimeMillis(): Long = System.currentTimeMillis()

actual val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
