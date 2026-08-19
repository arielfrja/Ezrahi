package com.arielfaridja.ezrahi.app.util

import com.arielfaridja.ezrahi.domain.model.RoleOption
import com.arielfaridja.ezrahi.domain.model.UserRole

fun defaultRoleOptions(): List<RoleOption> = UserRole.entries.map { role ->
    RoleOption(
        name = role.name,
        label = when (role) {
            UserRole.MANAGER -> "Manager / מנהל פעילות"
            UserRole.LEAD_GUIDE -> "Lead Guide / מוביל"
            UserRole.MEDIC -> "Medic / חובש"
            UserRole.SWEEP_GUIDE -> "Sweep Guide / מאסף"
            UserRole.LOGISTICS -> "Logistics / לוגיסטיקה"
            UserRole.MEMBER -> "Participant / משתתף"
        },
        isStaff = role != UserRole.MEMBER
    )
}

fun roleLabel(role: UserRole): String = defaultRoleOptions()
    .firstOrNull { it.name == role.name }?.label ?: role.name

fun roleLabel(roleName: String): String = defaultRoleOptions()
    .firstOrNull { it.name == roleName }?.label ?: roleName