package com.arielfaridja.ezrahi.UI.Fragments.ActivityOverview

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.arielfaridja.ezrahi.R
import com.arielfaridja.ezrahi.UI.Main.MainActivity

class ActivityOverviewFragment : Fragment() {

    companion object {
        fun newInstance() = ActivityOverviewFragment()
    }

    private lateinit var model: ActivityOverviewViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        var view = inflater.inflate(R.layout.fragment_activity_overview, container, false)
        val mainActivity = activity as MainActivity
        mainActivity.supportActionBar!!.title = null
        mainActivity.setToolbarFloating(false)
        return view
    }


}