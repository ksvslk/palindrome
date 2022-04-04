package com.indest.hah.other

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.indest.hah.R
import com.indest.hah.persistance.level.LevelProgress
import com.indest.hah.persistance.dto.LevelData

class LevelItemAdapter(
    private val currentLevelIcon: String,
    private val currentLevel: Int?,
    private val dataSet: List<Pair<Boolean, LevelData>>,
    private val onItemClicked: (LevelData?) -> Unit
) :
    RecyclerView.Adapter<LevelItemAdapter.LevelItemViewHolder>() {

    val TAG = "LevelSelectAdapter"

    /**
     * Provide a reference to the type of views that you are using
     * (custom ViewHolder).
     */
    class LevelItemViewHolder(
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
    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): LevelItemViewHolder {
        // Create a new view, which defines the UI of the list item
        val view = LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.level_select_item, viewGroup, false)
        return LevelItemViewHolder(view) {
            onItemClicked(dataSet[it].second)
        }
    }

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(levelItemViewHolder: LevelItemViewHolder, position: Int) {
        Log.d(TAG, "onBindViewHolder")
        // Get element from your dataset at this position and replace the
        // contents of the view with that element

        if (dataSet[position].first) {
            levelItemViewHolder.textView.text = "${dataSet[position].second.levelId}*"
        } else {
            levelItemViewHolder.textView.text = dataSet[position].second.levelId.toString()
        }

        levelItemViewHolder.itemView.isClickable =
            if (dataSet[position].second.progress == LevelProgress.TODO) {
                levelItemViewHolder.textView.isEnabled = false
                false
            } else {
                levelItemViewHolder.textView.isEnabled = true
                true
            }
        currentLevel?.let {
            if (it == dataSet[position].second.levelId) {
                levelItemViewHolder.setIsRecyclable(false)
                levelItemViewHolder.textView.text = currentLevelIcon
            }
        }

    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount() = dataSet.size

}
