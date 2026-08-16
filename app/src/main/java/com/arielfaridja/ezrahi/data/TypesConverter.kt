package com.arielfaridja.ezrahi.data

import androidx.room.TypeConverter
import com.arielfaridja.ezrahi.entities.Latlng
import com.arielfaridja.ezrahi.entities.User
import com.google.gson.Gson

class TypesConverter {
    companion object {
        @TypeConverter
        @JvmStatic
        fun userToString(user: User?): String? {
            return user?.toString()
        }

        @TypeConverter
        @JvmStatic
        fun stringToUser(string: String?): User? {
            return if (string == null) null else Gson().fromJson(string, User::class.java)
        }

        @TypeConverter
        @JvmStatic
        fun stringToLatlng(string: String?): Latlng? {
            return if (string == null) null else Latlng.StringToLatlng(string)
        }

        @TypeConverter
        @JvmStatic
        fun latlngToString(latlng: Latlng?): String? {
            return latlng?.toString()
        }

        @TypeConverter
        @JvmStatic
        fun fromIntList(list: ArrayList<Int>?): String? {
            return list?.joinToString(",")
        }

        @TypeConverter
        @JvmStatic
        fun toIntList(data: String?): ArrayList<Int>? {
            return if (data.isNullOrEmpty()) arrayListOf() else ArrayList(data.split(",").map { it.toInt() })
        }
    }
}
