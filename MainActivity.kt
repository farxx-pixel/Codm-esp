package com.overlay.codm

import android.content.*
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private val OVERLAY_REQUEST = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 80, 48, 48)
        }

        val statusText = TextView(this).apply {
            text = "CODM ESP Overlay"
            textSize = 22f
        }

        val grantBtn = Button(this).apply {
            text = "1. Grant Overlay Permission"
            setOnClickListener { requestOverlayPermission() }
        }

        val rootBtn = Button(this).apply {
            text = "2. Grant Root (tap, then allow in popup)"
            setOnClickListener {
                Runtime.getRuntime().exec(arrayOf("su", "-c", "echo ok"))
            }
        }

        val startBtn = Button(this).apply {
            text = "3. Launch ESP"
            setOnClickListener {
                if (!Settings.canDrawOverlays(this@MainActivity)) {
                    Toast.makeText(this@MainActivity, "Grant overlay permission first", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                startService(Intent(this@MainActivity, OverlayService::class.java))
                Toast.makeText(this@MainActivity, "ESP started — check overlay", Toast.LENGTH_SHORT).show()
            }
        }

        val stopBtn = Button(this).apply {
            text = "Stop ESP"
            setOnClickListener {
                stopService(Intent(this@MainActivity, OverlayService::class.java))
            }
        }

        listOf(statusText, grantBtn, rootBtn, startBtn, stopBtn).forEach { layout.addView(it) }
        setContentView(layout)
    }

    private fun requestOverlayPermission() {
        val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:$packageName"))
        startActivityForResult(intent, OVERLAY_REQUEST)
    }
}
