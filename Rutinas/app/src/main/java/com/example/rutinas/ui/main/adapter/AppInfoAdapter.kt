package com.example.rutinas.ui.main.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.rutinas.R
import com.example.rutinas.ui.edit.actions.AppInfo
import com.example.rutinas.databinding.ItemAppInfoBinding
import timber.log.Timber

class AppInfoAdapter(private var appList: List<AppInfo>) : RecyclerView.Adapter<AppInfoAdapter.AppInfoViewHolder>() {

    class AppInfoViewHolder(private val binding: ItemAppInfoBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(appInfo: AppInfo) {
            binding.ivAppIcon.setImageDrawable(appInfo.icon)
            binding.tvAppName.text = appInfo.appName
            binding.switchApp.isChecked = appInfo.selected
            binding.switchApp.setOnCheckedChangeListener { _, isChecked ->
                appInfo.selected = isChecked
                Timber.d("AppInfoAdapter: Aplicacion ${appInfo.appName} isChecked: $isChecked")
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppInfoViewHolder {
        val binding = ItemAppInfoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AppInfoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AppInfoViewHolder, position: Int) {
        holder.bind(appList[position])
    }

    override fun getItemCount(): Int = appList.size

    fun updateData(newData: List<AppInfo>) {
        appList = newData
        notifyDataSetChanged()
    }
}
