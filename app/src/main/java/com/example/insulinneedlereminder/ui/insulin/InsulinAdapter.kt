package com.example.insulinneedlereminder.ui.insulin

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.insulinneedlereminder.data.entity.InsulinRecord
import com.example.insulinneedlereminder.databinding.ItemInsulinBinding
import com.example.insulinneedlereminder.util.DateUtils

class InsulinAdapter(
    private val onCheckChanged: (InsulinRecord, Boolean) -> Unit,
    private val onDelete: (InsulinRecord) -> Unit
) : ListAdapter<InsulinRecord, InsulinAdapter.ViewHolder>(DiffCallback()) {

    inner class ViewHolder(private val binding: ItemInsulinBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(record: InsulinRecord) {
            binding.tvTimeLabel.text = record.timeLabel
            binding.tvUnits.text = "${record.units} ünite"
            binding.tvDateTime.text = DateUtils.formatDateTime(record.date)

            if (record.note.isNotEmpty()) {
                binding.tvNote.text = record.note
                binding.tvNote.visibility = android.view.View.VISIBLE
            } else {
                binding.tvNote.visibility = android.view.View.GONE
            }

            binding.cbDone.setOnCheckedChangeListener(null)
            binding.cbDone.isChecked = record.isDone
            binding.cbDone.setOnCheckedChangeListener { _, isChecked ->
                onCheckChanged(record, isChecked)
            }

            binding.btnDelete.setOnClickListener {
                onDelete(record)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemInsulinBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class DiffCallback : DiffUtil.ItemCallback<InsulinRecord>() {
        override fun areItemsTheSame(oldItem: InsulinRecord, newItem: InsulinRecord) =
            oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: InsulinRecord, newItem: InsulinRecord) =
            oldItem == newItem
    }
}