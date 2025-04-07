// Copyright (c) 2025 Anders Xiao. All rights reserved.
// https://github.com/endink
package com.labijie.m4u

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.SystemClock
import android.util.Log
import androidx.annotation.VisibleForTesting
import androidx.camera.core.ImageProxy
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.core.Delegate
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarker
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarkerResult

class FaceSolver(
    val context: Context,
    var minDetectionConfidence: Float = DEFAULT_HAND_DETECTION_CONFIDENCE,
    var minTrackingConfidence: Float = DEFAULT_HAND_TRACKING_CONFIDENCE,
    var minPresenceConfidence: Float = DEFAULT_HAND_PRESENCE_CONFIDENCE,
    var currentDelegate: Int = DELEGATE_CPU,
    var runningMode: RunningMode = RunningMode.IMAGE,
    private var listener: FaceLandmarkerListener? = null
) {
    companion object {
        val TAG = "GestureRecognizerHelper ${this.hashCode()}"
        private const val MP_FACE_LANDMARKER_TASK = "mpt.bytes"

        const val DELEGATE_CPU = 0
        const val DELEGATE_GPU = 1
        const val DEFAULT_HAND_DETECTION_CONFIDENCE = 0.5F
        const val DEFAULT_HAND_TRACKING_CONFIDENCE = 0.5F
        const val DEFAULT_HAND_PRESENCE_CONFIDENCE = 0.5F
        const val OTHER_ERROR = 0
        const val GPU_ERROR = 1
    }

    // For this example this needs to be a var so it can be reset on changes. If the GestureRecognizer
    // will not change, a lazy val would be preferable.
    private var faceLandmarker: FaceLandmarker? = null

    val fpsCounter by lazy {
        FPSCounter()
    }

    private fun returnLivestreamResult(result: FaceLandmarkerResult, mpImage: MPImage) {
        fpsCounter.count()
        val finishTimeMs = SystemClock.uptimeMillis()
        val inferenceTime = finishTimeMs - result.timestampMs()
        val r = FaceSolverResult(
            result,fpsCounter.getFps(), inferenceTime, mpImage.height, mpImage.width
        )
        listener?.onResults(r)
    }

    fun onError(e: RuntimeException)
    {
        listener?.onError(
            e.message ?: "An unknown error has occurred"
        )
    }

    fun close() {
        fpsCounter.clear()
        faceLandmarker?.close()
        faceLandmarker = null
    }

    public fun setup(){
        fpsCounter.clear()
        // Set general recognition options, including number of used threads
        val baseOptionBuilder = BaseOptions.builder()
        // Use the specified hardware for running the model. Default to CPU
        when (currentDelegate) {
            DELEGATE_CPU -> {
                baseOptionBuilder.setDelegate(Delegate.CPU)
            }
            DELEGATE_GPU -> {
                baseOptionBuilder.setDelegate(Delegate.GPU)
            }
        }
        baseOptionBuilder.setModelAssetPath(MP_FACE_LANDMARKER_TASK)

        try {

            val baseOptions = baseOptionBuilder.build()


            val faceOptions = FaceLandmarker.FaceLandmarkerOptions.builder()
                .setBaseOptions(baseOptions)
                .setMinTrackingConfidence(minTrackingConfidence)
                .setMinFaceDetectionConfidence(minDetectionConfidence)
                .setMinFacePresenceConfidence(minPresenceConfidence)
                .setRunningMode(runningMode)
                .setNumFaces(1)
                .setOutputFaceBlendshapes(true)
                .setOutputFacialTransformationMatrixes(true)

            if (runningMode == RunningMode.LIVE_STREAM) {
                faceOptions
                    .setResultListener(this::returnLivestreamResult)
                    .setErrorListener(this::onError)
            }

            val options = faceOptions.build()
            faceLandmarker = FaceLandmarker.createFromOptions(context, options)
        } catch (e: IllegalStateException) {
            listener?.onError(
                "Face solver failed to initialize. See error logs for " + "details"
            )
            Log.e(
                TAG,
                "MP Task Vision failed to load the task with error: " + e.message
            )
        } catch (e: RuntimeException) {
            listener?.onError(
                "Face solver failed to initialize. See error logs for " + "details",
                GPU_ERROR
            )
            Log.e(
                TAG,
                "MP Task Vision failed to load the task with error: " + e.message
            )
        }
    }

    fun isClosed(): Boolean {
        return faceLandmarker == null
    }

    // Accepts the URI for a video file loaded from the user's gallery and attempts to run
    // gesture recognizer inference on the video. This process will evaluate
    // every frame in the video and attach the results to a bundle that will be
    // returned.
    fun detectVideoFile(
        videoUri: Uri,
        inferenceIntervalMs: Long
    ): FaceSolverResult? {
        if (runningMode != RunningMode.VIDEO) {
            throw IllegalArgumentException(
                "Attempting to call recognizeVideoFile" +
                        " while not using RunningMode.VIDEO"
            )
        }

        // Inference time is the difference between the system time at the start and finish of the
        // process
        val startTime = SystemClock.uptimeMillis()

        var didErrorOccurred = false

        // Load frames from the video and run the gesture recognizer.
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(context, videoUri)
        val videoLengthMs =
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLong()

        // Note: We need to read width/height from frame instead of getting the width/height
        // of the video directly because MediaRetriever returns frames that are smaller than the
        // actual dimension of the video file.
        val firstFrame = retriever.getFrameAtTime(0)
        val width = firstFrame?.width
        val height = firstFrame?.height

        // If the video is invalid, returns a null recognition result
        if ((videoLengthMs == null) || (width == null) || (height == null)) return null

        // Next, we'll get one frame every frameInterval ms, then run recognizer
        // on these frames.
        var result : FaceLandmarkerResult? = null
        val numberOfFrameToRead = videoLengthMs.div(inferenceIntervalMs)

        for (i in 0..numberOfFrameToRead) {
            val timestampMs = i * inferenceIntervalMs // ms

            retriever
                .getFrameAtTime(
                    timestampMs * 1000, // convert from ms to micro-s
                    MediaMetadataRetriever.OPTION_CLOSEST
                )
                ?.let { frame ->
                    // Convert the video frame to ARGB_8888 which is required by the MediaPipe
                    val argb8888Frame =
                        if (frame.config == Bitmap.Config.ARGB_8888) frame
                        else frame.copy(Bitmap.Config.ARGB_8888, false)

                    // Convert the input Bitmap object to an MPImage object to run inference
                    val mpImage = BitmapImageBuilder(argb8888Frame).build()

                    // Run gesture recognizer using MediaPipe Gesture Recognizer
                    // API
                    faceLandmarker?.detect(mpImage)
                        ?.let { recognizerResult ->
                            result = recognizerResult
                        } ?: {
                        didErrorOccurred = true
                        listener?.onError(
                            "ResultBundle could not be returned" +
                                    " in recognizeVideoFile"
                        )
                    }
                }
                ?: run {
                    didErrorOccurred = true
                    listener?.onError(
                        "Frame at specified time could not be" +
                                " retrieved when recognition in video."
                    )
                }
        }

        retriever.release()

        val inferenceTimePerFrameMs =
            (SystemClock.uptimeMillis() - startTime).div(numberOfFrameToRead)

        return if (didErrorOccurred || result == null) {
            null
        } else {
            FaceSolverResult(result!!, fpsCounter.getFps(),inferenceTimePerFrameMs, height, width)
        }
    }

    // Accepted a Bitmap and runs gesture recognizer inference on it to
    // return results back to the caller
    fun detectImage(image: Bitmap): FaceSolverResult? {
        if (runningMode != RunningMode.IMAGE) {
            throw IllegalArgumentException(
                "Attempting to call detectImage" +
                        " while not using RunningMode.IMAGE"
            )
        }


        // Inference time is the difference between the system time at the
        // start and finish of the process
        val startTime = SystemClock.uptimeMillis()

        // Convert the input Bitmap object to an MPImage object to run inference
        val mpImage = BitmapImageBuilder(image).build()

        // Run gesture recognizer using MediaPipe Gesture Recognizer API
        faceLandmarker?.detect(mpImage)?.also { recognizerResult ->
            val inferenceTimeMs = SystemClock.uptimeMillis() - startTime
            return FaceSolverResult(
                recognizerResult,
                fpsCounter.getFps(),
                inferenceTimeMs,
                image.height,
                image.width
            )
        }

        // If gestureRecognizer?.recognize() returns null, this is likely an error. Returning null
        // to indicate this.
        listener?.onError(
            "Gesture Recognizer failed to recognize."
        )
        return null
    }

    // Convert the ImageProxy to MP Image and feed it to GestureRecognizer.
    fun detectLiveStream(
        isFrontCamera: Boolean,
        imageProxy: ImageProxy,
    ) {
        val frameTime = SystemClock.uptimeMillis()
// Copy out RGB bits from the frame to a bitmap buffer
        val bitmapBuffer =
            Bitmap.createBitmap(
                imageProxy.width,
                imageProxy.height,
                Bitmap.Config.ARGB_8888
            )
        imageProxy.use { bitmapBuffer.copyPixelsFromBuffer(imageProxy.planes[0].buffer) }
        imageProxy.close()

        val matrix = Matrix().apply {
            // Rotate the frame received from the camera to be in the same direction as it'll be shown
            postRotate(imageProxy.imageInfo.rotationDegrees.toFloat())

            // flip image if user use front camera
            if (isFrontCamera) {
                postScale(
                    -1f,
                    1f,
                    imageProxy.width.toFloat(),
                    imageProxy.height.toFloat()
                )
            }
        }

        val rotatedBitmap = Bitmap.createBitmap(
            bitmapBuffer, 0, 0, bitmapBuffer.width, bitmapBuffer.height,
            matrix, true
        )

        // Convert the input Bitmap object to an MPImage object to run inference
        val mpImage = BitmapImageBuilder(rotatedBitmap).build()

        recognizeAsync(mpImage, frameTime)
    }

    @VisibleForTesting
    fun recognizeAsync(mpImage: MPImage, frameTime: Long) {
        // As we're using running mode LIVE_STREAM, the recognition result will
        // be returned in returnLivestreamResult function
        faceLandmarker?.detectAsync(mpImage, frameTime)
    }

    interface FaceLandmarkerListener {
        fun onError(error: String, errorCode: Int = OTHER_ERROR)
        fun onResults(result: FaceSolverResult)
    }

    data class FaceSolverResult(
        val result: FaceLandmarkerResult,
        val fps:Float = 0.0f,
        val inferenceTime: Long,
        val inputImageHeight: Int,
        val inputImageWidth: Int,
    )
}