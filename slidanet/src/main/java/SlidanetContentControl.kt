import android.content.Context
import android.graphics.Color
import android.graphics.Point
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.ViewManager
import android.widget.RelativeLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.GestureDetectorCompat
import org.json.JSONObject
import java.lang.IllegalArgumentException
import kotlin.math.absoluteValue

class SlidanetContentControl(val applicationContext: Context) : ConstraintLayout(applicationContext),
                                                                View.OnTouchListener,
                                                                GestureDetector.OnGestureListener,
                                                                GestureDetector.OnDoubleTapListener
{

    private var boxStartPosition = Point(0,0)
    private var boxEndPosition = Point(0,0)
    private var currentTouchPosition = Point(0, 0)
    private var lastTouchPosition = Point(0, 0)
    private var firstTapRegistered = false
    private var movingView = false
    private var firstContactTime: Long = 0
    private var secondTapRegistered = false
    private var secondContactTime: Long = 0
    private var subjectFrame = Point(0,0)
    private val controlFrame = Point((Slidanet.screenWidthInPixels * Slidanet.screenDensity).toInt(),
                                     (Slidanet.screenHeightInPixels * Slidanet.screenDensity).toInt())
    private var subjectOrigin = Point(0,0)
    private var subjectDiagonal = Point(0,0)
    private var locationInView: IntArray = intArrayOf(0, 0)
    private var gestureDetector = GestureDetectorCompat(context, this)
    private var lastNormalizedTranslationX = 0f
    private var lastNormalizedTranslationY = 0f
    private var lastBoxBeginX = 0f
    private var lastBoxBeginY = 0f

    init {

        this.layoutParams = LayoutParams(LayoutParams.MATCH_PARENT,
                                         LayoutParams.MATCH_PARENT)
        this.setOnTouchListener(this)
        gestureDetector.setOnDoubleTapListener(this)
    }

    internal fun initialize() {

        boxStartPosition = Point(0,0)
        boxEndPosition = Point(0,0)
        currentTouchPosition = Point(0, 0)
        lastTouchPosition = Point(0, 0)
        firstTapRegistered = false
        movingView = false
        firstContactTime= 0
        secondTapRegistered = false
        secondContactTime = 0
        subjectFrame = Point(0,0)
        subjectOrigin = Point(0,0)
        subjectDiagonal = Point(0,0)
        locationInView = intArrayOf(0, 0)
        lastNormalizedTranslationX = 0f
        lastNormalizedTranslationY = 0f
        lastBoxBeginX = 0f
        lastBoxBeginY = 0f

    }

    override fun onTouch(p0: View?, p1: MotionEvent?): Boolean {

        Slidanet.rendererHandler.post {

            p1?.let {

                for (i in 0 until it.pointerCount) {

                    when (it.actionMasked) {

                        MotionEvent.ACTION_DOWN,
                        MotionEvent.ACTION_POINTER_DOWN -> processPointerDown(p1)

                        MotionEvent.ACTION_UP,
                        MotionEvent.ACTION_POINTER_UP -> processPointerUp(p1)

                        MotionEvent.ACTION_MOVE -> processMove(p1)

                        else -> {}
                    }
                }
            }
        }

        return true
    }

    private fun processPointerDown(event: MotionEvent?) {

        event?.let {

            val x = it.getX(0).toInt()
            val y = it.getY(0).toInt()

            currentTouchPosition.x = x
            currentTouchPosition.y = y
            lastTouchPosition.x = x
            lastTouchPosition.y = y
            boxStartPosition.x = currentTouchPosition.x
            boxStartPosition.y = currentTouchPosition.y

            this.getLocationOnScreen(locationInView)
            boxStartPosition.y = (Slidanet.screenHeightInPixels * Slidanet.screenDensity).toInt() - boxStartPosition.y

            var viewPosition: IntArray = intArrayOf(0, 0)

            if (Slidanet.followerTakeInProgress) {

                Slidanet.slidanetContentAddresses[Slidanet.editorContentAddress]?.let {  followerObject ->

                    viewPosition = followerObject.getViewPosition()
                }

            } else if (Slidanet.ownerEditingInProgress) {

                Slidanet.contentInEditor?.let { ownerObject ->

                    viewPosition = ownerObject.getViewPosition()
                    subjectFrame.x = ownerObject.getTextureWidth()
                    subjectFrame.y = ownerObject.getTextureHeight()
                    subjectDiagonal.x = ownerObject.getTextureWidth()
                }
            }

            subjectOrigin.y = (controlFrame.y - (viewPosition[1] + subjectFrame.y)+locationInView[1]).toFloat().toInt()
            subjectDiagonal.y = subjectOrigin.y + subjectFrame.y

        }
    }

    private fun processPointerUp(event: MotionEvent?) {

        event?.let {

            boxEndPosition.x = it.getX(0).toInt()
            boxEndPosition.y = it.getY(0).toInt()

        }
    }

    private fun processMove(event: MotionEvent?) {

        event?.let {

            currentTouchPosition.x = it.getX(0).toInt()
            currentTouchPosition.y = it.getY(0).toInt()
            boxEndPosition.x = currentTouchPosition.x
            boxEndPosition.y = currentTouchPosition.y
            boxEndPosition.y = controlFrame.y - currentTouchPosition.y

            if (Slidanet.followerTakeInProgress) {

                Slidanet.slidanetContentAddresses[Slidanet.editorContentAddress]?.let { followerObject ->

                    Slidanet.mainHandler?.post {

                        if (!followerObject.getContentWasTaken()) {

                            Slidanet.requestId++
                            val request = JSONObject()
                            request.put(SlidanetConstants.slidanet_content_address, followerObject.getContentWasTaken())
                            Slidanet.requests[Slidanet.requestId] = SlidanetResponseData(requestCode = SlidanetRequestType.TakeContent,
                                                                                         requestInfo = request,
                                                                                         responseCode = SlidanetResponseType.Undefined)
                            Slidanet.server.takeContentAddress(requestId = Slidanet.requestId,
                                                               contentAddress = followerObject.getContentAddress())
                        }
                    }

                    movingView = true

                    when (followerObject.getShareMode()) {

                        SlidanetSharingStyleType.SlideAllDirections,
                        SlidanetSharingStyleType.SlideUpAndDown,
                        SlidanetSharingStyleType.SlideLeftAndRight -> processGeneralMove()

                        SlidanetSharingStyleType.PeekDefine -> processDefinePeek()

                        SlidanetSharingStyleType.PeekSlide -> processMovePeek()

                        else -> {}
                    }
                }

            } else {

                Slidanet.contentInEditor?.let { ownerObject ->

                    movingView = true

                    when (ownerObject.getShareMode()) {

                        SlidanetSharingStyleType.SlideAllDirections,
                        SlidanetSharingStyleType.SlideLeftAndRight,
                        SlidanetSharingStyleType.SlideUpAndDown -> processGeneralMove()

                        SlidanetSharingStyleType.PeekDefine -> processDefinePeek()

                        SlidanetSharingStyleType.PeekSlide -> processMovePeek()

                        else -> {}
                    }
                }
            }
        }
    }

    private fun processGeneralMove() {

        try {

            var w = 0f
            var h = 0f
            var editingScale = 1f

            var normalizedTranslationX = 0f
            var normalizedTranslationY = 0f

            if (Slidanet.ownerEditingInProgress) {

                w = requireNotNull(Slidanet.contentInEditor?.getTextureWidth()?.toFloat())
                h = requireNotNull(Slidanet.contentInEditor?.getTextureHeight()?.toFloat())
                editingScale = requireNotNull(Slidanet.contentInEditor?.getEditingScale())
                normalizedTranslationX = Slidanet.contentInEditor?.getNormalizedTranslationX()!!
                normalizedTranslationY = Slidanet.contentInEditor?.getNormalizedTranslationY()!!

            } else if (Slidanet.followerTakeInProgress) {

                Slidanet.slidanetContentAddresses[Slidanet.editorContentAddress]?.let {

                    w = requireNotNull(it.getTextureWidth().toFloat())
                    h = requireNotNull(it.getTextureHeight().toFloat())
                    editingScale = it.getEditingScale()
                    normalizedTranslationX = it.getNormalizedTranslationX()
                    normalizedTranslationY = it.getNormalizedTranslationY()

                }
            }

            if (w > 0f && h > 0f && editingScale > 0f) {

                val a = w / h

                val scaleDeltaX = 1f
                val scaleDeltaY = 1f

                if (lastTouchPosition.x == 0 && lastTouchPosition.y == 0) {

                    lastTouchPosition = currentTouchPosition
                }

                val viewNormalizedDeltaX =
                    ((currentTouchPosition.x - lastTouchPosition.x).toFloat() / controlFrame.x.toFloat()) * scaleDeltaX

                val viewNormalizedDeltaY =
                    ((currentTouchPosition.y - lastTouchPosition.y).toFloat() / controlFrame.y.toFloat()) * scaleDeltaY


                lastTouchPosition.x = currentTouchPosition.x
                lastTouchPosition.y = currentTouchPosition.y

                Slidanet.contentInEditor?.getShareMode().let {

                    if (it == SlidanetSharingStyleType.SlideLeftAndRight ||
                        it == SlidanetSharingStyleType.SlideAllDirections) {

                        normalizedTranslationX += viewNormalizedDeltaX
                        if (normalizedTranslationX < -1f) normalizedTranslationX = -1f
                        if (normalizedTranslationX > 1f) normalizedTranslationX = 1f
                    }

                    if (it == SlidanetSharingStyleType.SlideUpAndDown ||
                        it == SlidanetSharingStyleType.SlideAllDirections) {

                        normalizedTranslationY -= viewNormalizedDeltaY
                        if (normalizedTranslationY < -1f) normalizedTranslationY = -1f
                        if (normalizedTranslationY > 1f) normalizedTranslationY = 1f

                    }
                }

                if ((normalizedTranslationX != lastNormalizedTranslationX) ||normalizedTranslationY != lastNormalizedTranslationY) {

                    lastNormalizedTranslationX = normalizedTranslationX
                    lastNormalizedTranslationY = normalizedTranslationY

                    if (Slidanet.followerTakeInProgress) {

                        Slidanet.slidanetContentAddresses[Slidanet.editorContentAddress]?.let {followerObject ->

                            followerObject.setShareTranslationParameters(x = normalizedTranslationX,
                                                                         y = normalizedTranslationY,
                                                                         z = 1f)
                            followerObject.initializeVertices(normalizedTranslationX =  normalizedTranslationX,
                                                              normalizedTranslationY =  normalizedTranslationY)
                            followerObject.setDisplayNeedsUpdate(true)
                        }

                    } else if (Slidanet.ownerEditingInProgress) {

                        Slidanet.contentInEditor?.let { ownerObject ->

                            ownerObject.setShareTranslationParameters(x = normalizedTranslationX,
                                                                      y = normalizedTranslationY,
                                                                      z = 1f)
                            ownerObject.initializeVertices(normalizedTranslationX =  normalizedTranslationX,
                                                            normalizedTranslationY = normalizedTranslationY)
                            ownerObject.setDisplayNeedsUpdate(true)

                            if (ownerObject.getUpdateDuringEdit()) {

                                ownerObject.incrementMoveCount()
                                Slidanet.slidanetContentAddresses[ownerObject.getContentAddress()]?.let { subjectContentAddress ->

                                    subjectContentAddress.setShareTranslationParameters(x = normalizedTranslationX,
                                                                                        y = normalizedTranslationY,
                                                                                        z = 1f)
                                    subjectContentAddress.initializeVertices(normalizedTranslationX =  normalizedTranslationX,
                                                                             normalizedTranslationY =  normalizedTranslationY)
                                    subjectContentAddress.setDisplayNeedsUpdate(true)
                                    subjectContentAddress.incrementMoveCount()
                                    subjectContentAddress.distributeMove()
                                }
                            } else {

                                ownerObject.setMoveCount(1)
                            }
                        }
                    }
                }
            }

        } catch (e: IllegalArgumentException) {

        }
    }

    private fun processMovePeek() {

        if (boxStartPosition.x == boxEndPosition.x || boxStartPosition.y == boxEndPosition.y) {
            return
        }

        generateMaskBox()

    }

    private fun generateMaskBox() {

        try {

            Slidanet.contentInEditor?.let {

                val boxWidth = it.getBoxEndX() - it.getBoxBeginX()
                val boxHeight = it.getBoxEndY() - it.getBoxBeginY()
                var boxBeginX = it.getBoxBeginX()
                var boxEndX = it.getBoxEndX()
                var boxBeginY = it.getBoxBeginY()
                var boxEndY = it.getBoxEndY()

                if (boxWidth > 0 && boxHeight > 0) {

                    val a = boxWidth / boxHeight

                    val scaleDeltaX = boxWidth * a
                    val scaleDeltaY = boxWidth / a

                    if (lastTouchPosition.x == 0 && lastTouchPosition.y == 0) {
                        lastTouchPosition = currentTouchPosition
                    }

                    val viewNormalizedDeltaX =
                        ((currentTouchPosition.x - lastTouchPosition.x).toFloat() / controlFrame.x.toFloat()) * scaleDeltaX

                    val viewNormalizedDeltaY =
                        ((currentTouchPosition.y - lastTouchPosition.y).toFloat() / controlFrame.y.toFloat()) * scaleDeltaY

                    lastTouchPosition.x = currentTouchPosition.x
                    lastTouchPosition.y = currentTouchPosition.y

                    boxBeginX += viewNormalizedDeltaX
                    if (boxBeginX < 0f) boxBeginX = 0f
                    if (boxBeginX > 1f - boxWidth) boxBeginX = 1f - boxWidth
                    boxEndX = boxBeginX + boxWidth

                    boxBeginY -= viewNormalizedDeltaY
                    if (boxBeginY < 0f) boxBeginY = 0f
                    if (boxBeginY > 1f - boxHeight) boxBeginY = 1f - boxHeight
                    boxEndY = boxBeginY + boxHeight

                    if (boxBeginX != lastBoxBeginX || boxBeginY != lastBoxBeginY) {

                        lastBoxBeginX = boxBeginX
                        lastBoxBeginY = boxBeginY

                        if (Slidanet.followerTakeInProgress) {

                            Slidanet.slidanetContentAddresses[Slidanet.editorContentAddress]?.let { followerObject ->

                                followerObject.setShareBoxParameters(beginX = boxBeginX,
                                                                     beginY = boxBeginY,
                                                                     endX = boxEndX,
                                                                     endY = boxEndY)
                                followerObject.setDisplayNeedsUpdate(true)

                            }

                        } else if (Slidanet.ownerEditingInProgress) {

                            it.setShareBoxParameters(beginX = boxBeginX,
                                                     beginY = boxBeginY,
                                                     endX = boxEndX,
                                                     endY = boxEndY)
                            it.setDisplayNeedsUpdate(true)

                            if (it.getUpdateDuringEdit()) {

                                it.incrementMoveCount()

                                Slidanet.slidanetContentAddresses[it.getContentAddress()]?.let { ownerObject ->

                                    ownerObject.setShareBoxParameters(beginX = boxBeginX,
                                                                      beginY = boxBeginY,
                                                                      endX = boxEndX,
                                                                      endY = boxEndY)
                                    ownerObject.setDisplayNeedsUpdate(true)
                                    ownerObject.incrementMoveCount()
                                    ownerObject.distributeMove()
                                }
                            } else {
                                it.setMoveCount(1)
                            }
                        }
                    }
                }
            }

        } catch (e: IllegalArgumentException) {

        }
    }

    private fun processDefinePeek() {

        calculateBox()
        generateMaskBox()

    }

    override fun performClick(): Boolean {
        return super.performClick()
    }

    private fun rectanglesOverlap(leftSideR1: Point, rightSideR1: Point, leftSideR2: Point, rightSideR2: Point) : Boolean {

        if (leftSideR1.x > rightSideR2.x || leftSideR2.x > rightSideR1.x) {
            return false
        }

        if (leftSideR1.y > rightSideR2.y || leftSideR2.y > rightSideR1.y) {
            return false
        }

        return true
    }

    private fun calculateBox() {

        var startX: Int
        var endX: Int
        var startY: Int
        var endY: Int

        if (boxStartPosition.x >= boxEndPosition.x) {

            startX = boxEndPosition.x
            endX = boxStartPosition.x

            if (startX > controlFrame.x) {

                startX = controlFrame.x
            }

            if (startX < 0) {

                startX = 0
            }

        } else {

            startX = boxStartPosition.x
            endX = boxEndPosition.x

            if (endX > controlFrame.x) {

                endX = controlFrame.x
            }

            if (endX < 0) {

                endX = 0
            }
        }

        if (boxStartPosition.y >= boxEndPosition.y) {

            endY = boxStartPosition.y
            startY = boxEndPosition.y

            if (startY > controlFrame.y) {

                startY = controlFrame.y
            }

        } else {

            startY = boxStartPosition.y
            endY = boxEndPosition.y

            if (endY < 0) {

                endY = 0
            }
        }

        if (rectanglesOverlap(Point(startX, startY), Point(endX, endY), subjectOrigin, subjectDiagonal)) {

            Slidanet.contentInEditor?.let { editorView ->

                if (startX < subjectOrigin.x) {

                    startX = subjectOrigin.x
                }

                if (endX > subjectDiagonal.x) {

                    endX = subjectDiagonal.x
                }


                if (startY < subjectOrigin.y) {

                    startY = subjectOrigin.y
                }

                if (endY > subjectDiagonal.y) {

                    endY = subjectDiagonal.y
                }

                val normalizedBoxBeginX = (startX - subjectOrigin.x).toFloat() / subjectFrame.x.toFloat()
                val normalizedBoxBeginY = (startY - subjectOrigin.y).toFloat() / subjectFrame.y.toFloat()
                val normalizedBoxEndX = (endX - subjectOrigin.x).toFloat() / subjectFrame.x.toFloat()
                val normalizedBoxEndY = (endY - subjectOrigin.y).toFloat() / subjectFrame.y.toFloat()


                if (Slidanet.followerTakeInProgress) {

                    Slidanet.slidanetContentAddresses[Slidanet.editorContentAddress]?.let { followerObject ->

                        followerObject.setShareBoxParameters(beginX = normalizedBoxBeginX,
                                                             beginY = normalizedBoxBeginY,
                                                             endX = normalizedBoxEndX,
                                                             endY = normalizedBoxEndY)
                        followerObject.setDisplayNeedsUpdate(true)

                    }

                } else /*(Slidanet.ownerEditingInProgress)*/ {

                    Slidanet.contentInEditor?.let {

                        editorView.setShareBoxParameters(beginX = normalizedBoxBeginX,
                                                         beginY = normalizedBoxBeginY,
                                                         endX = normalizedBoxEndX,
                                                         endY = normalizedBoxEndY)
                        editorView.setDisplayNeedsUpdate(true)
                        editorView.initializeBoxDimensions(normalizedBoxBeginX, normalizedBoxBeginY, normalizedBoxEndX, normalizedBoxEndY)

                        if (editorView.getUpdateDuringEdit()) {

                            editorView.incrementMoveCount()

                            Slidanet.slidanetContentAddresses[editorView.getContentAddress()]?.let { ownerObject ->

                                ownerObject.setShareBoxParameters(beginX = normalizedBoxBeginX,
                                                                  beginY = normalizedBoxBeginY,
                                                                  endX = normalizedBoxEndX,
                                                                  endY = normalizedBoxEndY)
                                ownerObject.setDisplayNeedsUpdate(true)
                                ownerObject.incrementMoveCount()
                                ownerObject.distributeMove()
                            }
                        } else {

                            editorView.setMoveCount(1)
                        }
                    }
                }
            }
        }
    }

    override fun onDown(p0: MotionEvent?): Boolean {
        return true
    }

    override fun onShowPress(p0: MotionEvent?) {

    }

    override fun onSingleTapUp(p0: MotionEvent?): Boolean {
        return true
    }

    override fun onScroll(p0: MotionEvent?, p1: MotionEvent?, p2: Float, p3: Float): Boolean {
        return true
    }

    override fun onLongPress(p0: MotionEvent?) {
    }

    override fun onFling(p0: MotionEvent?, p1: MotionEvent?, p2: Float, p3: Float): Boolean {
        return true
    }

    override fun onSingleTapConfirmed(p0: MotionEvent?): Boolean {
        return true
    }

    override fun onDoubleTap(p0: MotionEvent?): Boolean {

            Slidanet.rendererHandler.post {

                if (Slidanet.followerTakeInProgress) {

                    Slidanet.slidanetContentAddresses[Slidanet.editorContentAddress]?.let {

                        if (it.getDoubleTapEditingEnabled()) {

                            Slidanet.mainHandler?.post { (Slidanet.editorContent?.parent as ViewManager).removeView(Slidanet.editorContent) }

                            it.distributeMove()
                            it.logEditingRequest()

                            Slidanet.followerTakeInProgress = false

                            Slidanet.editorContentAddress = ""
                        }
                    }

                } else if (Slidanet.ownerEditingInProgress) {

                    Slidanet.contentInEditor?.let {

                        if (it.getDoubleTapEditingEnabled()) {

                            Slidanet.mainHandler?.post { (Slidanet.editorContent?.parent as ViewManager).removeView(Slidanet.editorContent) }

                            it.distributeMove()
                            it.logEditingRequest()

                            Slidanet.ownerEditingInProgress = false

                            Slidanet.editorContentAddress = ""
                        }
                    }
                }
            }

        return true
    }

    override fun onDoubleTapEvent(p0: MotionEvent?): Boolean {

        return true
    }

}
