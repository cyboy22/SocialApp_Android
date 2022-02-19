package com.example.socialapp

data class Content(val contentId: String,
                   val contentType: ContentType,
                   val contentOwner: String,
                   val text: String,
                   val slidanetId: String)