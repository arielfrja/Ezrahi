package com.arielfaridja.ezrahi.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "permissions")
class ActPermission(
    @PrimaryKey var id: Int,
    var name: String?,
    var showRoute: Boolean?,
    var addUser: Boolean?,
    var removeUser: Boolean?,
    var canReport: Boolean?,
    var rolesToHide: ArrayList<Int>?,
    var reportsToHide: ArrayList<Int>?
) {
    var ActId: String? = null

    companion object {
        @JvmStatic
        fun mapToPermission(permissions: Map<String, Any?>, level: Int): ActPermission {
            return ActPermission(
                level,
                permissions["Name"] as? String,
                permissions["ShowRoute"] as? Boolean,
                permissions["AddUser"] as? Boolean,
                permissions["RemoveUser"] as? Boolean,
                permissions["CanReport"] as? Boolean,
                permissions["RolesToHide"] as? ArrayList<Int>,
                permissions["ReportsToHide"] as? ArrayList<Int>
            )
        }
    }
}
