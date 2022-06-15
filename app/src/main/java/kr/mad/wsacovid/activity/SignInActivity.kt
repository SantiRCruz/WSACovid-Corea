package kr.mad.wsacovid.activity

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.PermissionChecker
import kr.mad.wsacovid.R
import kr.mad.wsacovid.activity.helper.BaseActivity
import kr.mad.wsacovid.restapi.JsonParser
import kr.mad.wsacovid.restapi.RestAPIConnector
import okhttp3.*
import java.io.IOException
import java.util.regex.Pattern

class SignInActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_in)
        permissionCheck()

        // Find EditText InputLayout
        val loginEditor: EditText = findViewById(R.id.login)
        val passEditor: EditText = findViewById(R.id.pass)

        //Find Layout for Error state
        val errorLayout: LinearLayout = findViewById(R.id.error)
        val errorText: TextView = findViewById(R.id.errorText)

        // Find SignIn Button and set Click Event
        val signIn: Button = findViewById(R.id.signIn)
        signIn.setOnClickListener {
            val emailPattern = Pattern.compile("@")
            val login = loginEditor.text.toString()
            val password = passEditor.text.toString()

            // email format Error
            if (!emailPattern.matcher(login).find()) {
                errorMsg(R.string.ERROR3)
                return@setOnClickListener
            }

            // network info Error
            if (!networkInfo()) {
                errorMsg(R.string.ERROR2)
                return@setOnClickListener
            }

            // server connecting
            val connector = RestAPIConnector()
            val client = OkHttpClient.Builder().build()
            client.newCall(connector.post("/signin/", connector.signIn(login, password))).enqueue(object : Callback {
                // server Error
                override fun onFailure(call: Call, e: IOException) {
                    runOnUiThread {
                        errorMsg(R.string.ERROR4)
                    }
                    Log.e("ERROR", e.stackTraceToString())
                }

                override fun onResponse(call: Call, response: Response) {
                    try {
                        val jsonParser = JsonParser()
                        val json = jsonParser.stringToJson(response.body?.string()!!)
                        println(json.toString())
                        // account Error
                        if (!json.getBoolean("success")) runOnUiThread {
                            errorMsg(R.string.ERROR1)
                        }
                        else {
                            // signIn complete and start HomeActivity
                            val data = jsonParser.getJsonObj(json, "data")
                            val id = data.getString("id")
                            val token = data.getString("token")
                            val name = data.getString("name")
                            val intent = Intent(this@SignInActivity, HomeActivity::class.java)
                            intent.putExtra("id", id)
                            intent.putExtra("token", token)
                            intent.putExtra("name", name)
                            startActivity(intent)
                            getSharedPreferences("App", MODE_PRIVATE).edit().putString("token", token).apply()
                            finish()
                        }
                    }
                    // server Error
                    catch (e: Exception) {
                        Log.e("ERROR", e.stackTraceToString())
                        errorMsg(R.string.ERROR4)
                    }
                }
            })
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        for (grantResult in grantResults) {
            if (grantResult != PackageManager.PERMISSION_GRANTED) {
                permissionCheck()
            }
        }
    }

    // Error Action Animator
    private fun errorMsg(errorCode: Int) {
        val errorLayout: LinearLayout = findViewById(R.id.error)
        val errorText: TextView = findViewById(R.id.errorText)
        val signIn: Button = findViewById(R.id.signIn)

        errorText.text = getText(errorCode)
        signIn.animate().setDuration(200).translationY(300f)
        errorLayout.animate().setDuration(200).alpha(1f).withEndAction {
            errorLayout.animate().setDuration(1200).alpha(1f).withEndAction {
                errorLayout.animate().setDuration(200).alpha(0f)
                signIn.animate().setDuration(200).translationY(0f)
            }
        }
    }

    private fun permissionCheck() {
        var permission = true
        if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_DENIED) permission = false
        if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_DENIED) permission = false
        if (checkSelfPermission(Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_DENIED) permission = false

        if (!permission) {
            requestPermissions(arrayOf(
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.READ_CONTACTS
            ), 0)
        }
    }
}