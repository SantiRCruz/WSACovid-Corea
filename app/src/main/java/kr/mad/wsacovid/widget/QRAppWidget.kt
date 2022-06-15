package kr.mad.wsacovid.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import android.widget.RemoteViews
import kr.mad.wsacovid.R
import kr.mad.wsacovid.restapi.JsonParser
import kr.mad.wsacovid.restapi.RestAPIConnector
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Response
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

/**
 * Implementation of App Widget functionality.
 */
class QRAppWidget : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        // There may be multiple widgets active, so update all of them
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onEnabled(context: Context) {
        // Enter relevant functionality for when the first widget is created
    }

    override fun onDisabled(context: Context) {
        // Enter relevant functionality for when the last widget is disabled
    }
}

internal fun updateAppWidget(
    context: Context,
    appWidgetManager: AppWidgetManager,
    appWidgetId: Int,
) {
    // Construct the RemoteViews object
    val views = RemoteViews(context.packageName, R.layout.q_r_app_widget)
    
    views.setImageViewBitmap(R.id.qr, getQR())

    // Instruct the widget manager to update the widget
    appWidgetManager.updateAppWidget(appWidgetId, views)
}

fun getQR(): Bitmap {
    val client = OkHttpClient()
    val userQR = RestAPIConnector().getQR()
    lateinit var bitmap: Bitmap
    val thread = Thread {
        val json = JsonParser().stringToJson(client.newCall(userQR).execute().body?.string()!!)
        val data = URL(json.getString("data"))
        val conn = data.openConnection() as HttpURLConnection
        BitmapFactory.decodeStream(conn.inputStream).let {
            bitmap = it
        }
    }
    thread.start()
    thread.join()
    return bitmap
}