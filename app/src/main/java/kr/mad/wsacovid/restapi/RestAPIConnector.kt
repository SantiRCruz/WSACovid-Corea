package kr.mad.wsacovid.restapi

import android.net.Uri
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.internal.http.hasBody
import org.json.JSONObject
import java.io.File
import java.util.*
import kotlin.collections.ArrayList

class RestAPIConnector {
    companion object {
        const val BASE_URL = "http://192.168.1.115:8045/api/v1"
    }

    fun signIn(login: String, password: String): RequestBody {
        val jsonObj = JSONObject()
        jsonObj.put("login", login)
        jsonObj.put("password", password)

        return RequestBody.create("application/json".toMediaType(), jsonObj.toString())
    }

    fun daySymptoms(body: ArrayList<Int>): RequestBody {
        return RequestBody.create("application/json".toMediaType(), body.toString())
    }

    fun dailyPhoto(body: Uri): RequestBody {
        val uuid = UUID.randomUUID().toString()
        return MultipartBody.Builder().addFormDataPart("file", "$uuid.jpg", RequestBody.create(MultipartBody.FORM, File(body.path!!))).build()
    }

    fun contacts(body: ArrayList<JSONObject>): RequestBody {
        return RequestBody.create("application/json".toMediaType(), body.toString())
    }

    fun getSymptomsHistory(id: String): Request {
        return Request.Builder().url("$BASE_URL/symptoms_history?user_id=$id").get().build()
    }

    fun getCases(): Request {
        return Request.Builder().url("$BASE_URL/cases").get().build()
    }

    fun getQR(): Request {
        return Request.Builder().url("$BASE_URL/user_qr").get().build()
    }

    fun getCheckList(): Request {
        return Request.Builder().url("$BASE_URL/symptom_list").get().build()
    }

    fun getCovidStats(): Request {
        return Request.Builder().url("$BASE_URL/covid_stats").get().build()
    }

    fun post(url: String, body: RequestBody): Request {
        return Request.Builder().url(BASE_URL+url).post(body).build()
    }
}