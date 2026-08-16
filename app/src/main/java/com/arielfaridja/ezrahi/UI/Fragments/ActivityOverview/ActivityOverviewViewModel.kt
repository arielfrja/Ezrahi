package com.arielfaridja.ezrahi.UI.Fragments.ActivityOverview

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.arielfaridja.ezrahi.data.DataRepoFactory
import com.arielfaridja.ezrahi.entities.Activity
import com.arielfaridja.ezrahi.entities.ActUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.HashMap

class ActivityOverviewViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = DataRepoFactory.getInstance(application)

    private val _activity = MutableLiveData<Activity>()
    val activity: LiveData<Activity> = _activity

    // Directly expose the LiveData from the repository.
    val users: LiveData<HashMap<String, ActUser>> = repo.user_getAllByCurrentActivity()

    init {
        loadActivity()
    }

    private fun loadActivity() {
        viewModelScope.launch(Dispatchers.IO) {
            val act = repo.activity_getCurrent()
            _activity.postValue(act)
        }
    }

    fun updateActivityName(newName: String) {
        val act = _activity.value ?: return
        act.name = newName
        viewModelScope.launch(Dispatchers.IO) {
            // This logic is a bit strange, but it's what was there.
            // A dedicated update method in the repo would be better.
            repo.activity_setCurrent(act.id)
            loadActivity()
        }
    }
}