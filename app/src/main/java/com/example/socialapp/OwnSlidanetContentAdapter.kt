package com.example.socialapp

import Slidanet
import SlidanetContentType
import SlidanetResponseType
import android.content.Context
import android.content.ContextWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.recyclerview.widget.RecyclerView
import java.io.File

class OwnSlidanetContentAdapter(): RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    class ImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val textView: TextView = TextView(SocialApp.applicationContext)
        private lateinit var slidanetView: ConstraintLayout

        fun bind(content: Content) {

            textView.text = content.text
            textView.id = View.generateViewId()
            slidanetView = SocialApp.slidanetViews[content.slidanetId]!!

            val objectWidth = content.objectWidth
            val objectHeight = content.objectHeight
            val aspectRatio = objectWidth.toFloat() / objectHeight.toFloat()
            val objectDisplayHeight = (SocialApp.screenHeight / Constants.rowSizeScaling).toInt()
            val objectDisplayWidth = (objectDisplayHeight * aspectRatio).toInt()
            val constraintLayout: ConstraintLayout = itemView as ConstraintLayout

            val constraintSet = ConstraintSet()
            constraintSet.clone(constraintLayout)
            slidanetView.layoutParams = ConstraintLayout.LayoutParams(objectDisplayWidth, objectDisplayHeight)
            textView.layoutParams = ConstraintLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                                                                  ViewGroup.LayoutParams.WRAP_CONTENT)
            constraintSet.connect(slidanetView.id, ConstraintSet.TOP, itemView.id, ConstraintSet.TOP, 0)
            constraintSet.connect(slidanetView.id, ConstraintSet.LEFT, itemView.id, ConstraintSet.LEFT, 0)
            constraintSet.connect(textView.id, ConstraintSet.LEFT, itemView.id, ConstraintSet.LEFT, 0)
            constraintSet.connect(textView.id, ConstraintSet.TOP, slidanetView.id, ConstraintSet.BOTTOM, 10)
            constraintSet.applyTo(constraintLayout)

            itemView.addView(slidanetView)
            itemView.addView(textView)
        }
    }

    class TextViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private lateinit var slidanetView: ConstraintLayout

        fun bind(content: Content) {

            slidanetView = SocialApp.slidanetViews[content.slidanetId]!!

            val objectWidth = content.objectWidth
            val objectHeight = content.objectHeight
            val aspectRatio = objectWidth.toFloat() / objectHeight.toFloat()
            val objectDisplayHeight = (SocialApp.screenHeight / Constants.rowSizeScaling).toInt()
            val objectDisplayWidth = (objectDisplayHeight * aspectRatio).toInt()
            val constraintLayout: ConstraintLayout = itemView as ConstraintLayout

            val constraintSet = ConstraintSet()
            constraintSet.clone(constraintLayout)
            slidanetView.layoutParams = ConstraintLayout.LayoutParams(objectDisplayWidth, objectDisplayHeight)
            constraintSet.connect(slidanetView.id, ConstraintSet.TOP, itemView.id, ConstraintSet.TOP, 0)
            constraintSet.connect(slidanetView.id, ConstraintSet.LEFT, itemView.id, ConstraintSet.LEFT, 0)
            constraintSet.applyTo(constraintLayout)

            itemView.addView(slidanetView)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {

        var view = ConstraintLayout(SocialApp.applicationContext)
        val t = View(SocialApp.applicationContext)

        when (viewType) {

            ContentType.Image.ordinal,
            ContentType.Video.ordinal -> { view = LayoutInflater.from(SocialApp.applicationContext).inflate(R.layout.slidanet_view_with_text,
                                                                      parent,
                                                                      false) as ConstraintLayout
                                          return ImageViewHolder(view) }

            ContentType.Text.ordinal -> { view = LayoutInflater.from(SocialApp.applicationContext).inflate(R.layout.slidanet_view,
                                                                     parent, false) as ConstraintLayout
                                          return TextViewHolder(view) }
        }

        return ImageViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {

        val content = SocialApp.socialContent[position]

        SocialApp.slidanetViews[content.slidanetId]?.let {

            when (getItemViewType(position)) {

                ContentType.Image.ordinal,
                ContentType.Video.ordinal -> (holder as ImageViewHolder).bind(content)
                ContentType.Text.ordinal -> (holder as TextViewHolder).bind(content)
            }
        } ?: kotlin.run {

            val path = File(ContextWrapper(SocialApp.applicationContext).getDir(Constants.contentDirectory,
                                                                                Context.MODE_PRIVATE), content.contentId).absolutePath

            when (Slidanet.connectContent(slidanetContentAddress = content.slidanetId,
                                          appContentPath = path)) {

                SlidanetResponseType.RequestSubmitted -> {

                    val sr = "Looks Good"
                }

                else -> {}
            }
        }
    }

    override fun getItemViewType(position: Int): Int {

        return SocialApp.socialContent[position].contentType.ordinal
    }

    override fun getItemCount() = SocialApp.socialContent.size

    private fun convertToSlidanetContentType(contentType: ContentType) : SlidanetContentType {

        val cType = SlidanetContentType.KImage

        when (contentType) {

            ContentType.Image -> SlidanetContentType.KImage
            ContentType.Video -> SlidanetContentType.KVideo
            ContentType.Text -> SlidanetContentType.KImage
        }

        return cType
    }

    fun initializeTextView(textView: TextView) {

    }

}