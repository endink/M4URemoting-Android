// Copyright (c) 2025 Anders Xiao. All rights reserved.
// https://github.com/endink

package com.labijie.m4u.fragment
import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.google.mediapipe.tasks.components.containers.Category
import com.labijie.m4u.databinding.ItemGestureRecognizerResultBinding
import java.util.*
import kotlin.math.min

class FaceSolveResultAdapter : RecyclerView.Adapter<FaceSolveResultAdapter.ViewHolder>() {

    companion object {
        private const val NO_VALUE = "--"
    }

    private var adapterCategories: MutableList<Category?> = mutableListOf()

    @SuppressLint("NotifyDataSetChanged")
    fun updateResults(categories: List<Category>) {
//        adapterCategories = MutableList(categories.size){ null }
//        val sortedCategories = categories.sortedByDescending { it.score() }
//        val min = min(sortedCategories.size, adapterCategories.size)
//        for (i in 0 until min) {
//            adapterCategories[i] = sortedCategories[i]
//        }
//        adapterCategories.sortedBy { it?.index() }
//        notifyDataSetChanged()
    }


    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {
        val binding = ItemGestureRecognizerResultBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }


    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        adapterCategories[position].let { category ->
            holder.bind(category?.categoryName(), category?.score())
        }
    }

    override fun getItemCount(): Int = adapterCategories.size

    inner class ViewHolder(private val binding: ItemGestureRecognizerResultBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(label: String?, score: Float?) {
            with(binding) {
                tvLabel.text = label ?: NO_VALUE
                tvScore.text = if (score != null) String.format(
                    Locale.US,
                    "%.2f",
                    score
                ) else NO_VALUE
            }
        }
    }
}