package com.arielfaridja.ezrahi.UI.Fragments.ActivityOverview

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.arielfaridja.ezrahi.databinding.ItemUserBinding
import com.arielfaridja.ezrahi.entities.ActUser

class UsersAdapter : ListAdapter<ActUser, UsersAdapter.UserViewHolder>(UserDiffCallback()) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val binding = ItemUserBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return UserViewHolder(binding)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class UserViewHolder(private val binding: ItemUserBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(user: ActUser) {
            binding.userName.text = "${user.firstName} ${user.lastName}"
            binding.userRole.text = user.role.toString()
            // אפשר להוסיף עוד שדות לפי הצורך
        }
    }
}

class UserDiffCallback : DiffUtil.ItemCallback<ActUser>() {
    override fun areItemsTheSame(oldItem: ActUser, newItem: ActUser): Boolean = oldItem.id == newItem.id
    override fun areContentsTheSame(oldItem: ActUser, newItem: ActUser): Boolean = oldItem == newItem
}
