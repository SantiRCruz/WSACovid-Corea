package kr.mad.wsacovid.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import kr.mad.wsacovid.R

class CheckListAdapter: RecyclerView.Adapter<CheckListAdapter.ViewHolder>() {
    lateinit var checkList: ArrayList<String>
    lateinit var checkStateArray: BooleanArray

    class ViewHolder(view: View): RecyclerView.ViewHolder(view) {
        val checkBox: CheckBox = view.findViewById(R.id.checkBox)
        private val title: TextView = view.findViewById(R.id.title)

        fun bind(checkList: ArrayList<String>) {
            title.text = checkList[adapterPosition]
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.check_list, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(checkList)
        holder.checkBox.setOnClickListener {
            checkStateArray[position] = holder.checkBox.isChecked
        }
    }

    override fun getItemCount(): Int = checkList.size

    fun getCheckState(): ArrayList<Int> {
        val intArray = ArrayList<Int>()
        for (i in checkStateArray.indices) {
            if (checkStateArray[i]) {
                intArray.add(i)
            }
        }
        return intArray
    }
}