package com.example.socialapp

import android.content.DialogInterface
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import java.text.SimpleDateFormat
import java.util.*

class ComposeTextActivity : AppCompatActivity(),
                            View.OnClickListener,
                            NetworkMessageHandler {

    private lateinit var doneButton: Button
    private lateinit var cancelButton: Button
    private lateinit var editText: EditText

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        supportActionBar?.hide()
        actionBar?.hide()

        setContentView(R.layout.activity_compose_text)

        initializeButtons()

        editText = findViewById(R.id.editText)
        SocialApp.networkMessageHandler = this
    }

    private fun initializeButtons() {

        doneButton = findViewById(R.id.doneButton)
        doneButton.setOnClickListener(this)
        cancelButton = findViewById(R.id.cancelButton)
        cancelButton.setOnClickListener(this)
    }

    override fun onClick(p0: View?) {

        if (p0 == doneButton) {
            doneButtonClicked()
        } else if (p0 == cancelButton) {
            cancelButtonClicked()
        }
    }

    private fun doneButtonClicked() {

        SocialApp.socialServer.addContentRequest(Content("",
                                                 ContentType.Text, "",
                                                 editText.text.toString(),""))
    }

    private fun cancelButtonClicked() {

        SocialApp.activityTracker = ActivityTracker.PostOptions
        startActivity(SocialApp.activities[SocialApp.activityTracker])
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

        SocialApp.activityTracker = tracker
        startActivity(SocialApp.activities[SocialApp.activityTracker])

    }

    override fun refreshContent() {
    }
}