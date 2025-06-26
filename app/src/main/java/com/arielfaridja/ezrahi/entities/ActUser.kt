package com.arielfaridja.ezrahi.entities

import androidx.annotation.NonNull
import androidx.room.Entity

@Entity(primaryKeys = ["id"], tableName = "Users")
open class ActUser : User {
    var actId: String = ""
    var role: Int = 0

    constructor(id: String, phone: String, email: String, firstName: String, lastName: String, location: Latlng, actId: String, role: Int)
        : super(id, firstName, lastName, phone, email, location) {
        this.actId = actId
        this.role = role
    }

    constructor(actId: String, u: User, role: Int) : super(u) {
        this.actId = actId
        this.role = role
    }

    constructor(actId: String, userId: String, role: Int) : super() {
        this.id = userId
        this.actId = actId
        this.role = role
    }

    fun setUser(u: User) {
        this.id = u.id
        this.email = u.email
        this.firstName = u.firstName
        this.lastName = u.lastName
        this.location = u.location
        this.phone = u.phone
    }
}
