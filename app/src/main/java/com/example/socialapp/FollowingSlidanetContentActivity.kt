package com.example.socialapp

import android.annotation.SuppressLint
import android.app.Activity
import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.ViewManager
import android.widget.Button
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView


class FollowingSlidanetContentActivity : AppCompatActivity(),
                                         NetworkMessageHandler,
                                         SlidanetCallbacks,
                                         View.OnClickListener {

    private lateinit var unfollowButton: Button
    private lateinit var legacyButton: Button
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: FollowingSlidanetContentAdapter

    @SuppressLint("NotifyDataSetChanged")
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        supportActionBar?.hide()
        actionBar?.hide()
        initializeButtons()
        setContentView(R.layout.activity_following_slidanet_content)
        SocialApp.networkMessageHandler = this
        SocialApp.slidanetCallbacks = this

        recyclerView = findViewById(R.id.slidanetOwnRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(applicationContext)
        adapter = FollowingSlidanetContentAdapter()
        recyclerView.adapter = adapter
        adapter.notifyDataSetChanged()

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
        legacyButton = findViewById(R.id.legacyButton)
    }

    private fun unfollowButtonClicked() {

    }

    private fun legacyButtonClicked() {

    }

    override fun onClick(p0: View?) {

        if (p0 == unfollowButton) {

            unfollowButtonClicked()

        } else if (p0 == legacyButton) {

            legacyButtonClicked()
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

    override fun addEditor(editorView: ConstraintLayout) {

        val parentView = findViewById<ConstraintLayout>(android.R.id.content)
        val viewWidth = parentView.width
        val viewHeight = parentView.height
        parentView.addView(editorView)

    }

    override fun removeEditor(editorView: ConstraintLayout) {

        (editorView.parent as ViewManager).removeView(editorView)
    }

    override fun refreshSlidanetContent(index: Int) {

        adapter.notifyItemChanged(index)
    }
}