package com.arielfaridja.ezrahi.UI.Fragments.ActivityOverview

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.arielfaridja.ezrahi.R
import com.arielfaridja.ezrahi.UI.Main.MainActivity
import com.arielfaridja.ezrahi.databinding.FragmentActivityOverviewBinding

class ActivityOverviewFragment : Fragment() {

    companion object {
        fun newInstance() = ActivityOverviewFragment()
    }

    private lateinit var model: ActivityOverviewViewModel

    private lateinit var binding: FragmentActivityOverviewBinding
    private var enabled: Boolean = false


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentActivityOverviewBinding.inflate(inflater, container, false)
        var view = binding.root
        val mainActivity = activity as MainActivity
        mainActivity.supportActionBar!!.title = null
        mainActivity.setToolbarFloating(false)
        binding.activityNameTitle.isHintEnabled = false
        binding.editActivityButton.setOnClickListener { view: View? ->
            if (!enabled)
                isEnabled(true)
            else
                isEnabled(false)

            enabled = !enabled
        }
        return view
    }

    fun isEnabled(flag: Boolean) {
        if (flag) {
            run {
                binding.activityNameTitle.boxStrokeWidth = 1
                binding.activityNameTitle.isHintEnabled = true
                binding.activityNameTitle.hint = "activity name"
                binding.activityNameTitle.editText!!.setTextAppearance(R.style.editableText_enabled)
                binding.activityNameTitle.editText!!.isEnabled = true
            }
        } else
            run {
                binding.activityNameTitle.boxStrokeWidth = 0
                binding.activityNameTitle.isHintEnabled = false
                binding.activityNameTitle.editText!!.setTextAppearance(R.style.editableText_disabled)
                binding.activityNameTitle.editText!!.isEnabled = false
            }

    }


}