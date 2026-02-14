package com.polypulse.app.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.polypulse.app.R
import com.polypulse.app.data.auth.TokenManager
import com.polypulse.app.data.remote.BackendApi
import com.polypulse.app.data.remote.dto.WhaleActivityDto
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit
import java.text.NumberFormat
import java.util.Locale

/**
 * Implementation of App Widget functionality.
 */
class WhaleWidget : AppWidgetProvider() {

    // Simple manual DI for the widget since Hilt injection into BroadcastReceiver is tricky
    // and we want to keep it lightweight.
    private fun getApi(): BackendApi {
        val json = Json { ignoreUnknownKeys = true }
        return Retrofit.Builder()
            .baseUrl("http://10.0.2.2:8000/") // Android Emulator localhost
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(BackendApi::class.java)
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        // There may be multiple widgets active, so update all of them
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val views = RemoteViews(context.packageName, R.layout.whale_widget)
        
        // Show loading state or default text initially
        views.setTextViewText(R.id.widget_market, "Loading latest whale...")

        // Launch coroutine to fetch data
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val tokenManager = TokenManager(context)
                val token = tokenManager.token.first()

                if (token != null) {
                    val api = getApi()
                    val whales = api.getWhaleActivity("Bearer $token")
                    
                    if (whales.isNotEmpty()) {
                        val latestWhale = whales.first()
                        updateWidgetView(context, views, latestWhale)
                    } else {
                        updateWidgetEmpty(context, views)
                    }
                } else {
                    updateWidgetLogin(context, views)
                }
                
                // Push update to widget
                withContext(Dispatchers.Main) {
                    appWidgetManager.updateAppWidget(appWidgetId, views)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                // Handle error silently or show simple error message
                withContext(Dispatchers.Main) {
                     views.setTextViewText(R.id.widget_market, "Error loading data")
                     appWidgetManager.updateAppWidget(appWidgetId, views)
                }
            }
        }
        
        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    private fun updateWidgetView(context: Context, views: RemoteViews, whale: WhaleActivityDto) {
        val isBuy = whale.side.equals("BUY", ignoreCase = true)
        val color = if (isBuy) context.getColor(android.R.color.holo_green_light) 
                   else context.getColor(android.R.color.holo_red_light)
        
        views.setTextViewText(R.id.widget_side, if (isBuy) "BUY" else "SELL")
        views.setTextColor(R.id.widget_side, color)
        
        val formattedValue = NumberFormat.getCurrencyInstance(Locale.US).format(whale.value_usd)
        views.setTextViewText(R.id.widget_value, formattedValue)
        
        views.setTextViewText(R.id.widget_market, whale.market_question)
        views.setTextViewText(R.id.widget_outcome, "${whale.outcome} @ ${whale.price}")
        
        // TODO: Format timestamp relative to now (e.g. "5m ago")
        views.setTextViewText(R.id.widget_timestamp, "Just now") 
    }
    
    private fun updateWidgetEmpty(context: Context, views: RemoteViews) {
        views.setTextViewText(R.id.widget_market, "No recent large trades.")
        views.setTextViewText(R.id.widget_value, "")
        views.setTextViewText(R.id.widget_side, "")
        views.setTextViewText(R.id.widget_outcome, "")
    }

    private fun updateWidgetLogin(context: Context, views: RemoteViews) {
        views.setTextViewText(R.id.widget_market, "Please login to PolyPulse app.")
        views.setTextViewText(R.id.widget_value, "")
        views.setTextViewText(R.id.widget_side, "")
        views.setTextViewText(R.id.widget_outcome, "")
    }
}
