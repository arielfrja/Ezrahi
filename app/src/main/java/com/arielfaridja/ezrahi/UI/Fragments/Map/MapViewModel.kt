package com.arielfaridja.ezrahi.UI.Fragments.Map

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.arielfaridja.ezrahi.data.DataRepoFactory
import com.arielfaridja.ezrahi.entities.ActUser

class MapViewModel : ViewModel() {
    private val db = DataRepoFactory.getInstance()
    val users = MutableLiveData<Map<String, ActUser>>()

    init {
        db.user_getAllByCurrentActivity().observeForever { dBusers ->
            users.value = dBusers
        }
    }
//    private val mediator = MediatorLiveData<Map<String,ActUser>>()
//    init {
//        mediator.addSource(db.user_getAllByCurrentActivity()
//        ) {
//                dbUsers -> users.value = dbUsers
//        }
//    }

    // TODO: Implement the ViewModel
}