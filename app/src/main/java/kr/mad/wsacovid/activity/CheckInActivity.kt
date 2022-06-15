package kr.mad.wsacovid.activity

import android.content.Intent
import android.database.Cursor
import android.database.CursorWindow
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.loader.content.CursorLoader
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kr.mad.wsacovid.R
import kr.mad.wsacovid.activity.helper.BaseActivity
import kr.mad.wsacovid.adapter.CheckListAdapter
import kr.mad.wsacovid.restapi.JsonParser
import kr.mad.wsacovid.restapi.RestAPIConnector
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException

class CheckInActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_check_in)
        getCheckList()

        val back: ImageView = findViewById(R.id.back)
        back.setOnClickListener {
            onBackPressed()
        }
    }

    private fun getCheckList() {
        val errorView: View = findViewById(R.id.errorView)
        val errorText: TextView = findViewById(R.id.state)
        val tryAgain: TextView = findViewById(R.id.tryAgain)

        if (!networkInfo()) {
            errorText.text = getString(R.string.ERROR2)
            errorView.animate().setDuration(30).scaleX(0.95f).scaleY(0.95f).withEndAction {
                errorView.animate().setDuration(30).scaleX(1f).scaleY(1f)
            }
            tryAgain.setOnClickListener {
                getCheckList()
            }
        }
        else {
            var realUri: Uri? = null

            val checkDataList = ArrayList<String>()
            val client = OkHttpClient()
            val getCheckList = RestAPIConnector().getCheckList()
            val checkView: View = findViewById(R.id.checkView)
            val photo: ImageView = findViewById(R.id.photo)
            val remove: ImageView = findViewById(R.id.remove)

            val getGalleryActivityResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == RESULT_OK) {
                    if (result.data != null) {
                        val uri = result.data?.data!!
                        val projection = arrayOf(MediaStore.Images.Media.DATA)
                        val cursor = managedQuery(uri, projection, null, null, null)
                        val column = cursor.getColumnIndex(MediaStore.Images.Media.DATA)
                        if (cursor.moveToFirst()) realUri = Uri.parse(cursor.getString(column))
                        photo.setImageURI(realUri)
                        remove.visibility = View.VISIBLE
                    }
                }
            }

            client.newCall(getCheckList).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    errorView.animate().setDuration(30).scaleX(0.95f).scaleY(0.95f).withEndAction {
                        errorText.text = getString(R.string.ERROR4)
                        errorView.animate().setDuration(30).scaleX(1f).scaleY(1f)
                    }
                    tryAgain.setOnClickListener {
                        getCheckList()
                    }
                }

                override fun onResponse(call: Call, response: Response) {
                    try {
                        val json = JsonParser().stringToJson(response.body?.string()!!)
                        val data = JsonParser().getJsonArray(json, "data")
                        for (i in 0 until data.length()) {
                            val check = data[i] as JSONObject
                            checkDataList.add(check.getString("title"))
                        }

                        runOnUiThread {
                            errorView.visibility = View.GONE
                            tryAgain.visibility = View.GONE
                            checkView.visibility = View.VISIBLE

                            val adapter = CheckListAdapter()
                            adapter.checkList = checkDataList
                            adapter.checkStateArray = BooleanArray(checkDataList.size)

                            val checkRecycler: RecyclerView = findViewById(R.id.checkRecycler)
                            checkRecycler.layoutManager = LinearLayoutManager(this@CheckInActivity, LinearLayoutManager.VERTICAL, false)
                            checkRecycler.adapter = adapter

                            remove.setOnClickListener {
                                photo.setImageResource(R.drawable.ic_photo)
                                realUri = null
                                remove.visibility = View.GONE
                            }

                            photo.setOnClickListener {
                                val intent = Intent(Intent.ACTION_PICK)
                                intent.type = "image/*"
                                getGalleryActivityResult.launch(intent)
                            }

                            val confirm: Button = findViewById(R.id.confirm)
                            confirm.setOnClickListener {
                                if (realUri == null) Toast.makeText(this@CheckInActivity, "check your photo", Toast.LENGTH_SHORT).show()
                                else {
                                    val checkState = (checkRecycler.adapter as CheckListAdapter).getCheckState()
                                    val postUri = realUri!!

                                    val daySymptoms = RestAPIConnector().daySymptoms(checkState)
                                    client.newCall(RestAPIConnector().post("/day_symptoms", daySymptoms)).enqueue(object : Callback {
                                        override fun onFailure(call: Call, e: IOException) {
                                            Log.e("ERROR",e.stackTraceToString())
                                        }

                                        override fun onResponse(call: Call, response: Response) {
                                           try {
                                               runOnUiThread {
                                               }
                                           } catch (e: Exception) {
                                               runOnUiThread {
                                                   Toast.makeText(this@CheckInActivity, "Failed Post Data", Toast.LENGTH_SHORT).show()
                                                   Log.e("ERROR", e.stackTraceToString())
                                               }
                                           }
                                        }
                                    })

                                    val dailyPhoto = RestAPIConnector().dailyPhoto(postUri)
                                    client.newCall(RestAPIConnector().post("/daily_photo", dailyPhoto)).enqueue(object : Callback {
                                        override fun onFailure(call: Call, e: IOException) {
                                            Log.e("ERROR",e.stackTraceToString())
                                        }

                                        override fun onResponse(call: Call, response: Response) {
                                            try {
                                                val json2 = JsonParser().stringToJson(response.body?.string()!!)
                                                val data2 = json2.getBoolean("success")
                                                runOnUiThread {
                                                    if (data2) {
                                                        Toast.makeText(this@CheckInActivity, "Complete", Toast.LENGTH_SHORT).show()
                                                        onBackPressed()
                                                    } else {
                                                        Toast.makeText(this@CheckInActivity, "Failed Post Data", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            } catch (e: Exception) {
                                                runOnUiThread {
                                                    Toast.makeText(this@CheckInActivity, "Failed Post Data", Toast.LENGTH_SHORT).show()
                                                    Log.e("ERROR", e.stackTraceToString())
                                                }
                                            }
                                        }
                                    })
                                }
                            }
                        }
                    } catch (e: Exception) {
                        errorText.text = getString(R.string.ERROR4)
                        errorView.animate().setDuration(30).scaleX(1f).scaleY(1f)
                        Log.e("ERROR", e.stackTraceToString())
                        tryAgain.setOnClickListener {
                            getCheckList()
                        }
                    }
                }
            })
        }
    }
}