package danielabbott.personalorganiser.ui.goals

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import danielabbott.personalorganiser.DateTimeUtil
import danielabbott.personalorganiser.R
import danielabbott.personalorganiser.data.DB.context
import danielabbott.personalorganiser.data.Milestone
import kotlinx.android.synthetic.main.goal_list_item.view.name
import kotlinx.android.synthetic.main.milestone_list_item.view.*


class MilestoneRecyclerViewAdapter(
    private var mValues: ArrayList<Milestone>,
    private val onClick: (Milestone) -> Unit,
    private val onLongClick: (Milestone) -> Unit
) : RecyclerView.Adapter<MilestoneRecyclerViewAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.milestone_list_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = mValues[position]
        holder.name.text = item.name

        holder.deadline.text =
            if (item.deadline == null) ""
            else DateTimeUtil.getDateString(context, item.deadline!!)

        holder.layout.setOnLongClickListener {
            onLongClick(item)
            true
        }

        holder.layout.setOnClickListener {
            onClick(item)
        }
    }

    override fun getItemCount(): Int = mValues.size

    inner class ViewHolder(val mView: View) : RecyclerView.ViewHolder(mView) {
        val layout: LinearLayout = mView.linlayout
        val name: TextView = mView.name
        val deadline: TextView = mView.deadline
    }

    fun add(m: Milestone): Int {
        mValues.add(m)
        update()
        return mValues.size
    }

    fun remove(m: Milestone): Int {
        mValues.remove(m)
        notifyDataSetChanged()
        return mValues.size
    }

    fun update() {
        mValues.sortBy { it.deadline }
        notifyDataSetChanged()
    }
}
