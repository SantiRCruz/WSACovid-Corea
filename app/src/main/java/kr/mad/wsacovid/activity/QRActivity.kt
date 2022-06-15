package kr.mad.wsacovid.activity

import android.graphics.BitmapFactory
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import kr.mad.wsacovid.R
import kr.mad.wsacovid.activity.helper.BaseActivity
import kr.mad.wsacovid.restapi.JsonParser
import kr.mad.wsacovid.restapi.RestAPIConnector
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Response
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

class QRActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_qractivity)
        getQR()
    }

    private fun getQR() {
        val errorView: View = findViewById(R.id.errorView)
        val tryAgain: TextView = findViewById(R.id.tryAgain)

        if (!networkInfo()) {
            errorView.animate().setDuration(30).scaleX(0.95f).scaleY(0.95f).withEndAction {
                errorView.animate().setDuration(30).scaleX(1f).scaleY(1f)
            }
            tryAgain.setOnClickListener {
                getQR()
            }
        }
        else {
            errorView.visibility = View.GONE
            tryAgain.visibility = View.GONE

            val qrView: View = findViewById(R.id.qrView)
            qrView.visibility = View.VISIBLE

            val qr: ImageView = findViewById(R.id.qr)

            val client = OkHttpClient()
            val userQR = RestAPIConnector().getQR()
            client.newCall(userQR).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                }

                override fun onResponse(call: Call, response: Response) {
                    val json = JsonParser().stringToJson(response.body?.string()!!)
                    val data = URL(json.getString("data"))
                    val conn = data.openConnection() as HttpURLConnection
                    val bitmap = BitmapFactory.decodeStream(conn.inputStream)
                    runOnUiThread {
                        qr.setImageBitmap(bitmap)
                    }
                }
            })
        }
    }
}