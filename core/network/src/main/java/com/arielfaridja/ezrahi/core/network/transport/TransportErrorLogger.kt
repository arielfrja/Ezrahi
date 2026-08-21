package com.arielfaridja.ezrahi.core.network.transport

fun interface TransportErrorLogger {
    fun logError(error: Throwable, eventId: String?, screen: String?)
}