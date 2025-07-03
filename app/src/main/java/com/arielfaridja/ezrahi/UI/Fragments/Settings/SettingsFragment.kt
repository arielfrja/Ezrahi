package com.arielfaridja.ezrahi.UI.Fragments.Settings

import android.os.Bundle
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.preference.ListPreference
import androidx.preference.PreferenceFragmentCompat
import com.arielfaridja.ezrahi.R
import com.arielfaridja.ezrahi.UI.Main.MainActivity
import com.arielfaridja.ezrahi.entities.Activity

class SettingsFragment : PreferenceFragmentCompat() {

    lateinit var activitiesList: ListPreference
    lateinit var model: SettingsViewModel


    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        val mainActivity = activity as MainActivity
        mainActivity.setToolbarFloating(false)
        // Observe user from MainActivity's ViewModel
        val mainViewModel = ViewModelProvider(mainActivity).get(com.arielfaridja.ezrahi.UI.Main.MainActivityViewModel::class.java)
        mainViewModel.getUserLiveData().observe(this, Observer { user ->
            if (user != null) {
                model = SettingsViewModel(user, Activity())
                setPreferencesFromResource(R.xml.root_preferences, rootKey)
                activitiesList = findPreference("currentActivity")!!
                activitiesList.entries = arrayOf("")
                activitiesList.entryValues = arrayOf("")
                activitiesList.isEnabled = false
                model.activitiesList.observe(this) {
                    var entries = ArrayList<CharSequence>()
                    var entryValues = ArrayList<CharSequence>()
                    if (it != null && it.size > 0) {
                        entries.clear()
                        entryValues.clear()
                        for (act in it) {
                            entries.add(act.value.name)
                            entryValues.add(act.key)
                        }
                        activitiesList.entries = entries.toArray(arrayOf(""))
                        activitiesList.entryValues = entryValues.toArray(arrayOf(""))
                        activitiesList.isEnabled = true
                    }
                }
                activitiesList.setOnPreferenceChangeListener { preference, newValue ->
                    var actId: String = newValue as String
                    model.updateCurrentActivity(actId)

                    return@setOnPreferenceChangeListener true
                }
            } else {
                // Show loading or error state
                preferenceScreen = null
                // Optionally, show a message to the user
            }
        })
    }

}