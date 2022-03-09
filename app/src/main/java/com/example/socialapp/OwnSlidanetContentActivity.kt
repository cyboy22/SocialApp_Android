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

        setContentView(R.layout.activity_own_slidanet_content)
        initializeButton()
        SocialApp.networkMessageHandler = this
    }

    override fun onResume() {

        super.onResume()

        SocialApp.networkMessageHandler = this
        if (!SocialApp.connectedToServer) {
            SocialApp.socialServer = SocialServer()
            SocialApp.receiveMessageHandler.post { SocialApp.socialServer.connect() }
        } else {
            initialize()
        }
    }

    override fun onStop() {
        super.onStop()
    }

    private fun initializeButton() {

        postButton = findViewById(R.id.postButton)
        postButton.setOnClickListener(this)

        followButton = findViewById(R.id.followButton)
        followButton.setOnClickListener(this)

        legacyButton = findViewById(R.id.legacyButton)
        legacyButton.setOnClickListener(this)
    }

    override fun onClick(p0: View?) {

        if (p0 == postButton) {
            postButtonClicked()
        } else if (p0 == followButton) {
            followButtonClicked()
        } else if (p0 == legacyButton) {
            legacyButtonClicked()
        }
    }

    private fun legacyButtonClicked() {

        SocialApp.activityTracker = ActivityTracker.OwnLegacyContent
        startActivity(SocialApp.activities[SocialApp.activityTracker])

    }

    override fun onPause() {
        super.onPause()
        Slidanet.disconnectFromNetwork()
    }

    private fun postButtonClicked() {

        SocialApp.activityTracker = ActivityTracker.PostOptions
        startActivity(SocialApp.activities[SocialApp.activityTracker])
    }

    private fun followButtonClicked() {

    }

    override fun networkAlert(message: String) {
    }

    override fun initialize() {

        if (!SocialApp.listingsDownloadComplete) {
            SocialApp.sendMessageHandler.post {
                SocialApp.socialServer.getContentListingRequest()
            }
        }

        if (!Slidanet.isConnected()) {

            val response = Slidanet.connectToNetwork(platformName = SocialApp.slidanetPlatformName,
                platformId = SocialApp.slidanetPlatformId,
                platformPassword = SocialApp.slidanetPlatformPassword,
                userId = SocialApp.slidanetId,
                ipAddress = SocialApp.slidanetServiceIpAddress,
                ipPort = SocialApp.slidanetServiceIpPort,
                appContext = SocialApp.applicationContext,
                responseHandler = SocialApp.slida)
        }
    }

    override fun switchActivity(tracker: ActivityTracker) {
    }

    override fun refreshContent() {

        val check = ""
    }
}