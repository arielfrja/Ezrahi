package com.arielfaridja.ezrahi.data

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.arielfaridja.ezrahi.entities.*
import org.json.JSONArray

interface IDataRepo {
    fun activity_get(actId: String, callback: Callback<Activity>): MutableLiveData<Activity>
    fun activity_getAllByCurrentUser(): MutableLiveData<HashMap<String, Activity>>
    fun activity_getAllByUser(uId: String, callback: Callback<HashMap<String, Activity>>)
    fun activity_getCurrent(): Activity
    fun activity_setCurrent(actId: String)
    fun auth_email_user_login(username: String, password: String, callback: Callback<User>): LiveData<User>
    fun auth_email_user_register(user: User, password: String, callback: Callback<User>)
    fun user_add(user: User): Boolean
    fun user_get(uId: String, callback: Callback<User>): MutableLiveData<User>
    fun user_getAll(): JSONArray
    fun user_getAllByActivity(actId: String, callback: Callback<HashMap<String, ActUser>>): HashMap<String, ActUser>
    fun report_getAllByActivity(actId: String, callback: Callback<HashMap<String, Report>>)
    fun report_add(report: Report, callback: Callback<String>)
    fun report_getAllByCurrentActivity(): MutableLiveData<HashMap<String, Report>>
    fun user_getAllByCurrentActivity(): MutableLiveData<HashMap<String, ActUser>>
    fun user_getCurrent(): User
    fun user_getLocation(uId: String): Latlng
    fun user_isSignedIn(): Boolean
    fun user_remove(uId: String): Boolean
    fun user_setCurrent(uId: String)
    fun user_update(user: User): Boolean
}
