package com.example.socialapp

import android.content.DialogInterface
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.appcompat.app.AlertDialog

class SelectImageActivity : AppCompatActivity(),
                            NetworkMessageHandler {

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        supportActionBar?.hide()
        actionBar?.hide()

        setContentView(R.layout.activity_select_image)
    }

    private fun alert(message: String) {

        this.let {

            AlertDialog.Builder(this).apply {

                setMessage(message)
                setTitle("Social App")
                setPositiveButton("OK", DialogInterface.OnClickListener { _, _ ->
                    print("Hello")
                }).create().show()
            }
        }
    }

    override fun networkAlert(message: String) {

        alert(message)
    }

    override fun initialize() {

    }

    override fun switchActivity(tracker: ActivityTracker) {

    }

    override fun refreshContent() {
    }
}