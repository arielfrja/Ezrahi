package com.arielfaridja.ezrahi.entities

import com.google.firebase.firestore.GeoPoint
import org.json.JSONException
import org.json.JSONObject

class Latlng() {
    var latitude: Double = 0.0
    var longitude: Double = 0.0

    constructor(latitude: Double, longitude: Double) : this() {
        this.latitude = latitude
        this.longitude = longitude
    }

    constructor(latlng: Latlng) : this(latlng.latitude, latlng.longitude)
    constructor(point: GeoPoint) : this(point.latitude, point.longitude)

    companion object {
        @JvmStatic
        fun StringToLatlng(string: String): Latlng {
            val res = Latlng()
            val j = JSONObject("{" + string + "}")
            val latlngObj = JSONObject(j.get("Latlng").toString())
            res.latitude = latlngObj.get("latitude").toString().toDouble()
            res.longitude = latlngObj.get("longitude").toString().toDouble()
            return res
        }
    }

    override fun toString(): String {
        return "Latlng: {latitude=$latitude, longitude=$longitude}"
    }
}
