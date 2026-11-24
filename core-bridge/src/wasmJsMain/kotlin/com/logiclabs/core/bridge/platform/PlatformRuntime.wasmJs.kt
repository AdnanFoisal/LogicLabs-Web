package com.logiclabs.core.bridge.platform

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

private fun jsDateNow(): Double = js("Date.now()")

actual fun currentTimeMillis(): Long = jsDateNow().toLong()

actual val ioDispatcher: CoroutineDispatcher = Dispatchers.Default
