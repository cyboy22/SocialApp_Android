package com.example.socialapp

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button

class OwnSlidanetContentActivity : AppCompatActivity(),
                                   NetworkMessageHandler,
                                   View.OnClickListener {

    private lateinit var postButton: Button
    private lateinit var followButton: Button
    private lateinit var legacyButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        supportActionBar?.hide()
        actionBar?.hide()

        initializeButton()

        setContentView(R.layout.activity_own_slidanet_content)
        SocialApp.networkMessageHandler = this

    }

    override fun onResume() {

        super.onResume()

        SocialApp.networkMessageHandler = this
        if (!SocialApp.connectedToServer) {
            SocialApp.socialServer = SocialServer()
            SocialApp.receiveMessageHandler.post { SocialApp.socialServer.connect() }
        }
    }



    fun initializeButton() {

        postButton = findViewById(R.id.postButton)
        followButton = findViewById(R.id.followButton)
        legacyButton = findViewById(R.id.legacyButton)
    }

    override fun onClick(p0: View?) {

        if (p0 == postButton) {
            postButtonClicked()
        } else if (p0 == followButton) {
            followButtonClicked()
        } else if (p0 == legacyButton) {
            slidanetButtonClicked()
        }
    }

    private fun slidanetButtonClicked() {

    }

    private fun postButtonClicked() {

    }

    private fun followButtonClicked() {

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