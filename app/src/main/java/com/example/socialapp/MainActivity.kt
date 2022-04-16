package com.example.socialapp

import android.app.Activity
import android.graphics.Point
import android.os.Build
import android.os.Bundle
import android.util.DisplayMetrics
import androidx.appcompat.app.AppCompatActivity
import kotlin.math.roundToInt

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        supportActionBar?.hide()
        actionBar?.hide()

        screenSizeInDp.apply {
            SocialApp.screenWidth = x
            SocialApp.screenHeight = y
            SocialApp.density = resources.displayMetrics.density
        }

        SocialApp.sharedPreferences = getPreferences(MODE_PRIVATE)

        this.applicationContext.apply {
            SocialApp.loadContextData()
            SocialApp.applicationContext = this
            SocialApp.createIntents()
        }

        startActivity(SocialApp.activities[SocialApp.activityTracker])
    }
}

val Activity.displayMetrics: DisplayMetrics

    get() {

        val displayMetrics = DisplayMetrics()

        if (Build.VERSION.SDK_INT >= 30){
            display?.apply {
                getRealMetrics(displayMetrics)
            }
        } else {
            // getMetrics() method was deprecated in api level 30
            windowManager.defaultDisplay.getMetrics(displayMetrics)
        }

        return displayMetrics
    }

val Activity.screenSizeInDp: Point

    get() {

        val point = Point()
        displayMetrics.apply {
            val a = widthPixels
            val b = heightPixels
            point.x = (widthPixels / density).roundToInt()
            point.y = (heightPixels / density).roundToInt()
        }

        return point
    }