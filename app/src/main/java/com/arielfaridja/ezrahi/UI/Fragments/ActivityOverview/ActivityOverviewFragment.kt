package com.arielfaridja.ezrahi.UI.Fragments.ActivityOverview

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.arielfaridja.ezrahi.R
import com.arielfaridja.ezrahi.UI.Main.MainActivity
import com.arielfaridja.ezrahi.databinding.FragmentActivityOverviewBinding

import android.widget.Toast
import com.arielfaridja.ezrahi.entities.ActUser

class ActivityOverviewFragment : Fragment() {

    companion object {
        fun newInstance() = ActivityOverviewFragment()
    }

    private val model: ActivityOverviewViewModel by viewModels()

    private lateinit var binding: FragmentActivityOverviewBinding
    private var enabled: Boolean = false
    private lateinit var usersAdapter: UsersAdapter


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentActivityOverviewBinding.inflate(inflater, container, false)
        val view = binding.root
        val mainActivity = activity as MainActivity
        mainActivity.supportActionBar!!.title = null
        mainActivity.setToolbarFloating(false)
        binding.activityNameTitle.isHintEnabled = false

        // RecyclerView setup
        usersAdapter = UsersAdapter()
        binding.ActivityDetails.layoutManager = LinearLayoutManager(requireContext())
        binding.ActivityDetails.adapter = usersAdapter

        // Observe activity name
        model.activity.observe(viewLifecycleOwner) { act ->
            binding.activityNameTitle.editText?.setText(act?.name ?: "")
        }

        // Observe users list
        model.users.observe(viewLifecycleOwner) { usersMap ->
            usersAdapter.submitList(usersMap?.values?.toList() ?: emptyList())
        }

        // Edit button logic
        binding.editActivityButton.setOnClickListener {
            if (!enabled) {
                isEnabled(true)
            } else {
                // Save new name
                val newName = binding.activityNameTitle.editText?.text?.toString()?.trim() ?: ""
                if (newName.isNotEmpty()) {
                    model.updateActivityName(newName)
                    Toast.makeText(requireContext(), "שם הפעילות עודכן", Toast.LENGTH_SHORT).show()
                }
                isEnabled(false)
            }
            enabled = !enabled
        }

        // Disable editing by default
        isEnabled(false)

        return view
    }

    fun isEnabled(flag: Boolean) {
        if (flag) {
            binding.activityNameTitle.boxStrokeWidth = 1
            binding.activityNameTitle.isHintEnabled = true
            binding.activityNameTitle.hint = getString(R.string.activity_name)
            binding.activityNameTitle.editText?.setTextAppearance(R.style.editableText_enabled)
            binding.activityNameTitle.editText?.isEnabled = true
        } else {
            binding.activityNameTitle.boxStrokeWidth = 0
            binding.activityNameTitle.isHintEnabled = false
            binding.activityNameTitle.editText?.setTextAppearance(R.style.editableText_disabled)
            binding.activityNameTitle.editText?.isEnabled = false
        }
    }



}