package kr.mad.wsacovid.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.ColorStateList
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayoutMediator
import kr.mad.wsacovid.R
import kr.mad.wsacovid.activity.helper.BaseActivity
import kr.mad.wsacovid.adapter.CovidStatsAdapter
import kr.mad.wsacovid.adapter.TabAdapter
import kr.mad.wsacovid.data.CovidStatsData
import kr.mad.wsacovid.data.SymptomsData
import kr.mad.wsacovid.restapi.JsonParser
import kr.mad.wsacovid.restapi.RestAPIConnector
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Response
import org.json.JSONObject
import org.w3c.dom.Text
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.properties.Delegates

class HomeActivity : BaseActivity() {
    private lateinit var lastDay: Date
    private var lastProbability by Delegates.notNull<Int>()
    private var symptomsList = ArrayList<SymptomsData>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        val connector = RestAPIConnector()
        val client = OkHttpClient.Builder().build()
        val symptoms = connector.getSymptomsHistory(intent.getStringExtra("id")!!)
        client.newCall(symptoms).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                val state: TextView = findViewById(R.id.state)
                state.text = getString(R.string.ERROR4)
                setCaseText()
            }

            override fun onResponse(call: Call, response: Response) {
                val jsonParser = JsonParser()
                val json = jsonParser.stringToJson(response.body?.string()!!)
                println(json.toString())
                if (json.getBoolean("success")) {
                    val data = jsonParser.getJsonArray(json, "data")
                    for (i in 0 until data.length()) {
                        val jsonObj = data.getJSONObject(i)
                        val date = SimpleDateFormat("yyyy-MM-dd hh:mm:ss").parse(jsonObj.getString("date"))
                        val probability = jsonObj.getInt("probability_infection")

                        symptomsList.add(SymptomsData(date,probability))

                        if (i == 0) {
                            lastDay = date!!
                            lastProbability = probability
                        }
                        if (lastDay.time < date!!.time) {
                            lastDay = date
                            lastProbability = probability
                        }
                    }
                    withReport()
                }
                else {
                    withOutReport()
                }
            }
        })
    }

    private fun setCaseText() {
        // Rest API Connecting for set case text
        val casesText: TextView = findViewById(R.id.cases)
        val connector = RestAPIConnector()
        val client = OkHttpClient.Builder().build()
        val cases = connector.getCases()
        client.newCall(cases).enqueue(object : Callback {
            // server Error
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    val caseView: View = findViewById(R.id.linearLayout)
                    val caseText: TextView = findViewById(R.id.casesText)
                    caseView.backgroundTintList = getColorStateList(R.color.red)
                    caseText.text = getString(R.string.ERROR4)
                    caseText.translationX = -20f
                    casesText.text = ""
                }
            }

            //set case Text
            override fun onResponse(call: Call, response: Response) {
                val jsonParser = JsonParser()
                val data = jsonParser.stringToJson(response.body?.string()!!).getInt("data")
                runOnUiThread {
                    casesText.text = if (data == 0) "No case" else data.toString()+"case"
                }
            }
        })
    }

    private fun setCovidStats() {
        val client = OkHttpClient.Builder().build()
        val covidStats = RestAPIConnector().getCovidStats()
        client.newCall(covidStats).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("ERROR", e.stackTraceToString())
            }

            override fun onResponse(call: Call, response: Response) {
                val dataList = ArrayList<CovidStatsData>()

                val json = JsonParser().stringToJson(response.body?.string()!!)
                val data = JsonParser().getJsonObj(json, "data")
                val world = JsonParser().getJsonObj(data, "world")
                val worldBefore = JsonParser().getJsonObj(data, "world_before")
                val city = JsonParser().getJsonObj(data, "current_city")

                val subVal1 = (world.getInt("infected") - worldBefore.getInt("infected")).toString() + " cases in your city"
                val subVal2 = if (city.getInt("vaccinated") > 50) "Keep it going and ask\nfriends to get vaccine"
                else "It’s very bad result\nfor making world safe"
                dataList.add(CovidStatsData("Infection cases", world.getString("infected"), "Over all world", subVal1))
                dataList.add(CovidStatsData("Deaths", world.getString("death"), "Over all world", city.getString("death")+" death in your city"))
                dataList.add(CovidStatsData("Recovered", world.getString("recovered"), world.getString("recovered_adults")+"% - adults", world.getString("recovered_young")+"% - young"))
                dataList.add(CovidStatsData("in your city", city.getString("vaccinated")+"%", "People vaccinated", subVal2))
                dataList.add(CovidStatsData("That you in safe", "Be sure", "", "Upload your contacts\nto our server and we\nwill keep you in touch\nabout infection cases"))

                val adapter = CovidStatsAdapter()
                adapter.dataList = dataList
                adapter.activity = this@HomeActivity

                runOnUiThread {
                    val covidView: ViewPager2 = findViewById(R.id.covidView)
                    covidView.orientation = ViewPager2.ORIENTATION_VERTICAL
                    covidView.adapter = adapter
                }
            }
        })
    }

    // report Data is not exist
    private fun withOutReport() {
        runOnUiThread {
            setContentView(R.layout.activity_home)
            setCaseText()
            setCovidStats()
            currentWeek()
            val date: TextView = findViewById(R.id.date)
            date.text = SimpleDateFormat("MMM dd, yyyy").format(Calendar.getInstance().timeInMillis)

            val name: TextView = findViewById(R.id.name)
            name.text = intent.getStringExtra("name")+","

            val qr: ImageView = findViewById(R.id.qr)
            qr.setOnClickListener {
                val intent = Intent(this, QRActivity::class.java)
                intent.putExtra("token", this.intent.getStringExtra("token"))
                startActivity(intent)
            }

            val checkIn: Button = findViewById(R.id.checkIn)
            checkIn.setOnClickListener {
                val intent = Intent(this, CheckInActivity::class.java)
                startActivity(intent)
            }
        }
    }

    // report Data is exist
    private fun withReport() {
        runOnUiThread {
            setContentView(R.layout.activit_home_report)
            setCaseText()
            setCovidStats()
            currentWeek()
            val date: TextView = findViewById(R.id.date)
            date.text = SimpleDateFormat("MMM dd, yyyy").format(Calendar.getInstance().timeInMillis)

            val name: TextView = findViewById(R.id.name)
            name.text = intent.getStringExtra("name")

            val monthDay: TextView = findViewById(R.id.monthDay)
            monthDay.text = SimpleDateFormat("MM/dd").format(lastDay)

            val yearTime: TextView = findViewById(R.id.yearTime)
            yearTime.text = SimpleDateFormat("/yyyy  mm:ddaa").format(lastDay)

            val share: ImageView = findViewById(R.id.share)
            if (lastProbability >= 60) {
                val titleView: View = findViewById(R.id.titleView)
                titleView.backgroundTintList = getColorStateList(R.color.red)

                val title: TextView = findViewById(R.id.title)
                title.text = "CALL TO DOCTOR"

                val description: TextView = findViewById(R.id.description)
                description.text = "You may be infected with a virus"

                share.setOnClickListener {
                    val uri = Uri.parse("sms:")
                    val intent = Intent(Intent.ACTION_VIEW, uri)
                    intent.putExtra("sms_body", "You may be infected with a virus")
                    startActivity(intent)
                }
            }
            else {
                share.setOnClickListener {
                    val uri = Uri.parse("sms:")
                    val intent = Intent(Intent.ACTION_VIEW, uri)
                    intent.putExtra("sms_body", "* Wear mask.  Keep 2m distance.  Wash hands.")
                    startActivity(intent)
                }
            }
        }
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private fun currentWeek() {
        val checkList = arrayListOf<View>(findViewById(R.id.check1),findViewById(R.id.check2),findViewById(R.id.check3),findViewById(R.id.check4),findViewById(R.id.check5),findViewById(R.id.check6),findViewById(R.id.check7))
        val lineList = arrayListOf<View>(findViewById(R.id.line1),findViewById(R.id.line2),findViewById(R.id.line3),findViewById(R.id.line4),findViewById(R.id.line5),findViewById(R.id.line6), findViewById(R.id.line6))

        var checkToday = false
        val currentDay = Calendar.getInstance()
        for (i in checkList.indices) {
            for (symptom in symptomsList) {
                currentDay.set(Calendar.DAY_OF_WEEK, i+1)
                val sdf = SimpleDateFormat("yyyyMMdd")
                println(sdf.format(symptom.date.time))
                println(sdf.format(currentDay.time))
                println("-------------")
                if (sdf.format(symptom.date.time) == sdf.format(currentDay.time)){
                    checkList[i].backgroundTintList = getColorStateList(R.color.blue)
                    try {
                        lineList[i-1].backgroundTintList = getColorStateList(R.color.blue)
                    }
                    catch (e: Exception) {}
                    if (symptom.infection >= 60) {
                        checkList[i].backgroundTintList = getColorStateList(R.color.pink)
                        try {
                            lineList[i-1].backgroundTintList = getColorStateList(R.color.pink)
                        }
                        catch (e: Exception) {}
                    }
                }
                else {
                    checkList[i].setBackgroundResource(R.drawable.ic_check_false)
                }
            }
            if (checkToday) {
                checkList[i].backgroundTintList = getColorStateList(R.color.gray)
                try {
                    lineList[i-1].backgroundTintList = getColorStateList(R.color.gray)
                }
                catch (e: Exception) {}
            }

            val today = Calendar.getInstance()
            val data = today.get(Calendar.DAY_OF_WEEK)
            if (data-1 == i) {
                checkList[i].layoutParams.height = 60
                checkList[i].layoutParams.width = 60
                checkList[i].requestLayout()
                checkToday = true
            }
        }
    }
}