package com.example.socialapp

import android.content.DialogInterface
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AlertDialog

class FollowingLegacyContentActivity : AppCompatActivity(),
                                       NetworkMessageHandler,
                                       View.OnClickListener {

    private lateinit var unfollowButton: Button
    private lateinit var slidanetButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        supportActionBar?.hide()
        actionBar?.hide()
        initializeButtons()

        setContentView(R.layout.activity_following_legacy_content)
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

    private fun initializeButtons() {

        unfollowButton = findViewById(R.id.unfollowButton)
        unfollowButton.setOnClickListener(this)
        slidanetButton = findViewById(R.id.slidanetButton)
        slidanetButton.setOnClickListener(this)
    }

    private fun unfollowButtonClicked() {

    }

    private fun slidanetButtonClicked() {

    }

    override fun onClick(p0: View?) {

        if (p0 == unfollowButton) {

            unfollowButtonClicked()

        } else if (p0 == slidanetButton) {

            slidanetButtonClicked()
        }
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