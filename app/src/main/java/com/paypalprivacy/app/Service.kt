package com.paypalprivacy.app
import android.accessibilityservice.AccessibilityService
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
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat

class PayPalAccessibilityService : AccessibilityService() {
    private val r = mutableListOf<Regex>()
    override fun onServiceConnected() { load() }
    override fun onAccessibilityEvent(e: android.view.accessibility.AccessibilityEvent) {
        val root = rootInActiveWindow ?: return
        if (!root.packageName.contains("paypal")) { sendBroadcast(Intent("H")); root.recycle(); return }
        val rects = mutableListOf<Rect>(); val amts = mutableListOf<String>()
        scan(root, rects, amts)
        if (rects.isNotEmpty()) sendBroadcast(Intent("S").apply { putParcelableArrayListExtra("r", ArrayList(rects)); putStringArrayListExtra("a", ArrayList(amts)) })
        root.recycle()
    }
    private fun scan(n: android.view.accessibility.AccessibilityNodeInfo, rects: MutableList<Rect>, amts: MutableList<String>) {
        val t = "${n.text} ${n.contentDescription}"
        Regex("""[\$]([\d.]+)""").find(t)?.value?.let { a ->
            if (r.any { it.containsMatchIn(t) }) {
                val rc = Rect(); n.getBoundsInScreen(rc)
                if (rc.width()>100) { rects.add(rc); amts.add(a) }
            }
        }
        for (i in 0 until n.childCount) n.getChild(i)?.let { scan(it, rects, amts); it.recycle() }
    }
    private fun load() { r.clear(); getSharedPreferences("s",0).getStringSet("p",setOf("onlyfans"))?.forEach{try{r.add(Regex(it,RegexOption.IGNORE_CASE))}catch(_:Exception){}} }
    override fun onInterrupt() {}
}

class OverlayService : Service() {
    private lateinit var wm: WindowManager
    private var c: FrameLayout? = null
    private val m = mutableMapOf<String, View>()
    private val rcv = object : BroadcastReceiver() {
        override fun onReceive(c: Context?, i: Intent?) {
            when (i?.action) {
                "S" -> show(i.getParcelableArrayListExtra("r")?:listOf(), i.getStringArrayListExtra("a")?:listOf())
                "H" -> hide()
            }
        }
    }
    override fun onCreate() {
        super.onCreate()
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(NotificationChannel("c","Privacy",NotificationManager.IMPORTANCE_LOW))
        }
        startForeground(1, NotificationCompat.Builder(this,"c").setContentTitle("Privacy Active").setSmallIcon(android.R.drawable.ic_secure).build())
        registerReceiver(rcv, IntentFilter().apply { addAction("S"); addAction("H") }, Context.RECEIVER_EXPORTED)
        c = FrameLayout(this)
        wm.addView(c, WindowManager.LayoutParams(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT, if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_SYSTEM_ALERT, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, PixelFormat.TRANSLUCENT))
    }
    private fun show(rects: List<Rect>, amts: List<String>) {
        val k = rects.mapIndexed { i, r -> "${r.left},${r.top}" }.toSet()
        m.keys.filter { it !in k }.forEach { c?.removeView(m[it]); m.remove(it) }
        rects.forEachIndexed { i, r ->
            val key = "${r.left},${r.top}"
            if (!m.containsKey(key)) {
                val v = FrameLayout(this).apply {
                    setBackgroundColor(Color.WHITE); elevation = 8f; setPadding(30, 15, 30, 15)
                    addView(TextView(this@OverlayService).apply { text = listOf("Starbucks","Amazon","Netflix","Uber").random(); textSize = 16f; setTextColor(Color.BLACK) })
                    addView(TextView(this@OverlayService).apply { text = amts.getOrNull(i)?:"$25"; textSize = 16f; setTextColor(Color.parseColor("#0070BA")); (layoutParams as FrameLayout.LayoutParams).gravity = Gravity.END or Gravity.CENTER_VERTICAL })
                }
                v.layoutParams = FrameLayout.LayoutParams(r.width(), r.height()).apply { leftMargin = r.left; topMargin = r.top }
                m[key] = v; c?.addView(v)
            }
        }
    }
    private fun hide() { m.values.forEach { c?.removeView(it) }; m.clear() }
    override fun onDestroy() { unregisterReceiver(rcv); c?.let { wm.removeView(it) }; super.onDestroy() }
    override fun onBind(i: Intent?): IBinder? = null
}
