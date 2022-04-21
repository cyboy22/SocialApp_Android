package com.example.socialapp

import SlidanetResponseHandler
import org.json.JSONObject
import SlidanetResponseData
import SlidanetResponseType
import android.util.Log

class Slida: SlidanetResponseHandler {

    override fun slidanetResponse(responseData: SlidanetResponseData) {

        when (responseData.requestCode) {

            SlidanetRequestType.Connect -> {

                handleConnectToSlidanetResponse(responseData)
            }

            SlidanetRequestType.Disconnect -> {

                handleDisconnectFromSlidanetResponse(responseData)
            }

            SlidanetRequestType.ConnectContent -> {

                handleConnectToViewResponse(responseData)
            }

            SlidanetRequestType.EditContent -> {

                handleEditViewResponse(responseData)
            }

            else -> {}
        }
    }

    private fun handleConnectToSlidanetResponse(slidanetResponseData: SlidanetResponseData) {

        when (slidanetResponseData.responseCode) {

            SlidanetResponseType.ConnectionAuthenticated -> {

                if (SocialApp.activityTracker == ActivityTracker.OwnSlidanetContent) {

                    SocialApp.networkMessageHandler.refreshContent()
                }
            }

            else -> {}
        }
    }

    private fun handleDisconnectFromSlidanetResponse(slidanetResponseData: SlidanetResponseData) {

        when (slidanetResponseData.responseCode) {

            SlidanetResponseType.Disconnected -> {

                if (SocialApp.activityTracker == ActivityTracker.OwnLegacyContent) {

                    SocialApp.networkMessageHandler.refreshContent()
                }
            }

            else -> {}
        }
    }

    private fun handleConnectToViewResponse(slidanetResponseData: SlidanetResponseData) {

        when (slidanetResponseData.responseCode) {

            SlidanetResponseType.AppContentConnectedToSlidanetAddress -> {

                val requestData = slidanetResponseData.requestInfo
                val viewId = requestData.getString(SlidanetConstants.slidanet_content_address)

                for (content in SocialApp.socialContent) {

                    if (content.slidanetContentAddress == viewId) {

                        content.objectWidth = requestData.getInt(SlidanetConstants.object_width)
                        content.objectHeight = requestData.getInt(SlidanetConstants.object_height)
                        SocialApp.slidanetViews[viewId] = (slidanetResponseData.slidanetContentContainer)!!
                        val index = SocialApp.socialContent.indexOfFirst {

                            it.slidanetContentAddress == viewId
                        }

                        if (SocialApp.activityTracker == ActivityTracker.OwnSlidanetContent ||
                            SocialApp.activityTracker == ActivityTracker.FollowingSlidanetContent) {

                            SocialApp.slidanetCallbacks.refreshSlidanetContent(index)
                        }
                    }
                }
            }

            SlidanetResponseType.InvalidSlidanetContentAddressForPlatform -> {

            }

            SlidanetResponseType.SlidanetContentAddressNotFound -> {

            }

            else -> {}
        }
    }

    private fun handleEditViewResponse(slidanetResponseData: SlidanetResponseData) {

    }
}