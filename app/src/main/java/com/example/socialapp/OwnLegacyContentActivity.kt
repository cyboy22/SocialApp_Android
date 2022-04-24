package com.example.socialapp

import android.annotation.SuppressLint
import android.content.DialogInterface
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.TextureView
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class OwnLegacyContentActivity : AppCompatActivity(),
                                 NetworkMessageHandler,
                                 View.OnClickListener {

    private lateinit var postButton: Button
    private lateinit var followButton: Button
    private lateinit var slidanetButton: Button
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: OwnLegacyContentAdapter

    @SuppressLint("NotifyDataSetChanged")
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        supportActionBar?.hide()
        actionBar?.hide()

        setContentView(R.layout.activity_own_legacy_content)
        SocialApp.networkMessageHandler = this
        initializeButtons()

        recyclerView = findViewById(R.id.legacyOwnRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(applicationContext)
        adapter = OwnLegacyContentAdapter()
        recyclerView.adapter = adapter
        adapter.notifyDataSetChanged()

    }

    override fun onResume() {

        super.onResume()

        SocialApp.networkMessageHandler = this
        SocialApp.connectToServer()
    }

    override fun onClick(p0: View?) {

        if (p0 == postButton) {

            postButtonClicked()

        } else if (p0 == followButton) {

            followButtonClicked()

        } else if (p0 == slidanetButton) {

            slidanetButtonClicked()
        }
    }

    private fun initializeButtons() {

        postButton = findViewById(R.id.postButton)
        postButton.setOnClickListener(this)

        followButton = findViewById(R.id.followButton)
        followButton.setOnClickListener(this)

        slidanetButton = findViewById(R.id.slidanetButton)
        slidanetButton.setOnClickListener(this)
    }

    private fun slidanetButtonClicked() {

        if (Slidanet.isConnected()) {

            SocialApp.activityTracker = ActivityTracker.OwnSlidanetContent
            startActivity(SocialApp.activities[SocialApp.activityTracker])
        }
    }

    private fun postButtonClicked() {

        SocialApp.activityTracker = ActivityTracker.PostOptions
        startActivity(SocialApp.activities[SocialApp.activityTracker])
    }

    private fun followButtonClicked() {

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

        if (!SocialApp.listingsDownloadComplete) {

            SocialApp.sendMessageHandler.post {

                SocialApp.socialServer.getContentListingRequest()
            }
        }
    }

    override fun switchActivity(tracker: ActivityTracker) {
    }

    override fun refreshContent() {

        adapter.notifyDataSetChanged()
    }

}