package com.example.socialapp

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button

class PostOptionsActivity : AppCompatActivity(),
                            NetworkMessageHandler,
                            View.OnClickListener {

    private lateinit var selectImageButton: Button
    private lateinit var selectVideoButton: Button
    private lateinit var composeTextButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        supportActionBar?.hide()
        actionBar?.hide()

        setContentView(R.layout.activity_post_options)
        SocialApp.networkMessageHandler = this

        initializeButtons()
    }

    override fun onClick(p0: View?) {

        if (p0 == selectImageButton) {
            selectImageButtonClicked()
        } else if (p0 == selectVideoButton) {
            selectVideoButtonClicked()
        } else if (p0 == composeTextButton) {
            composeTextButtonClicked()
        }
    }

    private fun initializeButtons() {

        selectImageButton = findViewById(R.id.selectImageButton)
        selectImageButton.setOnClickListener(this)
        selectVideoButton = findViewById(R.id.selectVideoButton)
        selectVideoButton.setOnClickListener(this)
        composeTextButton = findViewById(R.id.composeTextButton)
        composeTextButton.setOnClickListener(this)
    }

    private fun selectImageButtonClicked() {

    }

    private fun selectVideoButtonClicked() {

    }

    private fun composeTextButtonClicked() {

        SocialApp.activityTracker = ActivityTracker.ComposeText
        startActivity(SocialApp.activities[SocialApp.activityTracker])
    }

    override fun networkAlert(message: String) {
    }

    override fun initialize() {
    }

    override fun switchActivity(tracker: ActivityTracker) {
    }

    override fun refreshContent() {
    }
}