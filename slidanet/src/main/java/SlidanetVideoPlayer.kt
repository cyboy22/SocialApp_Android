import android.media.*
import android.text.TextUtils
import android.util.Log
import android.view.Surface
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.nio.ByteBuffer
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

internal class SlidanetVideoPlayer(val outputSurface: Surface,
                                   val audioEnabled: Boolean = true,
                                   private val callback: SlidanetVideoManager) {

    private val timeoutUsec = 10000L // 10msec
    private val debug = true
    private val tagStatic = "MediaMoviePlayer:"
    private val tag = tagStatic + javaClass.simpleName
    private var isRunning = false
    private var state = 0
    private var request = 0
    private var requestTime = 0L
    private var videoTrackIndex = -1
    private var audioTrackIndex = -1
    private var hasAudio = false
    private var videoStartTime: Long = 0
    private var videoMediaCodec: MediaCodec? = null
    private var audioMediaCodec: MediaCodec? = null
    private var videoMediaExtractor: MediaExtractor? = null
    private var audioMediaExtractor: MediaExtractor? = null
    private var videoBufferInfo: MediaCodec.BufferInfo? = null
    private var audioBufferInfo: MediaCodec.BufferInfo? = null
    private var videoInputBuffers: Array<ByteBuffer>? = emptyArray()
    private var videoOutputBuffers: Array<ByteBuffer>? = emptyArray()
    private var audioInputBuffers: Array<ByteBuffer>? = emptyArray()
    private var audioOutputBuffers: Array<ByteBuffer>? = emptyArray()
    private var audioChannels = 0
    private var audioSampleRate = 0
    private var audioInputBufSize = 0
    private var audioStartTime: Long = 0
    private var previousVideoPresentationTimeUs: Long = -1
    private var previousAudioPresentationTimeUs: Long = -1
    private var videoWidth = 0
    private var videoHeight = 0
    private var bitrate = 0
    private var frameRate = 0f
    private var rotation = 0
    private var duration = 0L
    private val lock = ReentrantLock()
    private val lockCondition = lock.newCondition()
    private val videoTaskLock = ReentrantLock()
    private val videoTaskLockCondition = videoTaskLock.newCondition()
    private val audioTaskLock = ReentrantLock()
    private val audioTaskLockCondition = audioTaskLock.newCondition()
    @Volatile private var mVideoInputDone = false
    @Volatile private var mVideoOutputDone = false
    @Volatile private var audioInputDone = false
    @Volatile private var audioOutputDone = false
    private var metadata: MediaMetadataRetriever? = null
    private var audioTrack: AudioTrack? = null
    private var audioOutTempBuf: ByteArray? = null
    private lateinit var sourcePath: String

    private val moviePlayerTask = Runnable {

        var localIsRunning = false
        var localReq = 0

        try {
            lock.withLock {

                isRunning = true
                localIsRunning = isRunning
                state = Constants.stateStop
                request = Constants.reqNon
                requestTime = -1
                lockCondition.signalAll()
            }

            while (localIsRunning) {

                try {
                    lock.withLock {

                        localIsRunning = isRunning
                        localReq = request
                        request = Constants.reqNon
                    }

                    if (localIsRunning) {

                        when (state) {
                            Constants.stateStop -> localIsRunning = processStop(localReq)
                            Constants.statePrepared -> localIsRunning = processPrepared(localReq)
                            Constants.statePlaying -> localIsRunning = processPlaying(localReq)
                            Constants.statePaused -> localIsRunning = processPaused(localReq)
                        }
                    }
                } catch (e: InterruptedException) {
                    break
                } catch (e: Exception) {
                    Log.e(tag, "MoviePlayerTask:", e)
                    break
                }
            }
        } finally {
            if (debug) Log.v(tag, "player task finished:local_isRunning=$localIsRunning")
            handleStop()
        }
    }

    private val videoTask = Runnable {

        if (debug) Log.v(tag, "VideoTask:start")

        while (isRunning && !mVideoInputDone && !mVideoOutputDone) {

            try {
                if (!mVideoInputDone) {
                    handleInputVideo()
                }
                if (!mVideoOutputDone) {
                    handleOutputVideo(callback)
                }
            } catch (e: java.lang.Exception) {
                Log.e(tag, "VideoTask:", e)
                break
            }
        }

        if (debug) Log.v(tag, "VideoTask:finished")

        videoTaskLock.withLock {

            mVideoOutputDone = true
            mVideoInputDone = mVideoOutputDone
            videoTaskLockCondition.signalAll()
        }
    }

    private val audioTask = Runnable {

        if (debug) Log.v(tag, "AudioTask:start")

        while (isRunning && !audioInputDone && !audioOutputDone) {

            try {

                if (!audioInputDone) {
                    handleInputAudio()
                }

                if (!audioOutputDone) {
                    handleOutputAudio(callback)
                }
            } catch (e: java.lang.Exception) {
                Log.e(tag, "VideoTask:", e)
                break
            }
        }

        if (debug) Log.v(tag, "AudioTask:finished")

        audioTaskLock.withLock {

            audioOutputDone = true
            audioInputDone = audioOutputDone
            audioTaskLockCondition.signalAll()
        }
    }

    init {
        if (debug) Log.v(tag, "Constructor:")

        Thread(moviePlayerTask, tag).start()

        lock.withLock {

            try {
                if (!isRunning) lockCondition.await()
            } catch (e: InterruptedException) {
            }
        }
    }

    fun getWidth(): Int {
        return videoWidth
    }

    fun getHeight(): Int {
        return videoHeight
    }

    fun getBitRate(): Int {
        return bitrate
    }

    fun getFramerate(): Float {
        return frameRate
    }

    fun getRotation(): Int {
        return rotation
    }

    fun getDurationUs(): Long {
        return duration
    }

    fun getSampleRate(): Int {
        return audioSampleRate
    }

    fun hasAudio(): Boolean {
        return hasAudio
    }

    fun prepare(srcVideo: String) {

        if (debug) Log.v(tag, "prepare:")

        lock.withLock {

            sourcePath = srcVideo
            request = Constants.reqPrepare
            lockCondition.signalAll()
        }
    }

    fun play() {

        if (debug) Log.v(tag, "play:")

        lock.withLock {

            if (state == Constants.statePlaying) return
            request = Constants.reqStart
            lockCondition.signalAll()
        }
    }

    fun seek(newTime: Long) {

        if (debug) Log.v(tag, "seek")

        lock.withLock {

            request = Constants.reqSeek
            requestTime = newTime
            lockCondition.signalAll()
        }
    }

    private fun stop() {

        if (debug) Log.v(tag, "stop:")

        lock.withLock {

            if (state != Constants.stateStop) {

                request = Constants.reqStop
                lockCondition.signalAll()

                try {
                    lockCondition.await(50, TimeUnit.MILLISECONDS)
                } catch (e: InterruptedException) { // ignore
                }
            }
        }
    }

    fun pause() {

        if (debug) Log.v(tag, "pause:")

        lock.withLock {

            request = Constants.reqPause
            lockCondition.signalAll()
        }
    }

    fun resume() {

        if (debug) Log.v(tag, "resume:")

        lock.withLock {
            request = Constants.reqResume
            lockCondition.signalAll()
        }
    }

    fun release() {

        if (debug) Log.v(tag, "release:")

        stop()

        lock.withLock {

            request = Constants.reqQuit
            lockCondition.signalAll()
        }
    }

    private fun processStop(req: Int) : Boolean {

        var localIsRunning = true

        when (req) {

            Constants.reqPrepare -> handlePrepare(sourcePath)
            Constants.reqStart,
            Constants.reqPause,
            Constants.reqResume -> throw java.lang.IllegalStateException("invalid state:$state")
            Constants.reqQuit -> localIsRunning = false

            else -> lock.withLock {

                lockCondition.await()
            }
        }

        lock.withLock {

            localIsRunning = localIsRunning and isRunning
        }

        return localIsRunning
    }

    private fun processPrepared(req: Int) : Boolean {

        var localIsRunning = true

        when (req) {
            Constants.reqStart -> handleStart()
            Constants.reqPause,
            Constants.reqResume -> throw IllegalStateException("invalid state:$state")
            Constants.reqStart -> handleStop()
            Constants.reqQuit -> localIsRunning = false

            else -> lock.withLock {
                lockCondition.await()
            }
        }

        lock.withLock {

            localIsRunning = localIsRunning and isRunning
        }

        return localIsRunning
    }

    private fun processPlaying(req: Int) : Boolean {

        var localIsRunning = true

        when (req) {

            Constants.reqPrepare,
            Constants.reqStart,
            Constants.reqResume -> throw java.lang.IllegalStateException("invalid state:$state")
            Constants.reqSeek -> handleSeek(requestTime)
            Constants.reqStop -> handleStop()
            Constants.reqPause -> handlePause()
            Constants.reqQuit -> localIsRunning = false

            else -> handleLoop(callback)
        }

        lock.withLock {

            localIsRunning = localIsRunning and isRunning
        }

        return localIsRunning
    }

    private fun processPaused(req: Int) : Boolean {

        var localIsRunning = true

        when (req) {

            Constants.reqPrepare,
            Constants.reqStart -> throw java.lang.IllegalStateException("invalid state:$state")
            Constants.reqSeek -> handleSeek(requestTime)
            Constants.reqStop -> handleStop()
            Constants.reqResume -> handleResume()
            Constants.reqQuit -> localIsRunning = false

            else -> lock.withLock {

                lockCondition.await()
            }
        }

        lock.withLock {

            localIsRunning = localIsRunning and isRunning
        }

        return localIsRunning
    }

    @Throws(IOException::class)
    private fun handlePrepare(source_file: String) {

        if (debug) Log.v(tag, "handlePrepare:$source_file")

        lock.withLock {

            if (state != Constants.stateStop) {
                throw RuntimeException("invalid state:$state")
            }
        }

        val src = File(source_file)
        if (TextUtils.isEmpty(source_file) || !src.canRead()) {
            throw FileNotFoundException("Unable to read $source_file")
        }
        audioTrackIndex = -1
        videoTrackIndex = audioTrackIndex
        metadata = MediaMetadataRetriever()
        metadata?.setDataSource(source_file)
        updateMovieInfo()

        videoTrackIndex = internalPrepareVideo(source_file)

        if (audioEnabled) audioTrackIndex = internalPrepareAudio(source_file)
        hasAudio = audioTrackIndex >= 0
        if (videoTrackIndex < 0 && audioTrackIndex < 0) {
            throw RuntimeException("No video and audio track found in $source_file")
        }

        lock.withLock { state = Constants.statePrepared }
        callback.onVideoPrepared()
    }

    private fun internalPrepareVideo(sourceFile: String?): Int {

        var trackIndex = -1
        videoMediaExtractor = MediaExtractor()
        try {
            videoMediaExtractor?.setDataSource(sourceFile!!)
            trackIndex = selectTrack(videoMediaExtractor!!, "video/")
            if (trackIndex >= 0) {
                videoMediaExtractor?.selectTrack(trackIndex)
                val format = videoMediaExtractor?.getTrackFormat(trackIndex)
                videoWidth = format!!.getInteger(MediaFormat.KEY_WIDTH)
                videoHeight = format.getInteger(MediaFormat.KEY_HEIGHT)
                duration = format.getLong(MediaFormat.KEY_DURATION)
                if (debug) Log.v(
                    tag, java.lang.String.format(
                        "format:size(%d,%d),duration=%d,bps=%d,framerate=%f,rotation=%d",
                        videoWidth, videoHeight, duration, bitrate, frameRate, rotation
                    )
                )
            }
        } catch (e: IOException) {
            Log.w(tag, e)
        }
        return trackIndex
    }

    private fun handleResume() {
        if (debug) Log.v(tag, "handleResume:")
    }

    private fun handlePause() {
        if (debug) Log.v(tag, "handlePause:")
    }

    private fun internalPrepareAudio(sourceFile: String?): Int {

        var trackIndex = -1
        audioMediaExtractor = MediaExtractor()
        try {
            audioMediaExtractor?.setDataSource(sourceFile!!)
            trackIndex = selectTrack(audioMediaExtractor!!, "audio/")
            if (trackIndex >= 0) {
                audioMediaExtractor?.selectTrack(trackIndex)
                val format: MediaFormat = audioMediaExtractor!!.getTrackFormat(trackIndex)
                audioChannels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                audioSampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)

                val channelConfig: Int = if (audioChannels == 1) {
                    AudioFormat.CHANNEL_OUT_MONO
                } else {
                    AudioFormat.CHANNEL_OUT_STEREO
                }

                val min_buf_size = AudioTrack.getMinBufferSize(audioSampleRate,
                    channelConfig,
                    AudioFormat.ENCODING_PCM_16BIT
                )
                val max_input_size = format.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE)
                audioInputBufSize = if (min_buf_size > 0) min_buf_size * 4 else max_input_size
                if (audioInputBufSize > max_input_size) audioInputBufSize = max_input_size
                val frameSizeInBytes: Int = audioChannels * 2
                audioInputBufSize = audioInputBufSize / frameSizeInBytes * frameSizeInBytes
                if (debug) Log.v(
                    tag,
                    java.lang.String.format(
                        "getMinBufferSize=%d,max_input_size=%d,mAudioInputBufSize=%d",
                        min_buf_size,
                        max_input_size,
                        audioInputBufSize
                    )
                )

                val audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MOVIE)
                    .build()

                val audioFormat = AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(audioSampleRate)
                    .setChannelMask(channelConfig)
                    .build()

                val sessionId = AudioManager.AUDIO_SESSION_ID_GENERATE

                audioTrack = AudioTrack(audioAttributes,
                    audioFormat,
                    audioInputBufSize,
                    AudioTrack.MODE_STREAM,
                    sessionId)


                try {
                    audioTrack?.play()
                } catch (e: java.lang.Exception) {
                    Log.e(tag, "failed to start audio track playing", e)
                    audioTrack?.release()
                    audioTrack = null
                }
            }
        } catch (e: IOException) {
            Log.w(tag, e)
        }
        return trackIndex
    }

    private fun updateMovieInfo() {

        bitrate = 0
        rotation = bitrate
        videoHeight = rotation
        videoWidth = videoHeight
        duration = 0
        frameRate = 0f

        var value: String? = metadata!!.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
        videoWidth = when (value != null) {
            true -> value.toInt()
            false-> 0
        }

        value = metadata!!.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
        videoHeight = when (value != null) {
            true -> value.toInt()
            false-> 0
        }

        value = metadata!!.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)
        rotation = when (value != null) {
            true -> value.toInt()
            false-> 0
        }

        value = metadata!!.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)
        bitrate = when (value != null) {
            true -> value.toInt()
            false-> 0
        }

        value = metadata!!.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
        duration = when (value != null) {
            true -> value.toLong() * 1000L
            false-> 0
        }
    }

    private fun handleStart() {

        if (debug) Log.v(tag, "handleStart:")

        lock.withLock {
            if (state != Constants.statePrepared) throw java.lang.RuntimeException("invalid state:$state")
            state = Constants.statePlaying
        }

        if (requestTime > 0) {
            handleSeek(requestTime)
        }
        previousAudioPresentationTimeUs = -1
        previousVideoPresentationTimeUs = previousAudioPresentationTimeUs
        mVideoOutputDone = true
        mVideoInputDone = mVideoOutputDone

        var videoThread: Thread? = null
        var audioThread: Thread? = null

        if (videoTrackIndex >= 0) {
            val codec = internalStartVideo(videoMediaExtractor!!, videoTrackIndex)
            if (codec != null) {
                videoMediaCodec = codec
                videoBufferInfo = MediaCodec.BufferInfo()
            }

            mVideoOutputDone = false
            mVideoInputDone = mVideoOutputDone
            videoThread = Thread(videoTask, "VideoTask")
        }

        audioOutputDone = true
        audioInputDone = audioOutputDone

        if (audioTrackIndex >= 0) {
            val codec = internalStartAudio(audioMediaExtractor!!, audioTrackIndex)
            if (codec != null) {
                audioMediaCodec = codec
                audioBufferInfo = MediaCodec.BufferInfo()
            }

            audioOutputDone = false
            audioInputDone = audioOutputDone
            audioThread = Thread(audioTask, "AudioTask")
        }
        videoThread?.start()
        audioThread?.start()

        callback.onVideoStarted()
    }

    private fun internalStartVideo(media_extractor: MediaExtractor,
                                     trackIndex: Int): MediaCodec? {

        if (debug) Log.v(tag, "internalStartVideo:")

        var codec: MediaCodec? = null

        if (trackIndex >= 0) {

            val format = media_extractor.getTrackFormat(trackIndex)
            val mime = format.getString(MediaFormat.KEY_MIME)

            if (mime != null) {
                try {
                    codec = MediaCodec.createDecoderByType(mime)
                    codec.configure(format, outputSurface, null, 0)
                    codec.start()
                } catch (e: IOException) {
                    Log.w(tag, e)
                    codec = null
                }

                if (debug) Log.v(tag, "internalStartVideo:codec started")
            }
        }
        return codec
    }

    protected fun internalStartAudio(media_extractor: MediaExtractor,
                                     trackIndex: Int): MediaCodec? {

        if (debug) Log.v(tag, "internalStartAudio:")

        var codec: MediaCodec? = null

        if (trackIndex >= 0) {

            val format = media_extractor.getTrackFormat(trackIndex)
            val mime = format.getString(MediaFormat.KEY_MIME)
            if (mime != null) {

                try {
                    codec = MediaCodec.createDecoderByType(mime)
                    codec.configure(format, null, null, 0)
                    codec.start()
                    if (debug) Log.v(tag, "internalStartAudio:codec started")

                    audioOutTempBuf = ByteArray(audioInputBufSize)

                } catch (e: IOException) {
                    Log.w(tag, e)
                    codec = null
                }
            }
        }

        return codec
    }

    private fun handleSeek(newTime: Long) {

        if (debug) Log.d(tag, "handleSeek")
        if (newTime < 0) return

        if (videoTrackIndex >= 0) {
            videoMediaExtractor?.seekTo(newTime, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
            videoMediaExtractor?.advance()
        }
        if (audioTrackIndex >= 0) {
            audioMediaExtractor?.seekTo(newTime, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
            audioMediaExtractor?.advance()
        }
        requestTime = -1
    }

    private fun handleLoop(callback: SlidanetVideoManager) { //		if (DEBUG) Log.d(TAG, "handleLoop");

        lock.withLock {

            try {
                lockCondition.await()
            } catch (e: InterruptedException) {
            }
        }
        if (mVideoInputDone && mVideoOutputDone && audioInputDone && audioOutputDone) {
            if (debug) Log.d(tag, "Reached EOS, looping check")
            handleStop()
        }
    }

    private fun internalProcessInput(codec: MediaCodec,
                                     extractor: MediaExtractor,
                                     inputBuffers: Array<ByteBuffer>,
                                     presentationTimeUs: Long,
                                     isAudio: Boolean): Boolean {
        var result = true

        while (isRunning) {

            val inputBufIndex = codec.dequeueInputBuffer(timeoutUsec)
            if (inputBufIndex == MediaCodec.INFO_TRY_AGAIN_LATER) break
            if (inputBufIndex >= 0) {
                val size = extractor.readSampleData(inputBuffers[inputBufIndex], 0)
                if (size > 0) {
                    codec.queueInputBuffer(inputBufIndex, 0, size, presentationTimeUs, 0)
                }
                result = extractor.advance() // return false if no data is available
                break
            }
        }
        return result
    }

    private fun handleInputVideo() {
        val presentationTimeUs: Long = videoMediaExtractor!!.getSampleTime()
        /*		if (presentationTimeUs < previousVideoPresentationTimeUs) {
    		presentationTimeUs += previousVideoPresentationTimeUs - presentationTimeUs; // + EPS;
    	}
    	previousVideoPresentationTimeUs = presentationTimeUs; */
        val b = internalProcessInput(
            videoMediaCodec!!, videoMediaExtractor!!, videoInputBuffers!!, presentationTimeUs, false)

        if (!b) {
            if (debug) Log.i(tag, "video track input reached EOS")
            while (isRunning) {
                val inputBufIndex: Int = videoMediaCodec!!.dequeueInputBuffer(timeoutUsec)
                if (inputBufIndex >= 0) {
                    videoMediaCodec!!.queueInputBuffer(
                        inputBufIndex, 0, 0, 0L,
                        MediaCodec.BUFFER_FLAG_END_OF_STREAM
                    )
                    if (debug) Log.v(tag, "sent input EOS:$videoMediaCodec")
                    break
                }
            }

            videoTaskLock.withLock {
                mVideoInputDone = true
                videoTaskLockCondition.signalAll()
            }
        }
    }

    private fun handleOutputVideo(callback: SlidanetVideoManager) { //    	if (DEBUG) Log.v(TAG, "handleDrainVideo:");

        while (isRunning && !mVideoOutputDone) {

            val decoderStatus: Int = videoMediaCodec!!.dequeueOutputBuffer(videoBufferInfo!!, timeoutUsec)
            if (decoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                return
            } else if (decoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                val newFormat: MediaFormat = videoMediaCodec!!.getOutputFormat()
                if (debug) Log.d(
                    tag,
                    "video decoder output format changed: $newFormat"
                )
            } else if (decoderStatus < 0) {
                throw java.lang.RuntimeException(
                    "unexpected result from video decoder.dequeueOutputBuffer: $decoderStatus")
            } else { // decoderStatus >= 0
                var doRender = false
                if (videoBufferInfo!!.size > 0) {
                    doRender = (videoBufferInfo!!.size != 0
                            && !internalWriteVideo(
                        videoOutputBuffers!!.get(decoderStatus),
                        0, videoBufferInfo!!.size, videoBufferInfo!!.presentationTimeUs
                    ))
                    if (doRender) {
                        if (!callback.onFrameAvailable(videoBufferInfo!!.presentationTimeUs))
                            videoStartTime =
                                adjustPresentationTime(videoTaskLock,
                                    videoStartTime,
                                    videoBufferInfo!!.presentationTimeUs)
                    }
                }

                videoMediaCodec?.releaseOutputBuffer(decoderStatus, doRender)
                callback.updateTexture()
                if (videoBufferInfo!!.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                    if (debug) Log.d(tag, "video:output EOS")
                    videoTaskLock.withLock {
                        mVideoOutputDone = true
                        videoTaskLockCondition.signalAll()
                    }
                }
            }
        }
    }

    private fun internalWriteVideo(buffer: ByteBuffer?,
                                     offset: Int,
                                     size: Int,
                                     presentationTimeUs: Long
    ): Boolean { //		if (DEBUG) Log.v(TAG, "internalWriteVideo");
        return false
    }

    private fun handleInputAudio() {

        val presentationTimeUs: Long = audioMediaExtractor!!.sampleTime
        val b = internalProcessInput(audioMediaCodec!!, audioMediaExtractor!!, audioInputBuffers!!, presentationTimeUs, true)
        if (!b) {
            if (debug) Log.i(tag, "audio track input reached EOS")
            while (isRunning) {
                val inputBufIndex: Int = audioMediaCodec!!.dequeueInputBuffer(timeoutUsec)
                if (inputBufIndex >= 0) {
                    audioMediaCodec!!.queueInputBuffer(inputBufIndex,
                        0,
                        0,
                        0L,
                        MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                    if (debug) Log.v(tag, "sent input EOS:$audioMediaCodec")
                    break
                }
            }

            audioTaskLock.withLock {
                audioInputDone = true
                audioTaskLockCondition.signalAll()
            }
        }
    }

    private fun handleOutputAudio(callback: SlidanetVideoManager) { //		if (DEBUG) Log.v(TAG, "handleDrainAudio:");

        while (isRunning && !audioOutputDone) {
            val decoderStatus: Int =
                audioMediaCodec!!.dequeueOutputBuffer(audioBufferInfo!!, timeoutUsec)
            if (decoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                return
            } else if (decoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                val newFormat: MediaFormat = audioMediaCodec!!.getOutputFormat()
                if (debug) Log.d(
                    tag,
                    "audio decoder output format changed: $newFormat"
                )
            } else if (decoderStatus < 0) {
                throw java.lang.RuntimeException(
                    "unexpected result from audio decoder.dequeueOutputBuffer: $decoderStatus"
                )
            } else { // decoderStatus >= 0
                if (audioBufferInfo!!.size > 0) {
                    internalWriteAudio(
                        audioOutputBuffers!!.get(decoderStatus),
                        0, audioBufferInfo!!.size, audioBufferInfo!!.presentationTimeUs
                    )
                    if (!callback.onFrameAvailable(audioBufferInfo!!.presentationTimeUs)) audioStartTime =
                        adjustPresentationTime(audioTaskLock,
                            audioStartTime,
                            audioBufferInfo!!.presentationTimeUs)
                }

                audioMediaCodec!!.releaseOutputBuffer(decoderStatus, false)
                if (audioBufferInfo!!.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                    if (debug) Log.d(tag, "audio:output EOS")
                    audioTaskLock.withLock {
                        audioOutputDone = true
                        audioTaskLockCondition.signalAll()
                    }
                }
            }
        }
    }

    private fun internalWriteAudio(buffer: ByteBuffer,
                                     offset: Int,
                                     size: Int,
                                     presentationTimeUs: Long): Boolean { //		if (DEBUG) Log.d(TAG, "internalWriteAudio");
        if (audioOutTempBuf!!.size < size) {
            audioOutTempBuf = ByteArray(size)
        }
        buffer.position(offset)
        buffer[audioOutTempBuf!!, 0, size]
        buffer.clear()
        if (audioTrack != null) audioTrack?.write(audioOutTempBuf!!, 0, size)
        return true
    }

    private fun adjustPresentationTime(lock: ReentrantLock,
                                       startTime: Long,
                                       presentationTimeUs: Long): Long {

        if (startTime > 0) {
            var t = presentationTimeUs - (System.nanoTime() / 1000 - startTime)
            while (t > 0) {
                lock.withLock {
                    try {
                        lockCondition.await((t % 1000L * 1000L), TimeUnit.MILLISECONDS)
                    } catch (e: InterruptedException) {
                    }
                    if (state == Constants.reqStop || state == Constants.reqQuit) {
                        lockCondition.signalAll()
                        return presentationTimeUs
                    }
                }
                t = presentationTimeUs - (System.nanoTime() / 1000L - startTime)
            }
            return startTime
        } else {
            System.nanoTime() / 1000L
        }

        return startTime
    }

    private fun handleStop() {

        if (debug) Log.v(tag, "handleStop:")

        videoTaskLock.withLock {
            if (videoTrackIndex >= 0) {
                mVideoOutputDone = true
                while (!mVideoInputDone) {
                    try {
                        videoTaskLockCondition.await()
                    } catch (e: InterruptedException) {
                        break
                    }
                }
                internalStopVideo()
                videoTrackIndex = -1
            }
            mVideoInputDone = true
            mVideoOutputDone = mVideoInputDone
        }

        audioTaskLock.withLock {
            if (audioTrackIndex >= 0) {
                audioOutputDone = true
                while (!audioInputDone) {
                    try {
                        audioTaskLockCondition.await()
                    } catch (e: InterruptedException) {
                        break
                    }
                }
                internalStopAudio()
                audioTrackIndex = -1
            }
            audioInputDone = true
            audioOutputDone = audioInputDone
        }
        if (videoMediaCodec != null) {
            videoMediaCodec!!.stop()
            videoMediaCodec!!.release()
            videoMediaCodec = null
        }
        if (audioMediaCodec != null) {
            audioMediaCodec!!.stop()
            audioMediaCodec!!.release()
            audioMediaCodec = null
        }
        if (videoMediaExtractor != null) {
            videoMediaExtractor!!.release()
            videoMediaExtractor = null
        }
        if (audioMediaExtractor != null) {
            audioMediaExtractor!!.release()
            audioMediaExtractor = null
        }
        videoBufferInfo = null.also { audioBufferInfo = it }
        videoInputBuffers = null.also { videoOutputBuffers = it }
        audioInputBuffers = null.also { audioOutputBuffers = it }
        if (metadata != null) {
            metadata?.release()
            metadata = null
        }
        lock.withLock {
            state = Constants.stateStop
        }

        callback.onVideoFinished()
    }

    private fun internalStopVideo() {
        if (debug) Log.v(tag, "internalStopVideo:")
    }

    private fun internalStopAudio() {

        if (debug) Log.v(tag, "internalStopAudio:")

        if (audioTrack != null) {

            if (audioTrack != null) {
                val state = audioTrack!!.state
                if (state != AudioTrack.STATE_UNINITIALIZED) audioTrack?.stop()
            }
            audioTrack?.release()
            audioTrack = null
        }
        audioOutTempBuf = null
    }

    private fun selectTrack(extractor: MediaExtractor, mimeType: String?): Int {

        val numTracks = extractor.trackCount
        var format: MediaFormat
        var mime: String
        for (i in 0 until numTracks) {
            format = extractor.getTrackFormat(i)
            mime = format.getString(MediaFormat.KEY_MIME)!!
            if (mime.startsWith(mimeType!!)) {
                if (debug) {
                    Log.d(
                        tagStatic,
                        "Extractor selected track $i ($mime): $format"
                    )
                }
                return i
            }
        }

        return -1
    }

    fun getVideoWidth() : Int {
        return videoWidth
    }

    fun getVideoHeight() : Int {
        return videoHeight
    }
}