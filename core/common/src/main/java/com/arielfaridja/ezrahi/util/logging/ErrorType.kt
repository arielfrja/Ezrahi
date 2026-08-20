package com.arielfaridja.ezrahi.util.logging

enum class ErrorType(val raw: String) {
    CRASH("CRASH"),
    CAUGHT("CAUGHT"),
    ROUTE_PARSER("ROUTE_PARSER"),
    NETWORK("NETWORK"),
    FIRESTORE_LISTENER("FIRESTORE_LISTENER"),
    AUTH("AUTH"),
    LOCATION_SERVICE("LOCATION_SERVICE"),
    UI_COMPOSE("UI_COMPOSE"),
    UNKNOWN("UNKNOWN")
}

enum class Severity(val raw: String) {
    FATAL("FATAL"),
    ERROR("ERROR"),
    WARNING("WARNING")
}