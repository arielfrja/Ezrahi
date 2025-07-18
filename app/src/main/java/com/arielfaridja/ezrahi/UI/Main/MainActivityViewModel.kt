package com.arielfaridja.ezrahi.UI.Main

import android.content.Intent
import android.content.SharedPreferences
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.arielfaridja.ezrahi.data.DataRepoFactory
import com.arielfaridja.ezrahi.data.IDataRepo
import com.arielfaridja.ezrahi.entities.Activity
import com.arielfaridja.ezrahi.entities.Latlng
import com.arielfaridja.ezrahi.entities.User

class MainActivityViewModel : ViewModel() {
    private val dataRepo: IDataRepo = DataRepoFactory.getInstance()
    private var intent: Intent? = null
    private var isSignIn: MutableLiveData<Boolean> = MutableLiveData(null)
    private var currentActivity: MutableLiveData<Activity> = MutableLiveData(dataRepo.activity_getCurrent())
    private var user: User? = null
    private var userLiveData: MutableLiveData<User?> = MutableLiveData(null)

    init {
        isSignIn.value = dataRepo.user_isSignedIn()
        user = dataRepo.user_getCurrent()
        userLiveData.value = user
    }

    fun getIsSignIn(): MutableLiveData<Boolean> = isSignIn
    fun setIsSignIn(isSignIn: MutableLiveData<Boolean>) { this.isSignIn = isSignIn }
    fun getActivity(): Activity? = currentActivity.value
    fun setActivity(activity: Activity) { currentActivity.value = activity }
    fun getCurrentActivity(): MutableLiveData<Activity> = currentActivity
    fun getCurrentUsersActivities() = dataRepo.activity_getAllByCurrentUser()
    fun getUser(): User? = user
    fun setUser(user: User) {
        this.user = user
        this.userLiveData.value = user
    }
    fun getUserLiveData(): MutableLiveData<User?> = userLiveData

    fun init(intent: Intent) {
        initUser(intent)
    }

    fun initUser(intent: Intent) {
        user = User(
            intent.getStringExtra("id") ?: "",
            intent.getStringExtra("first_name") ?: "",
            intent.getStringExtra("last_name") ?: "",
            intent.getStringExtra("phone") ?: "",
            intent.getStringExtra("email") ?: "",
            Latlng(
                intent.getDoubleExtra("latitude", 34.0),
                intent.getDoubleExtra("longitude", 32.0)
            )
        )
        userLiveData.value = user
    }

    fun isUserSignedIn() {
        isSignIn.value = dataRepo.user_isSignedIn()
    }


    fun loadActivityDataFromSP(sp: SharedPreferences) {
        val actId = sp.getString("actId", "") ?: ""
        if (actId.isNotEmpty()) {
            dataRepo.activity_setCurrent(actId)
            Thread {
                while (dataRepo.activity_getCurrent() == null) {}
                currentActivity.postValue(dataRepo.activity_getCurrent())
            }.start()
            // TODO: populate activity
        }
    }

    fun loadUserDataFromSP(sp: SharedPreferences) {
        user = User()
        user?.id = sp.getString("uId", "") ?: ""
        user?.firstName = sp.getString("FirstName", "") ?: ""
        user?.lastName = sp.getString("LastName", "") ?: ""
        user?.location = Latlng(
            (sp.getString("Latitude", "0.0") ?: "0.0").toDouble(),
            (sp.getString("Longitude", "0.0") ?: "0.0").toDouble()
        )
        user?.phone = sp.getString("Phone", "") ?: ""
        user?.email = sp.getString("Email", "") ?: ""
        userLiveData.value = user
    }
}
