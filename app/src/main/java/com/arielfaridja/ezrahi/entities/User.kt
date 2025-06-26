package com.arielfaridja.ezrahi.entities

import androidx.annotation.NonNull
import com.google.gson.Gson
import java.io.Serializable

open class User : Serializable {
    @NonNull
    var id: String = ""
    var firstName: String = ""
    var lastName: String = ""
    var email: String = ""
    var phone: String = ""
    var location: Latlng = Latlng()

    constructor()
    constructor(u: User) {
        this.id = u.id
        this.phone = u.phone
        this.email = u.email
        this.firstName = u.firstName
        this.lastName = u.lastName
        this.location = u.location
    }
    constructor(id: String, firstName: String, lastName: String, phone: String, email: String, location: Latlng) {
        this.id = id
        this.phone = phone
        this.email = email
        this.firstName = firstName
        this.lastName = lastName
        this.location = location
    }

    companion object {
        fun mapToUser(map: Map<String, Any?>): User {
            val id = map["uId"] as? String ?: ""
            val firstName = map["FirstName"] as? String ?: ""
            val lastName = map["LastName"] as? String ?: ""
            val phone = map["Phone"] as? String ?: ""
            val email = map["Email"] as? String ?: ""
            val latitude = map["Latitude"] as? Double ?: 0.0
            val longitude = map["Longitude"] as? Double ?: 0.0
            return User(id, firstName, lastName, phone, email, Latlng(latitude, longitude))
        }
    }

    fun toHashMap(): HashMap<String, Any?> {
        val data = HashMap<String, Any?>()
        data["uId"] = id
        data["FirstName"] = firstName
        data["LastName"] = lastName
        data["Email"] = email
        data["Latitude"] = location.latitude
        data["Longitude"] = location.longitude
        data["Phone"] = phone
        return data
    }

    override fun toString(): String {
        return Gson().toJson(this)
    }
}
