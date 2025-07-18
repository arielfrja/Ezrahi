package com.arielfaridja.ezrahi.UI.Fragments.Settings

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.arielfaridja.ezrahi.data.DataRepoFactory
import com.arielfaridja.ezrahi.entities.Activity
import com.arielfaridja.ezrahi.entities.User

class SettingsViewModel : ViewModel {
    lateinit var user: User
    lateinit var activity: Activity
    lateinit var liveDataUser: LiveData<User>
    var dataRepo = DataRepoFactory.getInstance()

    // Directly expose LiveData from the repository
    var activitiesList: LiveData<java.util.HashMap<String, Activity>> = dataRepo.activity_getAllByCurrentUser()

    constructor(user: User, activity: Activity) : super() {
        this.user = user
        this.activity = activity
        liveDataUser = MutableLiveData(user)
    }

    fun updateCurrentActivity(actId: String) {
        dataRepo.activity_setCurrent(actId)
    }
}