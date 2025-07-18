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
    var activitiesList: ListPreference? = null
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
                val foundPreference = findPreference<ListPreference>("current_activity")
                if (foundPreference != null) {
                    
                    activitiesList = foundPreference
                    // Set initial placeholder
                    activitiesList?.entries = arrayOf("No activities available")
                    activitiesList?.entryValues = arrayOf("")
                    activitiesList?.isEnabled = false
                    model.activitiesList.observe(viewLifecycleOwner) { itList ->
                        if (itList != null && itList.isNotEmpty()) {
                            val entries = ArrayList<CharSequence>()
                            val entryValues = ArrayList<CharSequence>()
                            for (act in itList) {
                                entries.add(act.value.name)
                                entryValues.add(act.key)
                            }
                            activitiesList?.entries = entries.toTypedArray()
                            activitiesList?.entryValues = entryValues.toTypedArray()
                            activitiesList?.isEnabled = true
                        } else {
                            // No activities: show placeholder and disable
                            activitiesList?.entries = arrayOf("No activities available")
                            activitiesList?.entryValues = arrayOf("")
                            activitiesList?.isEnabled = false
                        }
                    }
                    activitiesList?.setOnPreferenceChangeListener { _, newValue ->
                        val actId = newValue as String
                        model.updateCurrentActivity(actId)
                        true
                    }
                } else {
                    // Handle missing preference gracefully, e.g., log or show a message
                }
            } else {
                // Show loading or error state
                preferenceScreen = null
                // Optionally, show a message to the user
            }
        })
    }

}