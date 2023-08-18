@file:Suppress("PropertyName")

package com.indest.hah.other

import android.R.attr.text
import android.annotation.SuppressLint
import android.graphics.Color
import android.text.SpannableString
import android.text.style.UnderlineSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.color.MaterialColors
import com.indest.hah.R
import com.indest.hah.persistance.dto.LevelData
import com.indest.hah.persistance.level.LevelProgress


class LevelItemAdapter(
    private val currentLevelIcon: String,
    private val currentLevel: Int?,
    private val dataSet: List<Pair<Boolean, LevelData>>,
    private val playerLevel: Boolean = false,
    private val onItemClicked: (LevelData?) -> Unit,
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
    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(levelItemViewHolder: LevelItemViewHolder, position: Int) {
        // Get element from your dataset at this position and replace the
        // contents of the view with that element
        val levelNr = if (playerLevel) dataSet[position].second.levelId - 1000 else dataSet[position].second.levelId
        levelItemViewHolder.textView.text = if(dataSet[position].first) "$levelNr*" else "$levelNr"

        val newLevel = isNewLevel(dataSet[position].second)
        if (newLevel) {
            val highlightedColor = MaterialColors.getColor(
                levelItemViewHolder.textView.context,
                com.google.android.material.R.attr.colorPrimary, Color.RED
            )
            val underlinedText = SpannableString(levelItemViewHolder.textView.text)
            underlinedText.setSpan(
                UnderlineSpan(),
                0,
                levelItemViewHolder.textView.text.length,
                SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            levelItemViewHolder.textView.text = underlinedText
            levelItemViewHolder.textView.setTextColor(highlightedColor)
        }

        if (playerLevel)
        {
            levelItemViewHolder.itemView.isClickable = true
            levelItemViewHolder.textView.isEnabled = true
        }
        else {
            levelItemViewHolder.itemView.isClickable =
                if (dataSet[position].second.progress == LevelProgress.TODO) {
                    levelItemViewHolder.textView.isEnabled = false
                    false
                } else {
                    levelItemViewHolder.textView.isEnabled = true
                    true
                }
        }

        currentLevel?.let {
            if (it == levelNr && !playerLevel) {
                levelItemViewHolder.setIsRecyclable(false)
                levelItemViewHolder.textView.text = currentLevelIcon
            }
            if (newLevel)
            {
                levelItemViewHolder.setIsRecyclable(false)
            }
        }

    }

    // progress = 0, time_modified = 0 and level_id < 10000 && level_id > 1080 - NEW PLAYER LEVEL
    private fun isNewLevel(level:LevelData):Boolean
    {
        if (level.progress == LevelProgress.TODO && level.timeModified == 0L && level.levelId in 1080..9999)
        {
            return true
        }
        return false
    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount() = dataSet.size

}
