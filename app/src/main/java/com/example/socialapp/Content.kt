package com.example.socialapp

data class Content(val contentId: String,
                   val contentType: ContentType,
                   val contentOwner: String,
                   val text: String,
                   var objectWidth: Int = 0,
                   var objectHeight: Int = 0,
                   val slidanetId: String = "")