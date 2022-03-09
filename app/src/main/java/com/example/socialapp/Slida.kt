package com.example.socialapp

import SlidanetResponseHandler
import org.json.JSONObject
import SlidanetResponseData
import SlidanetResponseType
import android.util.Log

class Slida: SlidanetResponseHandler {

    override fun slidanetResponse(responseData: SlidanetResponseData) {

        when (responseData.requestCode) {
            SlidanetRequestType.KConnectToNetwork -> {
                handleConnectToSlidanetResponse(responseData)
            }

            SlidanetRequestType.KDisconnectFromNetwork -> {
                handleDisconnectFromSlidanetResponse(responseData)
            }

            else -> {}
        }
    }

    private fun handleConnectToSlidanetResponse(slidanetResponseData: SlidanetResponseData) {

        when (slidanetResponseData.responseCode) {

            SlidanetResponseType.KConnectionAuthenticated -> {
                if (SocialApp.activityTracker == ActivityTracker.OwnSlidanetContent) {
                    SocialApp.networkMessageHandler.refreshContent()
                }
            }

            else -> {}
        }
    }

    private fun handleDisconnectFromSlidanetResponse(slidanetResponseData: SlidanetResponseData) {

        when (slidanetResponseData.responseCode) {

            SlidanetResponseType.KDisconnected_ -> {

                if (SocialApp.activityTracker == ActivityTracker.OwnLegacyContent) {
                    SocialApp.networkMessageHandler.refreshContent()
                }
            }

            else -> {}
        }
    }
}