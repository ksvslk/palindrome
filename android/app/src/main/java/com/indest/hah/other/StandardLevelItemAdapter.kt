package com.indest.hah.other

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.indest.hah.R
import com.indest.hah.persistance.dto.Level
import com.indest.hah.persistance.dto.LevelData
import com.indest.hah.persistance.level.LevelProgress

class StandardLevelItemAdapter(
    private val dataSet: List<LevelData>,
    private val onItemClicked: (LevelData?) -> Unit,
) :
    RecyclerView.Adapter<StandardLevelItemAdapter.StandardLevelItemHolder>() {

    val TAG = "LevelSelectAdapter"

    /**
     * Provide a reference to the type of views that you are using
     * (custom ViewHolder).
     */
    class StandardLevelItemHolder(
        view: View,
        onItemClicked: (Int) -> Unit
    ) : RecyclerView.ViewHolder(view) {

        val textView: TextView = view.findViewById(R.id.levelSelectView)

        init {
            itemView.setOnClickListener {
                onItemClicked(adapterPosition)
            }
        }
    }


    // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): StandardLevelItemHolder {
        // Create a new view, which defines the UI of the list item
        val view = LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.level_select_item, viewGroup, false)
        return StandardLevelItemHolder(view) {
            onItemClicked(dataSet[it])
        }
    }

    // Replace the contents of a view (invoked by the layout manager)
    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(levelItemViewHolder: StandardLevelItemHolder, position: Int) {
        levelItemViewHolder.textView.text = (position + 1).toString()
        levelItemViewHolder.setIsRecyclable(false)
        levelItemViewHolder.itemView.isClickable = if (dataSet[position].progress == LevelProgress.TODO) {
            levelItemViewHolder.textView.isEnabled = false
            false
        } else {
            levelItemViewHolder.textView.isEnabled = true
            true
        }
    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount() = dataSet.size

}
