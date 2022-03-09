package com.example.socialapp

import Slidanet
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.os.*
import android.text.InputType
import android.view.Gravity
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.RelativeLayout
import android.widget.TextView
import java.io.EOFException
import java.io.File
import java.io.File.separator
import java.io.FileOutputStream
import java.io.IOException
import java.net.UnknownHostException
import java.util.*


enum class ActivityTracker { Register,
                             FollowingLegacyContent,
                             OwnLegacyContent,
                             FollowingSlidanetContent,
                             OwnSlidanetContent,
                             PostOptions,
                             SelectImage,
                             SelectVideo,
                             ComposeText,
                             SelectMember }

enum class ContentType (val value: Int) { Image(0),
                                          Video(1),
                                          Text(2);
    companion object {
        private val VALUES = values()
        fun getByValue(value: Int) = VALUES.firstOrNull { it.value == value }
    }
}

enum class MessageType { AddMemberRequest,
                         AddMemberResponse,
                         AuthenticateMemberRequest,
                         AuthenticateMemberResponse,
                         FollowMemberRequest,
                         FollowMemberResponse,
                         UnfollowMemberRequest,
                         UnfollowMemberResponse,
                         GetContentListingRequest,
                         GetContentListingResponse,
                         GetContentRequest,
                         GetContentResponse,
                         AddContentRequest,
                         AddContentResponse,
                         DistributedContentRequest,
                         RemoveContentRequest,
                         RemoveContentResponse,
                         SwitchToSlidanetRequest,
                         SwitchToSlidanetResponse,
                         SwitchToLegacyRequest,
                         SwitchToLegacyResponse }

enum class ClientResponseType { Ok,
                                MemberNameAlreadyExists,
                                InvalidMemberName,
                                MemberAlreadyInAdvertCommunity,
                                MemberNotInAdvertCommunity,
                                InvalidMemberNameForId,
                                InvalidUserId,
                                ConnectionAuthenticated,
                                AuthenticationFailed,
                                ContentNotFound,
                                NotContentOwner,
                                UnableToSaveContent }

object Constants {

    const val slidanetId: String = "SLIDANET_ID"
    const val memberId: String = "MEMBER_ID"
    const val memberName: String = "MEMBER_NAME"
    const val followingName: String = "FOLLOWING_NAME"
    const val slidanetModeActive = "SLIDANET_MODE_ACTIVE"
    const val slidanetPlatformName: String = "SLIDANET_PLATFORM_NAME"
    const val slidanetPlatformId: String = "SLIDANET_PLATFORM_ID"
    const val slidanetPlatformPassword: String = "SLIDANET_PLATFORM_PASSWORD"
    const val networkMessageSizeLength: Int = 10
    const val networkMessageTypeLength: Int = 5
    const val serverIpAddress: String = "192.168.0.7"
    const val serverIpPort: Int = 4000
    const val networkReconnectInterval = 3.0
    const val networkMessageHeaderLength = networkMessageSizeLength + networkMessageTypeLength
    const val floatWidth: Int = 10
    const val integerWidth: Int = 10
    const val shortWidth: Int = 5
    const val nameWidth = 2
    const val flagWidth = 1
    const val uuidWidth = 32
    const val memberNameTextSize = 15.0F
    const val memberNamePlaceholder = "choose username"
    const val spacesNotAllowed = "username cannot contain spaces"
    const val usernameIsEmpty = "username cannot be empty"
    const val usernameMustBeAlphanumeric = "username must be alphanumeric"
    const val usernameEditTextHeight = 50
    const val serverWriteThread = "Server Write Thread"
    const val serverReadThread = "Server Read Thread"
    const val keystorePassword = "slidanet"
    const val usernameAlreadyExists = " already exists"
    const val invalidUserId = "invalid user ID"
    const val invalidMemberNameForMemberId = "invalid member name for member ID"
    const val textIndent = 5
}

 object SocialApp {

     var screenWidth: Int = 0
     var screenHeight: Int = 0
     var density: Float = 0.0F
     lateinit var applicationContext: Context
     var activityTracker: ActivityTracker = ActivityTracker.Register
     lateinit var slidanetId: String
     lateinit var slidanetIpAddress: String
     var slidanetIpPort: Int = 0
     var memberId = ""
     var memberName = ""
     private var followingName = ""
     lateinit var slidanetPlatformName: String
     lateinit var slidanetPlatformId: String
     lateinit var slidanetPlatformPassword: String
     var slidanetServiceIpAddress = ""
     var slidanetServiceIpPort: Int = 0
     lateinit var socialServer: SocialServer
     var connectedToServer: Boolean = false
     val activities = mutableMapOf<ActivityTracker, Intent>()
     var mainHandler: Handler? = Handler(Looper.getMainLooper())
     private var serverReadThread = HandlerThread(Constants.serverReadThread,
                                                  Process.THREAD_PRIORITY_BACKGROUND)
     private var serverWriteThread = HandlerThread(Constants.serverWriteThread,
                                                   Process.THREAD_PRIORITY_BACKGROUND)
     var sendMessageHandler: Handler
     var receiveMessageHandler: Handler
     lateinit var networkMessageHandler: NetworkMessageHandler
     val locale: Locale = Locale.ENGLISH
     lateinit var sharedPreferences: SharedPreferences
     var socialContent = ArrayList<Content>()
     var slidanetModeActive = false
     private var totalContentCount = 0
     private var actualContentCount = 0
     var listingsDownloadComplete = false
     val slida = Slida()

     init {
       serverReadThread.start()
       receiveMessageHandler = Handler(serverReadThread.looper)
       serverWriteThread.start()
       sendMessageHandler = Handler(serverWriteThread.looper)
     }

     fun loadContextData() {

        sharedPreferences.let {
            slidanetId = it.getString(Constants.slidanetId, "").toString()
            memberId = it.getString(Constants.memberId, "").toString()
            memberName = it.getString(Constants.memberName, "").toString()
            followingName = it.getString(Constants.followingName, "").toString()
            slidanetPlatformName = it.getString(Constants.slidanetPlatformName, "").toString()
            slidanetPlatformId = it.getString(Constants.slidanetPlatformId, "").toString()
            slidanetPlatformPassword = it.getString(Constants.slidanetPlatformPassword, "").toString()
            slidanetModeActive = it.getBoolean(Constants.slidanetModeActive, false)
        }

         activityTracker = if (memberId.isEmpty()) {
             ActivityTracker.Register
         } else if (followingName.isNotEmpty()){
             if (slidanetModeActive) {
                 ActivityTracker.FollowingSlidanetContent
             } else {
                 ActivityTracker.FollowingLegacyContent
             }
         } else {
             if (slidanetModeActive) {
                 ActivityTracker.OwnSlidanetContent
             } else {
                 ActivityTracker.OwnLegacyContent
             }
         }
    }

     fun connectToServer() {

         if (!connectedToServer) {
             socialServer = SocialServer()
             receiveMessageHandler.post { socialServer.connect() }
         }
     }

    fun createEditableTextField(context: Context?,
                                width: Int,
                                height: Int,
                                placeholder: String,
                                backgroundColor: Int,
                                listener: TextView.OnEditorActionListener) : EditText {

       val editText = EditText(context)

       val originX: Int = (0.5F *(screenWidth - width)).toInt()
       val originY = (0.5F * (screenHeight - height) * density).toInt()
       val layoutParams: RelativeLayout.LayoutParams
         = RelativeLayout.LayoutParams((width * density).toInt(), (height * density).toInt() )

       layoutParams.leftMargin = originX
       layoutParams.topMargin = originY
       editText.setBackgroundColor(backgroundColor)
       editText.hint = placeholder
       editText.textSize = Constants.memberNameTextSize
       editText.typeface = Typeface.DEFAULT
       editText.inputType = InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
       editText.gravity = Gravity.CENTER
       editText.layoutParams = layoutParams
       editText.setImeActionLabel("Done", KeyEvent.KEYCODE_ENTER)
       editText.imeOptions = EditorInfo.IME_ACTION_DONE
       editText.maxLines = 1
       editText.inputType = InputType.TYPE_CLASS_TEXT
       editText.setOnEditorActionListener(listener)

       return editText
    }

     fun createIntents() {

         activities[ActivityTracker.Register] = Intent(applicationContext, RegisterActivity::class.java)
         activities[ActivityTracker.FollowingLegacyContent] = Intent(applicationContext, FollowingLegacyContentActivity::class.java)
         activities[ActivityTracker.OwnLegacyContent] = Intent(applicationContext, OwnLegacyContentActivity::class.java)
         activities[ActivityTracker.FollowingSlidanetContent] = Intent(applicationContext, FollowingSlidanetContentActivity::class.java)
         activities[ActivityTracker.OwnSlidanetContent] = Intent(applicationContext, OwnSlidanetContentActivity::class.java)
         activities[ActivityTracker.PostOptions] = Intent(applicationContext, PostOptionsActivity::class.java)
         activities[ActivityTracker.SelectImage] = Intent(applicationContext, SelectImageActivity::class.java)
         activities[ActivityTracker.SelectVideo] = Intent(applicationContext, SelectVideoActivity::class.java)
         activities[ActivityTracker.ComposeText] = Intent(applicationContext, ComposeTextActivity::class.java)
     }

    fun isLettersOrDigits(chars: String): Boolean {
       return chars.matches("^[a-zA-Z0-9]*$".toRegex())
    }


     fun booleanToInt(b: Boolean): Int {
         return if (b) 1 else 0
     }

     fun processServerMessage(messageType: MessageType, message: ByteArray) {

       try {
          when (messageType) {

             MessageType.AddMemberResponse -> processAddMemberResponse(message)
              MessageType.AuthenticateMemberResponse -> processAuthenticateMemberResponse(message)
              MessageType.GetContentListingResponse -> processGetContentListingResponse(message)
              MessageType.GetContentResponse -> processGetContentResponse(message)
              MessageType.FollowMemberResponse -> processFollowMemberResponse(message)
              MessageType.AddContentResponse -> processAddContentResponse(message)


              else -> {}
          }
       } catch (e: Exception) {

       }
    }

    @Throws(
       UnknownHostException::class,
       IOException::class,
       EOFException::class,
       SecurityException::class,
       IllegalArgumentException:: class,
       NullPointerException::class)
    fun processAddMemberResponse(message: ByteArray) {

       SocialAppMessage(message).apply {
          requireNotNull(this.getInteger(Constants.shortWidth)).let { rc_it ->
             when (ClientResponseType.values()[rc_it]) {

                ClientResponseType.Ok -> {
                    memberId = requireNotNull(this.getString(Constants.uuidWidth))
                    val platformNameLength = requireNotNull(this.getInteger(Constants.nameWidth))
                    slidanetPlatformName = requireNotNull(this.getString(platformNameLength))
                    slidanetPlatformId = requireNotNull(this.getString(Constants.uuidWidth))
                    slidanetPlatformPassword = requireNotNull(this.getString(Constants.uuidWidth))
                    val memberNameLength = requireNotNull(this.getInteger(Constants.nameWidth))
                    memberName = requireNotNull(this.getString(memberNameLength))
                    slidanetId = requireNotNull(this.getString(Constants.uuidWidth))

                    sharedPreferences.edit()?.let {
                        it.putString(Constants.memberId, memberId)
                        it.putString(Constants.slidanetPlatformName, slidanetPlatformName)
                        it.putString(Constants.slidanetPlatformId, slidanetPlatformId)
                        it.putString(Constants.slidanetPlatformPassword, slidanetPlatformPassword)
                        it.putString(Constants.memberName, memberName)
                        it.putString(Constants.slidanetId, slidanetId)
                        it.commit()
                    }

                    activityTracker = ActivityTracker.OwnLegacyContent
                    mainHandler?.post { networkMessageHandler.switchActivity(activityTracker) }
                }

                ClientResponseType.MemberNameAlreadyExists -> {
                    mainHandler?.post { networkMessageHandler.networkAlert(Constants.usernameAlreadyExists) }
                }

                else -> {}
             }
          }
       }
    }

     @Throws(
         UnknownHostException::class,
         IOException::class,
         EOFException::class,
         SecurityException::class,
         IllegalArgumentException:: class,
         NullPointerException::class)
     fun processFollowMemberResponse(message: ByteArray) {

         SocialAppMessage(message).apply {
             requireNotNull(this.getInteger(Constants.shortWidth)).let { rc_it ->
                 when (ClientResponseType.values()[rc_it]) {

                     ClientResponseType.Ok -> {
                         val followedMemberNameLength = requireNotNull(this.getInteger(Constants.nameWidth))
                         followingName = requireNotNull(this.getString(followedMemberNameLength))

                         activityTracker = if (slidanetModeActive) {
                             ActivityTracker.FollowingSlidanetContent
                         } else {
                             ActivityTracker.FollowingLegacyContent
                         }

                         mainHandler?.post { networkMessageHandler.switchActivity(activityTracker) }
                     }

                     ClientResponseType.InvalidMemberName -> {
                         val followedMemberNameLength = requireNotNull(this.getInteger(Constants.nameWidth))
                         val name = requireNotNull(this.getString(followedMemberNameLength))
                         val errorMessage = "could not find $name"
                         mainHandler?.post { networkMessageHandler.networkAlert(errorMessage) }
                     }

                     else -> {}
                 }
             }
         }
     }

     @Throws(
         UnknownHostException::class,
         IOException::class,
         EOFException::class,
         SecurityException::class,
         IllegalArgumentException:: class,
         NullPointerException::class)
     fun processAuthenticateMemberResponse(message: ByteArray) {

         SocialAppMessage(message).apply {
             requireNotNull(this.getInteger(Constants.shortWidth)).let { rc_it ->
                 when (ClientResponseType.values()[rc_it]) {

                     ClientResponseType.ConnectionAuthenticated -> {
                         val memberNameLength = requireNotNull(this.getInteger(Constants.nameWidth))
                         memberName = requireNotNull(this.getString(memberNameLength))
                         memberId = requireNotNull(this.getString(Constants.uuidWidth))
                         slidanetId = requireNotNull(this.getString(Constants.uuidWidth))
                         val platformNameLength = requireNotNull(this.getInteger(Constants.nameWidth))
                         slidanetPlatformName = requireNotNull(this.getString(platformNameLength))
                         slidanetPlatformId = requireNotNull(this.getString(Constants.uuidWidth))
                         slidanetPlatformPassword = requireNotNull(this.getString(Constants.uuidWidth))
                         val slidanetServiceIpAddressLength = requireNotNull(this.getInteger(Constants.nameWidth))
                         slidanetServiceIpAddress = requireNotNull(this.getString(slidanetServiceIpAddressLength))
                         slidanetServiceIpPort = requireNotNull(this.getInteger(Constants.integerWidth))

                         sharedPreferences.edit()?.let {
                             it.putString(Constants.memberId, memberId)
                             it.putString(Constants.slidanetPlatformName, slidanetPlatformName)
                             it.putString(Constants.slidanetPlatformId, slidanetPlatformId)
                             it.putString(Constants.slidanetPlatformPassword, slidanetPlatformPassword)
                             it.putString(Constants.memberName, memberName)
                             it.putString(Constants.slidanetId, slidanetId)
                             it.commit()
                         }

                         mainHandler?.post { networkMessageHandler.initialize() }
                     }

                     ClientResponseType.InvalidUserId -> {
                         mainHandler?.post { networkMessageHandler.networkAlert(Constants.invalidUserId) }
                     }

                     ClientResponseType.InvalidMemberNameForId -> {
                         mainHandler?.post { networkMessageHandler.networkAlert(Constants.invalidMemberNameForMemberId) }
                     }

                     else -> {}
                 }
             }
         }
     }

     @Throws(
         UnknownHostException::class,
         IOException::class,
         EOFException::class,
         SecurityException::class,
         IllegalArgumentException:: class,
         NullPointerException::class)
     fun processGetContentListingResponse(message: ByteArray) {

         SocialAppMessage(message).apply {
             requireNotNull(this.getInteger(Constants.shortWidth)).let { rc_it ->
                 when (ClientResponseType.values()[rc_it]) {

                     ClientResponseType.Ok -> {
                         totalContentCount = requireNotNull(this.getInteger(Constants.integerWidth))

                         repeat(totalContentCount) {
                             val contentId = requireNotNull(this.getString(Constants.uuidWidth))
                             val contentType = requireNotNull(this.getInteger(Constants.nameWidth))
                             if (contentType != ContentType.Text.ordinal) {
                                 val file = File(Environment.getDataDirectory().toString() + separator.toString() + contentId)
                                 if (!file.exists()) {
                                     socialServer.getContentRequest(contentId)
                                 } else {
                                     actualContentCount++
                                 }
                             } else {

                                 actualContentCount++
                             }
                             val contentOwnerLength = requireNotNull(this.getInteger(Constants.nameWidth))
                             val contentOwner = requireNotNull(this.getString(contentOwnerLength))
                             val slidanetViewIdLength = requireNotNull(this.getInteger(Constants.nameWidth))
                             val slidanetViewId = requireNotNull(this.getString(slidanetViewIdLength))
                             val textLength = requireNotNull(this.getInteger(Constants.integerWidth))
                             var text = ""
                             if (textLength > 0) {
                                 text = requireNotNull(this.getString(textLength))

                                 if (contentType == ContentType.Text.ordinal) {
                                     val file = File(contentId)

                                     val cw = ContextWrapper(applicationContext)
                                     val directory = cw.getDir("SocialAppContent", Context.MODE_PRIVATE)
                                     if (!directory.exists()) {
                                         directory.mkdir()
                                     }

                                     val bitmapFile = File(directory, contentId)
                                     if (!bitmapFile.exists()) {
                                         val bitmap = Slidanet.textToImage(text,
                                             Typeface.DEFAULT,
                                             14.0F,Color.BLACK, Color.WHITE,
                                             density,Constants.textIndent,
                                             applicationContext)
                                         storeImage(bitmap, contentId)
                                     }
                                 }
                             }

                             socialContent.add(Content(contentId,
                                                       ContentType.getByValue(contentType)!!,
                                                       contentOwner,
                                                       text,
                                                       slidanetViewId))
                         }

                         listingsDownloadComplete = true

                         if (totalContentCount == actualContentCount) {
                             if (activityTracker != ActivityTracker.OwnSlidanetContent && activityTracker != ActivityTracker.FollowingSlidanetContent) {
                                 mainHandler?.post { networkMessageHandler.refreshContent() }
                             }
                         }
                     }
                     else -> {}
                 }
             }
         }
     }

     @Throws(
         UnknownHostException::class,
         IOException::class,
         EOFException::class,
         SecurityException::class,
         IllegalArgumentException:: class,
         NullPointerException::class)
     fun processGetContentResponse(message: ByteArray) {

         SocialAppMessage(message).apply {
             requireNotNull(this.getInteger(Constants.shortWidth)).let { rc_it ->
                 when (ClientResponseType.values()[rc_it]) {

                     ClientResponseType.Ok -> {

                         val contentId = requireNotNull(this.getString(Constants.uuidWidth))
                         val contentLength = requireNotNull(this.getInteger(Constants.integerWidth))
                         val content = requireNotNull(this.getContent(contentLength))
                         val file = File(Environment.getDataDirectory().toString() + separator.toString() + contentId)
                         file.writeBytes(content)
                         actualContentCount++

                         if (totalContentCount == actualContentCount) {
                             mainHandler?.post { networkMessageHandler.refreshContent() }
                         }
                     }

                     ClientResponseType.ContentNotFound -> {

                     }

                     else -> {}
                 }
             }
         }
     }

     @Throws(
         UnknownHostException::class,
         IOException::class,
         EOFException::class,
         SecurityException::class,
         IllegalArgumentException:: class,
         NullPointerException::class)
     fun processAddContentResponse(message: ByteArray) {

         SocialAppMessage(message).apply {
             requireNotNull(this.getInteger(Constants.shortWidth)).let { rc_it ->
                 when (ClientResponseType.values()[rc_it]) {

                     ClientResponseType.Ok -> {

                         val contentType = requireNotNull(this.getInteger(Constants.nameWidth))
                         var contextId = ""
                         if (ContentType.getByValue(contentType) != ContentType.Text) {
                             contextId = requireNotNull(this.getString(Constants.uuidWidth))
                             // find file with name contextId
                             // rename to contentId
                         }

                         val contentId = requireNotNull(this.getString(Constants.uuidWidth))
                         val textLength = requireNotNull(this.getInteger(Constants.integerWidth))
                         val text = requireNotNull(this.getString(textLength))
                         val slidanetViewId = requireNotNull(this.getString(Constants.uuidWidth))

                         socialContent.add(Content(contentId,
                                           ContentType.getByValue(contentType)!!,
                                           memberId,
                                           text,
                                           slidanetViewId))
                         mainHandler?.post { networkMessageHandler.switchActivity(ActivityTracker.OwnLegacyContent) }
                     }

                     else -> {}
                 }
             }
         }
     }

     private fun storeImage(image: Bitmap, contentId: String) {

         try {
             val cw = ContextWrapper(applicationContext)
             val directory = cw.getDir("SocialAppContent", Context.MODE_PRIVATE)
             if (!directory.exists()) {
                 directory.mkdir()
             }

             val path = File(directory, contentId)
             val fos = FileOutputStream(path);
             image.compress(Bitmap.CompressFormat.PNG, 100, fos)
             fos.close()
         } catch (e: java.lang.Exception) {
             e.printStackTrace()
         }
     }
 }

interface NetworkMessageHandler {
    fun networkAlert(message: String)
    fun initialize()
    fun switchActivity(tracker: ActivityTracker)
    fun refreshContent()
}

fun createUUID() : String {
    return UUID.randomUUID().toString()
}


