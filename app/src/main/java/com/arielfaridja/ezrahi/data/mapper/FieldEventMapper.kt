package com.arielfaridja.ezrahi.data.mapper

import com.arielfaridja.ezrahi.domain.model.FieldEvent
import com.arielfaridja.ezrahi.entities.Activity

object FieldEventMapper {

    fun toModern(legacy: Activity): FieldEvent = FieldEvent(
        id = legacy.id,
        name = legacy.name,
        managerId = legacy.owner.id,
        managerContact = legacy.owner.phone,
        gpxRouteUrl = legacy.routesSrc.firstOrNull(),
        isLive = true
    )
}