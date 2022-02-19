package com.example.socialapp

import android.annotation.SuppressLint
import android.content.DialogInterface
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.KeyEvent
import android.widget.EditText
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog

class SelectMemberActivity : AppCompatActivity(),
                             TextView.OnEditorActionListener,
                             DialogInterface.OnClickListener,
                             NetworkMessageHandler {

    private var layoutContainer: RelativeLayout? = null
    private var memberNameField: EditText? = null

    @SuppressLint("ResourceAsColor")
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        supportActionBar?.hide()
        actionBar?.hide()

        SocialApp.applicationContext.let {
            createLayout()

            memberNameField = SocialApp.createEditableTextField(
                it,
                SocialApp.screenWidth,
                Constants.usernameEditTextHeight,
                Constants.memberNamePlaceholder,
                Color.WHITE,
                this
            )
        }

        SocialApp.networkMessageHandler = this
        SocialApp.activityTracker = ActivityTracker.SelectMember

        memberNameField?.let {
            layoutContainer?.addView(it)
        }
    }

    override fun onResume() {

        super.onResume()

        SocialApp.networkMessageHandler = this
        if (!SocialApp.connectedToServer) {
            SocialApp.socialServer = SocialServer()
            SocialApp.receiveMessageHandler.post { SocialApp.socialServer.connect() }
        }
    }

    override fun onStart() {

        super.onStart()
        SocialApp.networkMessageHandler = this
    }

    override fun onUserLeaveHint() {

        super.onUserLeaveHint()
        SocialApp.sendMessageHandler.post { SocialApp.socialServer.disconnect() }
    }

    private fun createLayout() {

        layoutContainer = RelativeLayout(this)
        val containerLayoutParams: RelativeLayout.LayoutParams
                = RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.MATCH_PARENT,
            RelativeLayout.LayoutParams.MATCH_PARENT)
        layoutContainer?.layoutParams = containerLayoutParams

        setContentView(layoutContainer)
        layoutContainer?.setBackgroundColor(Color.YELLOW)
    }

    override fun onEditorAction(p0: TextView?, p1: Int, p2: KeyEvent?): Boolean {

        if (p0 == memberNameField) {
            if (p0?.text != null) {
                if (p0.text.contains(" ")) {
                    alert(Constants.spacesNotAllowed)
                    p0.text = ""
                } else if (p0.text.isEmpty()) {
                    alert(Constants.usernameIsEmpty)
                } else if (!SocialApp.isLettersOrDigits(p0.text.toString())) {
                    alert(Constants.usernameMustBeAlphanumeric)
                } else {
                    p0.text.toString().let {
                        it.lowercase()
                        SocialApp.sendMessageHandler.post {
                            SocialApp.socialServer.followMemberRequest(it)
                        }
                    }
                }
            }
        }

        return true
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

    override fun onClick(p0: DialogInterface?, p1: Int) {

    }

    override fun networkAlert(message: String) {
        alert(message)
    }

    override fun initialize() {

    }

    override fun switchActivity(tracker: ActivityTracker) {

        startActivity(SocialApp.activities[SocialApp.activityTracker])
    }

    override fun refreshContent() {
    }
}