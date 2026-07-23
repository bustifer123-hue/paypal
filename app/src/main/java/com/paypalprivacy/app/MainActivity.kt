package com.paypalprivacy.app
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 100, 50, 50)
        }
        layout.addView(TextView(this).apply {
            text = "PayPal Privacy Overlay"
            textSize = 28f
            setPadding(0, 0, 0, 50)
        })
        layout.addView(Button(this).apply {
            text = "1. Enable Overlay Permission"
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, 
                    Uri.parse("package:$packageName")))
            }
        })
        layout.addView(Button(this).apply {
            text = "2. Enable Accessibility Service"
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                Toast.makeText(context, "Find 'PayPal Privacy' and enable it", Toast.LENGTH_LONG).show()
            }
        })
        val input = EditText(this).apply {
            hint = "Words to hide: onlyfans, adult, $500"
            setPadding(0, 50, 0, 50)
        }
        layout.addView(input)
        layout.addView(Button(this).apply {
            text = "3. Save & Activate"
            setOnClickListener {
                val words = input.text.toString().split(",").map { it.trim() }.filter { it.isNotEmpty() }
                if (words.isEmpty()) {
                    Toast.makeText(context, "Enter at least one word", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                getSharedPreferences("settings", MODE_PRIVATE).edit()
                    .putStringSet("patterns", words.toSet()).apply()
                Toast.makeText(context, "Active! Open PayPal to test.", Toast.LENGTH_LONG).show()
            }
        })
        layout.addView(TextView(this).apply {
            text = "\nInstructions:\n1. Enable both permissions\n2. Enter sensitive words\n3. Open PayPal app"
            setPadding(0, 50, 0, 0)
            textSize = 14f
        })
        setContentView(layout)
    }
}
