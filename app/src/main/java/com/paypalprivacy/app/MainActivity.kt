package com.paypalprivacy.app
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.*
class MainActivity : Activity() {
    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val l = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(50,100,50,50) }
        l.addView(TextView(this).apply{text="PayPal Privacy";textSize=28f})
        l.addView(Button(this).apply{text="Enable Overlay";setOnClickListener{startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,Uri.parse("package:$packageName")))}})
        l.addView(Button(this).apply{text="Enable Accessibility";setOnClickListener{startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))}})
        val e = EditText(this).apply{hint="Words to hide: onlyfans, adult"}
        l.addView(e)
        l.addView(Button(this).apply{text="Save";setOnClickListener{getSharedPreferences("s",0).edit().putStringSet("p",e.text.toString().split(",").map{it.trim()}.toSet()).apply();Toast.makeText(this,"Done",1).show()}})
        setContentView(l)
    }
}
