package com.indest.hah.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.indest.hah.R
import com.indest.hah.persistance.dto.LevelData

class LevelItemAdapter(
    private val dataSet: List<LevelData>,
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
        val textView: TextView = view.findViewById(R.id.textView)

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
            onItemClicked(dataSet[it])
        }
    }

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(levelItemViewHolder: LevelItemViewHolder, position: Int) {

        // Get element from your dataset at this position and replace the
        // contents of the view with that element
        levelItemViewHolder.textView.text = dataSet[position].levelId.toString()
    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount() = dataSet.size


}
