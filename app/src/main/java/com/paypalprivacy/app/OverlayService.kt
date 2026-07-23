package com.paypalprivacy.app
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Rect
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat

class OverlayService : Service() {
    private lateinit var wm: WindowManager
    private var container: FrameLayout? = null
    private val cards = mutableMapOf<String, View>()
    
    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                "SHOW_FAKE" -> {
                    val rects = intent.getParcelableArrayListExtra<Rect>("rects")
                    val amounts = intent.getStringArrayListExtra("amounts")
                    showFakes(rects ?: listOf(), amounts ?: listOf())
                }
                "HIDE_OVERLAYS" -> hideAll()
            }
        }
    }
    
    override fun onCreate() {
        super.onCreate()
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel("privacy", "Privacy Overlay", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }
        
        startForeground(1, NotificationCompat.Builder(this, "privacy")
            .setContentTitle("Privacy Overlay Active")
            .setSmallIcon(android.R.drawable.ic_secure)
            .build())
        
        registerReceiver(receiver, IntentFilter().apply {
            addAction("SHOW_FAKE")
            addAction("HIDE_OVERLAYS")
        }, Context.RECEIVER_EXPORTED)
        
        setupContainer()
    }
    
    private fun setupContainer() {
        container = FrameLayout(this)
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) 
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else 
                WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or 
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            PixelFormat.TRANSLUCENT
        )
        wm.addView(container, params)
    }
    
    private fun showFakes(rects: List<Rect>, amounts: List<String>) {
        val currentKeys = rects.mapIndexed { i, r -> "${r.left},${r.top}" }.toSet()
        
        cards.keys.filter { it !in currentKeys }.forEach {
            container?.removeView(cards[it])
            cards.remove(it)
        }
        
        rects.forEachIndexed { i, rect ->
            val key = "${rect.left},${rect.top}"
            if (!cards.containsKey(key)) {
                val view = createFakeCard(amounts.getOrNull(i) ?: "$25.00")
                view.layoutParams = FrameLayout.LayoutParams(rect.width(), rect.height()).apply {
                    leftMargin = rect.left
                    topMargin = rect.top
                }
                cards[key] = view
                container?.addView(view)
            }
        }
    }
    
    private fun createFakeCard(amount: String): View {
        val merchants = listOf("Starbucks", "Amazon", "Netflix", "Uber", "Grocery Store", "Gas Station", "Pharmacy")
        
        return FrameLayout(this).apply {
            setBackgroundColor(Color.WHITE)
            elevation = 8f
            setPadding(30, 15, 30, 15)
            
            addView(TextView(this@OverlayService).apply {
                text = merchants.random()
                textSize = 16f
                setTextColor(Color.BLACK)
            })
            
            addView(TextView(this@OverlayService).apply {
                text = amount
                textSize = 16f
                setTextColor(Color.parseColor("#0070BA"))
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply { 
                    gravity = Gravity.END or Gravity.CENTER_VERTICAL 
                }
            })
        }
    }
    
    private fun hideAll() {
        cards.values.forEach { container?.removeView(it) }
        cards.clear()
    }
    
    override fun onDestroy() {
        unregisterReceiver(receiver)
        container?.let { wm.removeView(it) }
        super.onDestroy()
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
}
