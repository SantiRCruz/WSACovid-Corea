package kr.mad.wsacovid.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kr.mad.wsacovid.R
import kr.mad.wsacovid.activity.HomeActivity
import kotlin.properties.Delegates

class TabAdapter: RecyclerView.Adapter<TabAdapter.ViewHolder>() {
    var dataSize by Delegates.notNull<Int>()

    class ViewHolder(val view: View): RecyclerView.ViewHolder(view)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.tab_list, parent, false)
        return ViewHolder(view)
    }

    private var old = -1
    private var new = -1
    private lateinit var newView: ViewHolder
    @SuppressLint("NotifyDataSetChanged")
    override fun onBindViewHolder(holder: ViewHolder, @SuppressLint("RecyclerView") position: Int) {
        holder.apply {
            if (old == position && new != old) {
                view.alpha = 0.2f
            }
            if (new == position) {
                view.alpha = 1f
            }
           /* view.setOnClickListener {
                newView = this
                old = new
                new = position
                view.alpha = 1f
                (view.context as HomeActivity).currentPositionSet(position)
                notifyDataSetChanged()
            }*/
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun scroll(up: Boolean) {
        if (up) {
            old = new
            new += 1
        }
        else {
            old = new
            new -= 1
        }
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = dataSize
}