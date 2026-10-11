// SPDX-License-Identifier: GPL-3.0-only
// Blutzucker-Blick: see LICENSE in the repository root.
package de.blutzuckerblick

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView

/** Kurzer Startbildschirm; gespeicherte Messwerte verbleiben in der Haupt-App. */
class IntroActivity : Activity() {
    private val handler = Handler(Looper.getMainLooper())
    private var opened = false
    private val openMain = Runnable {
        if (!opened && !isFinishing && !isDestroyed) {
            opened = true
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val dp = resources.displayMetrics.density
        window.statusBarColor = Color.rgb(247, 250, 250)
        window.navigationBarColor = Color.rgb(247, 250, 250)
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding((24 * dp).toInt(), 0, (24 * dp).toInt(), 0)
            setBackgroundColor(Color.rgb(247, 250, 250))
        }
        val logo = ImageView(this).apply {
            setImageResource(R.drawable.blood_drop_logo)
            scaleType = ImageView.ScaleType.FIT_CENTER
            contentDescription = "Roter Blutstropfen"
        }
        root.addView(logo, LinearLayout.LayoutParams((142 * dp).toInt(), (142 * dp).toInt()))
        root.addView(TextView(this).apply {
            text = "Blutzucker Blick"
            textSize = 27f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setTextColor(Color.rgb(20, 103, 123))
            setPadding(0, (25 * dp).toInt(), 0, (7 * dp).toInt())
        })
        root.addView(TextView(this).apply {
            text = "Messwerte & Auswertung"
            textSize = 17f
            gravity = Gravity.CENTER
            setTextColor(Color.rgb(81, 95, 97))
        })
        root.addView(TextView(this).apply {
            text = "Version 1.3.4"
            textSize = 14f
            gravity = Gravity.CENTER
            setTextColor(Color.rgb(81, 95, 97))
            setPadding(0, (10 * dp).toInt(), 0, 0)
        })
        root.setOnClickListener {
            handler.removeCallbacks(openMain)
            openMain.run()
        }
        setContentView(root)
        handler.postDelayed(openMain, 1900L)
    }

    override fun onDestroy() {
        handler.removeCallbacks(openMain)
        super.onDestroy()
    }
}
