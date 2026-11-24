package com.logiclabs.core.bridge.platform

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

actual fun currentTimeMillis(): Long = js("Date.now()").unsafeCast<Double>().toLong()

actual val ioDispatcher: CoroutineDispatcher = Dispatchers.Default
