package com.example.socialapp

import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import android.view.textclassifier.TextClassifier.TYPE_EMAIL
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.textclassifier.TextClassifier
import androidx.constraintlayout.widget.ConstraintLayout


class OwnLegacyContentAdapter(): RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    class ImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val imageView: TextView = itemView.findViewById(R.id.legacyImageView)
        private val textView: TextView = itemView.findViewById(R.id.legacyTextView)

        fun bind(contentId: String, contentText: String) {
            textView.text = contentText
            //textView.setBackgroundColor(Color.RED)

            // load imageView from disk file (check width/height and adjust layout parameters)
        }
    }

    class VideoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val videoView: TextView = itemView.findViewById(R.id.legacyImageView)
        private val textView: TextView = itemView.findViewById(R.id.legacyTextView)

        fun bind(contentId: String, contentText: String) {

            textView.text = contentText
            // load videoView from disk file (check width/height and adjust layout parameters)
        }
    }

    class TextViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val textView: TextView = itemView.findViewById(R.id.legacyTextView)

        fun bind(contentText: String) {

            textView.text = contentText
            //textView.setBackgroundColor(Color.RED)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {

        var view = ConstraintLayout(SocialApp.applicationContext)

        when (viewType) {

            ContentType.Image.ordinal -> { view = LayoutInflater.from(SocialApp.applicationContext).inflate(R.layout.legacy_image_item, parent, false) as ConstraintLayout
                                           return ImageViewHolder(view) }
            ContentType.Video.ordinal -> { view = LayoutInflater.from(SocialApp.applicationContext).inflate(R.layout.legacy_video_item, parent, false) as ConstraintLayout
                                           return VideoViewHolder(view) }
            ContentType.Text.ordinal -> { view = LayoutInflater.from(SocialApp.applicationContext).inflate(R.layout.legacy_text_item, parent, false) as ConstraintLayout
                                          return TextViewHolder(view) }
        }

        return ImageViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {

        val content = SocialApp.socialContent[position]

        when (getItemViewType(position)) {

            ContentType.Image.ordinal -> (holder as ImageViewHolder).bind(content.contentId, content.text)
            ContentType.Video.ordinal -> (holder as VideoViewHolder).bind(content.contentId, content.text)
            ContentType.Text.ordinal -> (holder as TextViewHolder).bind(content.text)
        }
    }

    override fun getItemViewType(position: Int): Int {

        return SocialApp.socialContent[position].contentType.ordinal
    }

    override fun getItemCount() = SocialApp.socialContent.size
}