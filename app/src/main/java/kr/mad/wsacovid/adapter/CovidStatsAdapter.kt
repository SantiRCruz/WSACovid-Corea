package kr.mad.wsacovid.adapter

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentResolver
import android.content.Context
import android.provider.ContactsContract
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import kr.mad.wsacovid.R
import kr.mad.wsacovid.data.CovidStatsData
import kr.mad.wsacovid.restapi.JsonParser
import kr.mad.wsacovid.restapi.RestAPIConnector
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException

class CovidStatsAdapter: RecyclerView.Adapter<CovidStatsAdapter.ViewHolder>() {
    lateinit var dataList: ArrayList<CovidStatsData>
    lateinit var activity: Activity

    class ViewHolder(private val view: View): RecyclerView.ViewHolder(view) {
        private val title: TextView = view.findViewById(R.id.title)
        private val titleVal: TextView = view.findViewById(R.id.titleVal)
        private val sub: TextView = view.findViewById(R.id.sub)
        private val subVal: TextView = view.findViewById(R.id.subVal)
        private val subVal2: TextView = view.findViewById(R.id.subVal2)

        @SuppressLint("Recycle", "Range")
        fun bind(dataList: ArrayList<CovidStatsData>, activity: Activity) {
            title.text = dataList[adapterPosition].title
            titleVal.text = dataList[adapterPosition].titleVal
            sub.text = dataList[adapterPosition].sub

            if (adapterPosition == 2) subVal2.text = dataList[adapterPosition].subVal
            else subVal.text = dataList[adapterPosition].subVal

            if (adapterPosition == dataList.lastIndex) {
                subVal.translationY = -34f
                subVal.lineHeight = 50
                titleVal.textSize = 32f
                title.translationY = -24f

                view.setOnClickListener {
                    val body = ArrayList<JSONObject>()
                    val cr = view.context.contentResolver
                    val cursor = cr.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, null, null, null)
                    cursor?.let {
                        while (it.moveToNext()) {
                            val nameData = it.getString(it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME))
                            val name = nameData.split(" ")[0]
                            val surname = nameData.split(" ")[1]
                            val phone = it.getString(it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER))

                            val jsonObj = JSONObject()
                            jsonObj.put("name", name)
                            jsonObj.put("surname", surname)
                            jsonObj.put("tel", phone)
                            body.add(jsonObj)
                        }
                    }
                    val client = OkHttpClient()
                    val postContact = RestAPIConnector().contacts(body)
                    client.newCall(RestAPIConnector().post("/contacts", postContact)).enqueue(object : Callback {
                        override fun onFailure(call: Call, e: IOException) {
                            Toast.makeText(view.context, view.context.getString(R.string.ERROR4), Toast.LENGTH_SHORT).show()
                            Log.e("ERROR", e.stackTraceToString())
                        }

                        override fun onResponse(call: Call, response: Response) {
                            try {
                                val json = JsonParser().stringToJson(response.body?.string()!!)
                                if (json.getBoolean("success")) {
                                    activity.runOnUiThread {
                                        Toast.makeText(view.context, "Success", Toast.LENGTH_SHORT).show()

                                        sub.text = "No one"
                                        sub.textSize = 32f
                                        subVal.translationY = 0f
                                        subVal.text = "vaccinated yet"
                                        titleVal.text = "${body.size} cases"
                                        title.text = "in your contacts list"
                                    }
                                }
                                else {
                                    activity.runOnUiThread {
                                        Toast.makeText(view.context, view.context.getString(R.string.ERROR4), Toast.LENGTH_SHORT).show()
                                    }
                                }
                            } catch (e: Exception) {
                                activity.runOnUiThread {
                                    Toast.makeText(view.context, view.context.getString(R.string.ERROR4), Toast.LENGTH_SHORT).show()
                                    Log.e("ERROR", e.stackTraceToString())
                                }
                            }
                        }
                    })
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.covid_list, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(dataList, activity)
    }

    override fun getItemCount(): Int = dataList.size
}