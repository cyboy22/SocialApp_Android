package com.example.socialapp

import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class OwnSlidanetContentActivity : AppCompatActivity(),
                                   NetworkMessageHandler,
                                   SlidanetCallbacks,
                                   View.OnClickListener {

    private lateinit var postButton: Button
    private lateinit var followButton: Button
    private lateinit var legacyButton: Button
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: OwnSlidanetContentAdapter

    @SuppressLint("NotifyDataSetChanged")
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        supportActionBar?.hide()
        actionBar?.hide()

        setContentView(R.layout.activity_own_slidanet_content)
        initializeButton()
        SocialApp.networkMessageHandler = this
        SocialApp.slidanetCallbacks = this

        recyclerView = findViewById(R.id.slidanetOwnRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(applicationContext)
        adapter = OwnSlidanetContentAdapter()
        recyclerView.adapter = adapter
        adapter.notifyDataSetChanged()
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
        Slidanet.disconnect()
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

            val response = Slidanet.connect(applicationName = SocialApp.slidanetPlatformName,
                                            applicationPassword = SocialApp.slidanetPlatformPassword,
                                            slidaName = SocialApp.slidanetId,
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

    override fun refreshSlidanetContent(index: Int) {

        adapter.notifyItemChanged(index)
    }
}