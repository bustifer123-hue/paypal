package com.paypalprivacy.app
import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.graphics.Rect
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class PayPalAccessibilityService : AccessibilityService() {
    private val patterns = mutableListOf<Regex>()
    private var lastScan = 0L
    
    override fun onServiceConnected() {
        super.onServiceConnected()
        loadPatterns()
    }
    
    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val now = System.currentTimeMillis()
        if (now - lastScan < 100) return
        lastScan = now
        
        val root = rootInActiveWindow ?: return
        
        if (!root.packageName.toString().contains("paypal")) {
            sendBroadcast(Intent("HIDE_OVERLAYS"))
            root.recycle()
            return
        }
        
        val rects = mutableListOf<Rect>()
        val amounts = mutableListOf<String>()
        
        scanForTransactions(root, rects, amounts)
        
        if (rects.isNotEmpty()) {
            sendBroadcast(Intent("SHOW_FAKE").apply {
                putParcelableArrayListExtra("rects", ArrayList(rects))
                putStringArrayListExtra("amounts", ArrayList(amounts))
            })
        }
        
        root.recycle()
    }
    
    private fun scanForTransactions(node: AccessibilityNodeInfo, rects: MutableList<Rect>, amounts: MutableList<String>) {
        val text = "${node.text} ${node.contentDescription}"
        val amount = Regex("""[\$€£]([\d,]+\.?\d{0,2})""").find(text)?.value
        
        if (amount != null && shouldHide(text)) {
            val rect = Rect()
            node.getBoundsInScreen(rect)
            if (rect.width() > 100 && rect.height() > 50) {
                rects.add(rect)
                amounts.add(amount)
            }
        }
        
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let {
                scanForTransactions(it, rects, amounts)
                it.recycle()
            }
        }
    }
    
    private fun shouldHide(text: String): Boolean {
        return patterns.any { it.containsMatchIn(text) }
    }
    
    private fun loadPatterns() {
        val prefs = getSharedPreferences("settings", MODE_PRIVATE)
        val words = prefs.getStringSet("patterns", setOf("onlyfans")) ?: setOf("onlyfans")
        patterns.clear()
        words.forEach { 
            try { patterns.add(Regex(it, RegexOption.IGNORE_CASE)) } catch(_: Exception) {}
        }
    }
    
    override fun onInterrupt() {}
}
