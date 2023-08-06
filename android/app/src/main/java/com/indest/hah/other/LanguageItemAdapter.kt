package com.indest.hah.other

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Recycler
import com.google.android.material.chip.Chip
import com.indest.hah.R
import java.security.AccessController.getContext


class LanguageItemAdapter(
    private val context: Context,
    private val disableCount:Boolean = false,
    private val dataSet: List<Pair<String,Int>>,
    private val onItemClicked: (String?) -> Unit) :
        RecyclerView.Adapter<LanguageItemAdapter.LanguageItemViewHolder>() {

    private val TAG = "LanguageItemAdapter"

    /**
     * Provide a reference to the type of views that you are using
     * (custom ViewHolder).
     */
    class LanguageItemViewHolder(
        view: View,
        onItemClicked: (Int) -> Unit
    ) : RecyclerView.ViewHolder(view) {
        init {
            itemView.setOnClickListener {
                onItemClicked(adapterPosition)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LanguageItemViewHolder {
        // Create a new view, which defines the UI of the list item
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.language_select_item, parent, false)
        if (disableCount) {
            view.findViewById<AppCompatTextView>(R.id.count_of_levels).visibility = View.GONE
        }
        return LanguageItemViewHolder(view) {
            onItemClicked(dataSet[it].first)
        }
    }

    override fun onBindViewHolder(holder: LanguageItemViewHolder, position: Int) {
        val languageAndFlag = dataSet[position].first.mapCountryCodeToLanguageAndFlag(context)
        holder.itemView.findViewById<AppCompatTextView>(R.id.language_to_select).text = languageAndFlag.first
        holder.itemView.findViewById<AppCompatImageView>(R.id.language_image).setImageResource(languageAndFlag.second)
        if (!disableCount) {
            holder.itemView.findViewById<AppCompatTextView>(R.id.count_of_levels).text = dataSet[position].second.toString()
        }
    }

    override fun getItemCount() = dataSet.size



}