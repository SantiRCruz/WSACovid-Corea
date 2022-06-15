package kr.mad.wsacovid.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.graphics.*
import android.os.Environment
import android.widget.RemoteViews
import kr.mad.wsacovid.R
import kr.mad.wsacovid.restapi.JsonParser
import kr.mad.wsacovid.restapi.RestAPIConnector
import okhttp3.OkHttpClient
import java.io.File
import java.io.FileOutputStream
import kotlin.concurrent.thread

/**
 * Implementation of App Widget functionality.
 */
class QRMediumAppWidget : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        // There may be multiple widgets active, so update all of them
        for (appWidgetId in appWidgetIds) {
            updateAppWidget2(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onEnabled(context: Context) {
        // Enter relevant functionality for when the first widget is created
    }

    override fun onDisabled(context: Context) {
        // Enter relevant functionality for when the last widget is disabled
    }
}

internal fun updateAppWidget2(
    context: Context,
    appWidgetManager: AppWidgetManager,
    appWidgetId: Int,
) {
    // Construct the RemoteViews object
    val views = RemoteViews(context.packageName, R.layout.q_r_medium_app_widget)

    val text2 = textToBitmap(context, "in your city", "fonts/proxima-regular.otf")
    views.setImageViewBitmap(R.id.casesText, text2)

    thread {
        OkHttpClient().newCall(RestAPIConnector().getCases()).execute().body?.string()?.let {
            val data = "+${JsonParser().stringToJson(it).getString("data")} cases"
            views.setImageViewBitmap(R.id.cases, textToBitmap(context, data, "fonts/proxima-bold.otf"))
        }
        OkHttpClient().newCall(RestAPIConnector().getCovidStats()).execute().body?.string()?.let {
            val currentCity = JsonParser().getJsonObj(JsonParser().stringToJson(it), "data").getJSONObject("current_city")
            views.setImageViewBitmap(R.id.vaccinated, textToBitmap(context, "+${currentCity.getString("vaccinated")} vaccinated", "fonts/proxima-regular.otf"))
            views.setImageViewBitmap(R.id.recovered, textToBitmap(context, "+${currentCity.getString("recovered")} recovered", "fonts/proxima-regular.otf"))
        }
    }

    views.setImageViewBitmap(R.id.qr, getQR())

    // Instruct the widget manager to update the widget
    appWidgetManager.updateAppWidget(appWidgetId, views)
}

private fun textToBitmap(context: Context, text: String, type: String): Bitmap {
    val bitmap = Bitmap.createBitmap(160, 100, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint().apply {
        typeface = Typeface.createFromAsset(context.assets, type)
        style = Paint.Style.FILL
        color = Color.WHITE
        textSize = 10f
    }
    canvas.drawText(text, 0f, 0f, paint)
    val fos = FileOutputStream(File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), "ppp.jpg"))
    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos)
    return bitmap
}