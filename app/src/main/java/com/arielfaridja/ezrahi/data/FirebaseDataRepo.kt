package com.arielfaridja.ezrahi.data

// ...existing imports...

// This is a large file. For brevity, only the class and structure are shown here. The full implementation should be ported from the Java version.

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.annotation.NonNull
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.preference.PreferenceManager
import androidx.room.Room
import com.arielfaridja.ezrahi.R
import com.arielfaridja.ezrahi.entities.*
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.*
import com.google.gson.Gson
import org.json.JSONArray
import java.util.*

class FirebaseDataRepo(val context: Context) : IDataRepo {
    // ...existing code ported from Java...
    // The full implementation should be ported here, following Kotlin best practices.

    override fun activity_get(actId: String, callback: Callback<Activity>): MutableLiveData<Activity> {
        TODO("Implement")
    }
    override fun activity_getAllByCurrentUser(): MutableLiveData<HashMap<String, Activity>> {
        TODO("Implement")
    }
    override fun activity_getAllByUser(uId: String, callback: Callback<HashMap<String, Activity>>) {
        TODO("Implement")
    }
    override fun activity_getCurrent(): Activity {
        TODO("Implement")
    }
    override fun activity_setCurrent(actId: String) {
        TODO("Implement")
    }
    override fun auth_email_user_login(username: String, password: String, callback: Callback<User>): LiveData<User> {
        TODO("Implement")
    }
    override fun auth_email_user_register(user: User, password: String, callback: Callback<User>) {
        TODO("Implement")
    }
    override fun user_add(user: User): Boolean {
        TODO("Implement")
    }
    override fun user_get(uId: String, callback: Callback<User>): MutableLiveData<User> {
        TODO("Implement")
    }
    override fun user_getAll(): org.json.JSONArray {
        TODO("Implement")
    }
    override fun user_getAllByActivity(actId: String, callback: Callback<HashMap<String, ActUser>>): HashMap<String, ActUser> {
        TODO("Implement")
    }
    override fun report_getAllByActivity(actId: String, callback: Callback<HashMap<String, Report>>) {
        TODO("Implement")
    }
    override fun report_add(report: Report, callback: Callback<String>) {
        TODO("Implement")
    }
    override fun report_getAllByCurrentActivity(): MutableLiveData<HashMap<String, Report>> {
        TODO("Implement")
    }
    override fun user_getAllByCurrentActivity(): MutableLiveData<HashMap<String, ActUser>> {
        TODO("Implement")
    }
    override fun user_getCurrent(): User {
        TODO("Implement")
    }
    override fun user_getLocation(uId: String): Latlng {
        TODO("Implement")
    }
    override fun user_isSignedIn(): Boolean {
        TODO("Implement")
    }
    override fun user_remove(uId: String): Boolean {
        TODO("Implement")
    }
    override fun user_setCurrent(uId: String) {
        TODO("Implement")
    }
    override fun user_update(user: User): Boolean {
        TODO("Implement")
    }
}
