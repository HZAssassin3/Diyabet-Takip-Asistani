package com.example.insulinneedlereminder.ui.glucose

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.insulinneedlereminder.data.entity.GlucoseRecord
import com.example.insulinneedlereminder.databinding.ItemGlucoseBinding
import com.example.insulinneedlereminder.util.DateUtils
import com.example.insulinneedlereminder.util.GlucoseStatus

class GlucoseAdapter(
    private val onDelete: (GlucoseRecord) -> Unit
) : ListAdapter<GlucoseRecord, GlucoseAdapter.ViewHolder>(DiffCallback()) {

    inner class ViewHolder(private val binding: ItemGlucoseBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(record: GlucoseRecord) {
            binding.tvGlucoseValue.text = "${record.value} mg/dL"
            binding.tvDateTime.text = DateUtils.formatDateTime(record.date)
            binding.tvMealStatus.text = if (record.mealStatus == "yemek_oncesi")
                "Yemek Öncesi" else "Yemek Sonrası"

            if (record.note.isNotEmpty()) {
                binding.tvNote.text = record.note
                binding.tvNote.visibility = android.view.View.VISIBLE
            } else {
                binding.tvNote.visibility = android.view.View.GONE
            }

            // Renk göstergesi
            val colorRes = when (GlucoseStatus.from(record.value)) {
                GlucoseStatus.LOW    -> com.example.insulinneedlereminder.R.color.glucose_low
                GlucoseStatus.NORMAL -> com.example.insulinneedlereminder.R.color.glucose_normal
                GlucoseStatus.HIGH   -> com.example.insulinneedlereminder.R.color.glucose_high
            }
            binding.viewStatusBar.setBackgroundColor(
                binding.root.context.getColor(colorRes)
            )

            binding.btnDelete.setOnClickListener { onDelete(record) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemGlucoseBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class DiffCallback : DiffUtil.ItemCallback<GlucoseRecord>() {
        override fun areItemsTheSame(oldItem: GlucoseRecord, newItem: GlucoseRecord) =
            oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: GlucoseRecord, newItem: GlucoseRecord) =
            oldItem == newItem
    }
}