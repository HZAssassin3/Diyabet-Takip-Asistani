package com.example.insulinneedlereminder.ui.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import com.example.insulinneedlereminder.R
import com.example.insulinneedlereminder.data.db.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class GlucoseWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: android.os.Bundle
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        updateAppWidget(context, appWidgetManager, appWidgetId)
    }

    companion object {
        fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_last_glucose)
            val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
            val recordLimit = calculateRecordLimit(options)

            CoroutineScope(Dispatchers.IO).launch {
                val db = AppDatabase.getInstance(context)
                val records = db.glucoseDao().getLastNDirect(recordLimit)

                withContext(Dispatchers.Main) {
                    val sdf = SimpleDateFormat("dd.MM HH:mm", Locale.getDefault())
                    views.setTextViewText(R.id.tvWidgetTitle, "Son $recordLimit Ölçüm")

                    // XML'deki yeni ID'leri dizilere alalım ki döngüyle basalım
                    val rowIds = arrayOf(
                        R.id.layoutRecord1,
                        R.id.layoutRecord2,
                        R.id.layoutRecord3,
                        R.id.layoutRecord4,
                        R.id.layoutRecord5
                    )
                    val valIds = arrayOf(R.id.tvVal1, R.id.tvVal2, R.id.tvVal3, R.id.tvVal4, R.id.tvVal5)
                    val dateIds = arrayOf(R.id.tvDate1, R.id.tvDate2, R.id.tvDate3, R.id.tvDate4, R.id.tvDate5)

                    for (i in 0 until rowIds.size) {
                        val shouldShowRow = i < recordLimit
                        views.setViewVisibility(rowIds[i], if (shouldShowRow) View.VISIBLE else View.GONE)
                        if (!shouldShowRow) continue

                        if (i < records.size) {
                            val record = records[i]
                            // Değeri String'e çevirerek basıyoruz (Hata almamak için kritik!)
                            views.setTextViewText(valIds[i], "${record.value} mg/dL")

                            val dateText = try {
                                sdf.format(Date(record.date))
                            } catch (e: Exception) {
                                record.date.toString()
                            }
                            views.setTextViewText(dateIds[i], dateText)
                        } else {
                            // Eğer 3'ten az kayıt varsa kalan satırları temizle
                            views.setTextViewText(valIds[i], "--")
                            views.setTextViewText(dateIds[i], "")
                        }
                    }
                    appWidgetManager.updateAppWidget(appWidgetId, views)
                }
            }
        }

        private fun calculateRecordLimit(options: android.os.Bundle): Int {
            val maxHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 120)
            return when {
                maxHeight < 110 -> 1
                maxHeight < 170 -> 2
                maxHeight < 230 -> 3
                maxHeight < 300 -> 4
                else -> 5
            }
        }

        fun sendRefreshBroadcast(context: Context) {
            val intent = Intent(context, GlucoseWidget::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            }
            val ids = AppWidgetManager.getInstance(context)
                .getAppWidgetIds(ComponentName(context, GlucoseWidget::class.java))

            if (ids.isNotEmpty()) {
                intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
                context.sendBroadcast(intent)
            }
        }
    }
}