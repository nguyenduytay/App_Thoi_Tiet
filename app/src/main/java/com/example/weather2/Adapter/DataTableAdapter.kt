package com.example.weather2.Adapter

import android.annotation.SuppressLint
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.weather2.Model.ItemAdapter.itemDataTableAdapter
import com.example.weather2.databinding.ItemDataTableAdapterBinding


class DataTableAdapter(private var itemList: List<itemDataTableAdapter>) :
    RecyclerView.Adapter<DataTableAdapter.MyAdapterViewHolder>() {
    class MyAdapterViewHolder(val binding: ItemDataTableAdapterBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyAdapterViewHolder {
        val binding = ItemDataTableAdapterBinding.inflate(
            android.view.LayoutInflater.from(parent.context),
            parent,
            false
        )
        return MyAdapterViewHolder(binding)
    }

    override fun getItemCount(): Int  = itemList.size

    override fun onBindViewHolder(holder: MyAdapterViewHolder, position: Int) {
        val item = itemList[position]
        holder.binding.time.text = item.time
        holder.binding.value.text = item.value
        holder.binding.status.text = item.status
    }
    @SuppressLint("NotifyDataSetChanged")
    fun updateData(new: List<itemDataTableAdapter>) {
        itemList = new
        notifyDataSetChanged()
    }
}