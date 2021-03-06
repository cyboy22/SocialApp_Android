package com.example.socialapp

import android.annotation.SuppressLint
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.ViewManager
import android.widget.Button
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class OwnSlidanetContentActivity : AppCompatActivity(),
                                   NetworkMessageHandler,
                                   SlidanetCallbacks,
                                   View.OnClickListener {

    private lateinit var postButton: Button
    private lateinit var followButton: Button
    private lateinit var legacyButton: Button
    private lateinit var slideButton: Button
    private lateinit var peekButton: Button
    private lateinit var peekDefineButton: Button
    private lateinit var peekSlideButton: Button
    private lateinit var slideLeftAndRightButton: Button
    private lateinit var slideUpAndDownButton: Button
    private lateinit var slideAllDirectionsButton: Button
    private lateinit var cancelEditingButton: Button
    private lateinit var applyEditingButton: Button
    private lateinit var peekOptionsLayout: ConstraintLayout
    private lateinit var slideOptionsLayout: ConstraintLayout
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

        peekOptionsLayout = ConstraintLayout(this)
        peekOptionsLayout.id = View.generateViewId()

        slideOptionsLayout = ConstraintLayout(this)
        slideOptionsLayout.id = View.generateViewId()

        postButton = findViewById(R.id.postButton)
        postButton.id = View.generateViewId()
        postButton.setOnClickListener(this)

        followButton = findViewById(R.id.followButton)
        followButton.id = View.generateViewId()
        followButton.setOnClickListener(this)

        legacyButton = findViewById(R.id.legacyButton)
        legacyButton.id = View.generateViewId()
        legacyButton.setOnClickListener(this)

        slideButton = Button(this)
        slideButton.setOnClickListener(this)
        slideButton.transformationMethod = null
        slideButton.background = null
        slideButton.id = View.generateViewId()
        slideButton.setText(R.string.slideButtonText)

        peekButton = Button(this)
        peekButton.setOnClickListener(this)
        peekButton.transformationMethod = null
        peekButton.background = null
        peekButton.id = View.generateViewId()
        peekButton.setText(R.string.peekButtonText)

        peekDefineButton = Button(this)
        peekDefineButton.setOnClickListener(this)
        peekDefineButton.transformationMethod = null
        peekDefineButton.background = null
        peekDefineButton.id = View.generateViewId()
        peekDefineButton.setText(R.string.peekDefineButtonText)

        peekSlideButton = Button(this)
        peekSlideButton.setOnClickListener(this)
        peekSlideButton.transformationMethod = null
        peekSlideButton.background = null
        peekSlideButton.id = View.generateViewId()
        peekSlideButton.setText(R.string.peekSlideButtonText)

        cancelEditingButton = Button(this)
        cancelEditingButton.setOnClickListener(this)
        cancelEditingButton.transformationMethod = null
        cancelEditingButton.background = null
        cancelEditingButton.id = View.generateViewId()
        cancelEditingButton.setText(R.string.cancelEditingButtonText)
        cancelEditingButton.setTextColor(Color.WHITE)

        applyEditingButton = Button(this)
        applyEditingButton.setOnClickListener(this)
        applyEditingButton.transformationMethod = null
        applyEditingButton.background = null
        applyEditingButton.id = View.generateViewId()
        applyEditingButton.setText(R.string.applyEditingButtonText)
        applyEditingButton.setTextColor(Color.WHITE)

        slideLeftAndRightButton = Button(this)
        slideLeftAndRightButton.setOnClickListener(this)
        slideLeftAndRightButton.transformationMethod = null
        slideLeftAndRightButton.background = null
        slideLeftAndRightButton.id = View.generateViewId()
        slideLeftAndRightButton.setText(R.string.slideLeftAndRightButtonText)
        slideLeftAndRightButton.setTextColor(Color.WHITE)

        slideUpAndDownButton = Button(this)
        slideUpAndDownButton.setOnClickListener(this)
        slideUpAndDownButton.transformationMethod = null
        slideUpAndDownButton.background = null
        slideUpAndDownButton.id = View.generateViewId()
        slideUpAndDownButton.setText(R.string.slideUpAndDownButtonText)
        slideUpAndDownButton.setTextColor(Color.WHITE)

        slideAllDirectionsButton = Button(this)
        slideAllDirectionsButton.setOnClickListener(this)
        slideAllDirectionsButton.transformationMethod = null
        slideAllDirectionsButton.background = null
        slideAllDirectionsButton.id = View.generateViewId()
        slideAllDirectionsButton.setText(R.string.slideAllDirectionsButtonText)
        slideAllDirectionsButton.setTextColor(Color.WHITE)

    }

    override fun onClick(p0: View?) {

        when (p0) {

            postButton -> postButtonClicked()
            followButton -> followButtonClicked()
            legacyButton -> legacyButtonClicked()
            slideButton -> slideButtonClicked()
            peekButton -> peekButtonClicked()
            peekDefineButton -> peekDefineButtonClicked()
            peekSlideButton -> peekSlideButtonClicked()
            cancelEditingButton -> cancelEditingButtonClicked()
            applyEditingButton -> applyEditingButtonClicked()
            slideLeftAndRightButton -> slideLeftAndRightButtonClicked()
            slideUpAndDownButton -> slideUpAndDownButtonClicked()
            slideAllDirectionsButton -> slideAllDirectionsButtonClicked()
            else -> {}

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

    private fun slideButtonClicked() {

    }

    private fun peekButtonClicked() {

    }

    private fun peekDefineButtonClicked() {

        val response = Slidanet.setSharingStyle(slidanetContentAddress = SocialApp.slidanetEditContentAddress,
                                                slidanetSharingStyle = SlidanetSharingStyleType.PeekDefine,
                                                boxBeginX = 0f,
                                                boxBeginY =  0f,
                                                boxEndX = 0f,
                                                boxEndY = 0f)

    }

    private fun peekSlideButtonClicked() {

        val response = Slidanet.setSharingStyle(slidanetContentAddress = SocialApp.slidanetEditContentAddress,
                                                slidanetSharingStyle = SlidanetSharingStyleType.PeekSlide,
                                                boxBeginX = 0f,
                                                boxBeginY =  0f,
                                                boxEndX = 0f,
                                                boxEndY = 0f)
    }

    private fun slideLeftAndRightButtonClicked() {

        val response = Slidanet.setSharingStyle(slidanetContentAddress = SocialApp.slidanetEditContentAddress,
                                                slidanetSharingStyle = SlidanetSharingStyleType.SlideLeftAndRight,
                                                x = 1f,
                                                y = 0f)

    }

    private fun slideUpAndDownButtonClicked() {

        val response = Slidanet.setSharingStyle(slidanetContentAddress = SocialApp.slidanetEditContentAddress,
                                                slidanetSharingStyle = SlidanetSharingStyleType.SlideUpAndDown,
                                                x = 0f,
                                                y = -1f)
    }

    private fun slideAllDirectionsButtonClicked() {

        val response = Slidanet.setSharingStyle(slidanetContentAddress = SocialApp.slidanetEditContentAddress,
                                                slidanetSharingStyle = SlidanetSharingStyleType.SlideAllDirections,
                                                x = 1f,
                                                y = 1f)
    }

    private fun cancelEditingButtonClicked() {

        val response = Slidanet.cancelContentEditing(SocialApp.slidanetEditContentAddress)

    }

    private fun applyEditingButtonClicked() {

        val response = Slidanet.commitContentEditing(SocialApp.slidanetEditContentAddress)

    }

    override fun networkAlert(message: String) {

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

    }

    override fun addEditor(editorView: ConstraintLayout) {

        val contentView: ConstraintLayout = findViewById<ConstraintLayout>(R.id.slidanet_own_content)
        contentView.addView(editorView)
        initializeEditingButtons()

    }

    override fun removeEditor(editorView: ConstraintLayout) {

        (editorView.parent as ViewManager).removeView(editorView)

    }

    override fun refreshSlidanetContent(index: Int) {

        adapter.notifyItemChanged(index)
    }

    private fun muteEditingButtons() {

        slideButton.setTextColor(Constants.buttonMuteColor)
        peekButton.setTextColor(Constants.buttonMuteColor)
        peekDefineButton.setTextColor(Constants.buttonMuteColor)
        peekSlideButton.setTextColor(Constants.buttonMuteColor)
        slideLeftAndRightButton.setTextColor(Constants.buttonMuteColor)
        slideUpAndDownButton.setTextColor(Constants.buttonMuteColor)
        slideAllDirectionsButton.setTextColor(Constants.buttonMuteColor)

    }

    private fun layoutEditingButtons() {

        SocialApp.slidanetEditorView.addView(peekButton)
        SocialApp.slidanetEditorView.addView(slideButton)
        SocialApp.slidanetEditorView.addView(cancelEditingButton)
        SocialApp.slidanetEditorView.addView(applyEditingButton)
        SocialApp.slidanetEditorView.addView(peekDefineButton)
        SocialApp.slidanetEditorView.addView(peekSlideButton)
        SocialApp.slidanetEditorView.addView(peekOptionsLayout)
        SocialApp.slidanetEditorView.addView(slideLeftAndRightButton)
        SocialApp.slidanetEditorView.addView(slideUpAndDownButton)
        SocialApp.slidanetEditorView.addView(slideAllDirectionsButton)

        val constraintSet = ConstraintSet()
        constraintSet.clone(SocialApp.slidanetEditorView)

        constraintSet.connect(peekButton.id, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP)
        constraintSet.connect(peekButton.id, ConstraintSet.LEFT, ConstraintSet.PARENT_ID, ConstraintSet.LEFT)

        constraintSet.connect(peekDefineButton.id, ConstraintSet.TOP, peekButton.id, ConstraintSet.BOTTOM)
        constraintSet.connect(peekDefineButton.id, ConstraintSet.LEFT, ConstraintSet.PARENT_ID, ConstraintSet.LEFT)

        constraintSet.connect(peekSlideButton.id, ConstraintSet.TOP, peekDefineButton.id, ConstraintSet.BOTTOM)
        constraintSet.connect(peekSlideButton.id, ConstraintSet.LEFT, ConstraintSet.PARENT_ID, ConstraintSet.LEFT)

        constraintSet.connect(slideLeftAndRightButton.id, ConstraintSet.TOP, slideButton.id, ConstraintSet.BOTTOM)
        constraintSet.connect(slideLeftAndRightButton.id, ConstraintSet.RIGHT, ConstraintSet.PARENT_ID, ConstraintSet.RIGHT)

        constraintSet.connect(slideUpAndDownButton.id, ConstraintSet.TOP, slideLeftAndRightButton.id, ConstraintSet.BOTTOM)
        constraintSet.connect(slideUpAndDownButton.id, ConstraintSet.RIGHT, ConstraintSet.PARENT_ID, ConstraintSet.RIGHT)

        constraintSet.connect(slideAllDirectionsButton.id, ConstraintSet.TOP, slideUpAndDownButton.id, ConstraintSet.BOTTOM)
        constraintSet.connect(slideAllDirectionsButton.id, ConstraintSet.RIGHT, ConstraintSet.PARENT_ID, ConstraintSet.RIGHT)

        constraintSet.connect(slideButton.id, ConstraintSet.RIGHT, ConstraintSet.PARENT_ID, ConstraintSet.RIGHT)
        constraintSet.connect(peekButton.id, ConstraintSet.LEFT, ConstraintSet.PARENT_ID, ConstraintSet.LEFT)

        constraintSet.connect(cancelEditingButton.id, ConstraintSet.LEFT, ConstraintSet.PARENT_ID, ConstraintSet.LEFT)
        constraintSet.connect(cancelEditingButton.id, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM)

        constraintSet.connect(applyEditingButton.id, ConstraintSet.RIGHT, ConstraintSet.PARENT_ID, ConstraintSet.RIGHT)
        constraintSet.connect(applyEditingButton.id, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM)
        constraintSet.applyTo(SocialApp.slidanetEditorView)

    }

    override fun initializeEditingButtons() {

        muteEditingButtons()

        when (SocialApp.slidanetSharingStyle) {

            SlidanetSharingStyleType.SlideAllDirections -> {
                slideAllDirectionsButton.setTextColor(Constants.buttonActiveColor)
                slideButton.setTextColor(Constants.buttonActiveColor)
            }

            SlidanetSharingStyleType.SlideLeftAndRight -> {
                slideLeftAndRightButton.setTextColor(Constants.buttonActiveColor)
                slideButton.setTextColor(Constants.buttonActiveColor)
            }

            SlidanetSharingStyleType.SlideUpAndDown -> {
                slideUpAndDownButton.setTextColor(Constants.buttonActiveColor)
                slideButton.setTextColor(Constants.buttonActiveColor)
            }

            SlidanetSharingStyleType.PeekDefine -> {
                peekDefineButton.setTextColor(Constants.buttonActiveColor)
                peekButton.setTextColor(Constants.buttonActiveColor)
            }

            SlidanetSharingStyleType.PeekSlide -> {
                peekSlideButton.setTextColor(Constants.buttonActiveColor)
                peekButton.setTextColor(Constants.buttonActiveColor)
            }

            else -> {}

        }

        layoutEditingButtons()
    }
}