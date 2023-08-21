package com.indest.hah.other

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
        levelItemViewHolder.textView.text = "${(position + 1)}"

        val newLevel = isNewLevel(dataSet[position])
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
            if (dataSet[position].progress != LevelProgress.TODO) levelItemViewHolder.textView.setTextColor(highlightedColor)
        }


        levelItemViewHolder.setIsRecyclable(false)
        levelItemViewHolder.itemView.isClickable = if (dataSet[position].progress == LevelProgress.TODO) {
            levelItemViewHolder.textView.isEnabled = false
            false
        } else {
            levelItemViewHolder.textView.isEnabled = true
            true
        }
    }

    // time_modified = 0 level_id > 10000 and author != ""
    private fun isNewLevel(level:LevelData):Boolean
    {
        if (level.timeModified == 0L && level.levelId > 10000 && level.author != "")
        {
            return true
        }
        return false
    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount() = dataSet.size

}
