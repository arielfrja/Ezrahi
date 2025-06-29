package com.arielfaridja.ezrahi.UI.Fragments.Settings

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.arielfaridja.ezrahi.data.DataRepoFactory
import com.arielfaridja.ezrahi.entities.Activity
import com.arielfaridja.ezrahi.entities.User

    class SettingsViewModel : ViewModel {
    // TODO: Implement the ViewModel
    lateinit var user: User
    lateinit var activity: Activity
    lateinit var liveDataUser: LiveData<User>
    var dataRepo = DataRepoFactory.getInstance()
    var activitiesList = dataRepo.activity_getAllByCurrentUser()
    var update = MutableLiveData<java.util.HashMap<String, Activity>>()

    constructor(user: User, activity: Activity) : super() {
        this.user = user
        this.activity = activity
        liveDataUser = MutableLiveData(user)

        dataRepo.activity_getAllByCurrentUser().observeForever { activities ->
            if (!activities.isNullOrEmpty()) {
                activitiesList.postValue(activities)
            }
        }

        dataRepo.activity_getAllByCurrentUser()

    }

    /***
     * a function to ake sure that if its take a while to update local activities list,
     * the model will be updated too.
     */


    fun updateCurrentActivity(actId: String) {
        dataRepo.activity_setCurrent(actId)
    }
}