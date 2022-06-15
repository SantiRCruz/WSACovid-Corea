package kr.mad.wsacovid.restapi

import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener

class JsonParser {

    fun stringToJson(data: String): JSONObject {
        return JSONTokener(data).nextValue() as JSONObject
    }

    fun getJsonObj(data: JSONObject, name: String): JSONObject {
        return data.getJSONObject(name)
    }

    fun getJsonArray(data: JSONObject, name: String): JSONArray {
        return data.getJSONArray(name)
    }
}