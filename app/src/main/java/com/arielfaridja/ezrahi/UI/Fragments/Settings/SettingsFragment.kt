package com.arielfaridja.ezrahi.UI.Fragments.Settings

import android.os.Bundle
import androidx.preference.ListPreference
import androidx.preference.PreferenceFragmentCompat
import com.arielfaridja.ezrahi.R
import com.arielfaridja.ezrahi.UI.Main.MainActivity
import com.arielfaridja.ezrahi.entities.Activity

class SettingsFragment : PreferenceFragmentCompat() {

    lateinit var activitiesList: ListPreference
    lateinit var model: SettingsViewModel


    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {

        var mainActivity = activity as MainActivity
        val user = mainActivity.user
        if (user != null) {
            model = SettingsViewModel(user, Activity())
        } else {
            // Handle the null user case appropriately (e.g., show error, fallback, etc.)
            // For now, throw exception to avoid crash later
            throw IllegalStateException("User is null in SettingsFragment")
        }
        mainActivity.setToolbarFloating(false)
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

    }

}