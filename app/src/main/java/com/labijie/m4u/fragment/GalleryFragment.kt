/*
 * Copyright 2022 The TensorFlow Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *             http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.labijie.m4u.fragment

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.labijie.m4u.FaceSolver
import com.labijie.m4u.MainViewModel
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.labijie.m4u.databinding.FragmentGalleryBinding
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

class GalleryFragment : Fragment(),
    FaceSolver.FaceLandmarkerListener {

    enum class MediaType {
        IMAGE,
        VIDEO,
        UNKNOWN
    }

    private var _fragmentGalleryBinding: FragmentGalleryBinding? = null
    private val fragmentGalleryBinding
        get() = _fragmentGalleryBinding!!
    private lateinit var faceSolver: FaceSolver
    private val viewModel: MainViewModel by activityViewModels()
    private var defaultNumResults = 1
    private val gestureRecognizerResultsAdapter by lazy {
        FaceSolveResultAdapter()
    }

    /** Blocking ML operations are performed using this executor */
    private lateinit var backgroundExecutor: ScheduledExecutorService

    private val getContent =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            // Handle the returned Uri
            uri?.let { mediaUri ->
                when (val mediaType = loadMediaType(mediaUri)) {
                    MediaType.IMAGE -> runGestureRecognitionOnImage(mediaUri)
                    MediaType.VIDEO -> runGestureRecognitionOnVideo(mediaUri)
                    MediaType.UNKNOWN -> {
                        updateDisplayView(mediaType)
                        Toast.makeText(
                            requireContext(),
                            "Unsupported data type.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _fragmentGalleryBinding =
            FragmentGalleryBinding.inflate(inflater, container, false)

        return fragmentGalleryBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        fragmentGalleryBinding.fabGetContent.setOnClickListener {
            getContent.launch(arrayOf("image/*", "video/*"))
        }

        with(fragmentGalleryBinding.recyclerviewResults) {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = gestureRecognizerResultsAdapter
        }
        //initBottomSheetControls()
    }

    override fun onPause() {
        fragmentGalleryBinding.overlay.clear()
        if (fragmentGalleryBinding.videoView.isPlaying) {
            fragmentGalleryBinding.videoView.stopPlayback()
        }
        fragmentGalleryBinding.videoView.visibility = View.GONE
        super.onPause()
    }

    // Load and display the image.
    private fun runGestureRecognitionOnImage(uri: Uri) {
        setUiEnabled(false)
        backgroundExecutor = Executors.newSingleThreadScheduledExecutor()
        updateDisplayView(MediaType.IMAGE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(
                requireActivity().contentResolver,
                uri
            )
            ImageDecoder.decodeBitmap(source)
        } else {
            MediaStore.Images.Media.getBitmap(
                requireActivity().contentResolver,
                uri
            )
        }
            .copy(Bitmap.Config.ARGB_8888, true)
            ?.let { bitmap ->
                fragmentGalleryBinding.imageResult.setImageBitmap(bitmap)

                // Run gesture recognizer on the input image
                backgroundExecutor.execute {

                    faceSolver =
                        FaceSolver(
                            context = requireContext(),
                            runningMode = RunningMode.IMAGE,
                            minDetectionConfidence = viewModel.currentMinHandDetectionConfidence,
                            minTrackingConfidence = viewModel.currentMinHandTrackingConfidence,
                            minPresenceConfidence = viewModel.currentMinHandPresenceConfidence,
                            currentDelegate = viewModel.currentDelegate
                        )

                    faceSolver.detectImage(bitmap)
                        ?.let { resultBundle ->
                            activity?.runOnUiThread {

                                    fragmentGalleryBinding.overlay.setResults(
                                        resultBundle,
                                        bitmap.height,
                                        bitmap.width,
                                        RunningMode.IMAGE
                                    )

                                    // This will return an empty list if there are no gestures detected
                                    if(!resultBundle.result.faceBlendshapes().isPresent) {

                                        gestureRecognizerResultsAdapter.updateResults(
                                            resultBundle.result.faceBlendshapes().get().first()
                                        )
                                    } else {
                                        Toast.makeText(
                                            context,
                                            "Hands not detected",
                                            Toast.LENGTH_SHORT).show()
                                    }

                                setUiEnabled(true)
//                                fragmentGalleryBinding.bottomSheetLayout.inferenceTimeVal.text =
//                                    String.format(
//                                        "%d ms",
//                                        resultBundle.inferenceTime
//                                    )
                            }
                        } ?: run {
                        Log.e(
                            TAG, "Error running gesture recognizer."
                        )
                    }

                    faceSolver.close()
                }
            }
    }

    // Load and display the video.
    private fun runGestureRecognitionOnVideo(uri: Uri) {
        setUiEnabled(false)
        updateDisplayView(MediaType.VIDEO)
        gestureRecognizerResultsAdapter.updateResults(emptyList())
        gestureRecognizerResultsAdapter.notifyDataSetChanged()

        with(fragmentGalleryBinding.videoView) {
            setVideoURI(uri)
            // mute the audio
            setOnPreparedListener { it.setVolume(0f, 0f) }
            requestFocus()
        }

        backgroundExecutor = Executors.newSingleThreadScheduledExecutor()
        backgroundExecutor.execute {

            faceSolver =
                FaceSolver(
                    context = requireContext(),
                    runningMode = RunningMode.VIDEO,
                    minDetectionConfidence = viewModel.currentMinHandDetectionConfidence,
                    minTrackingConfidence = viewModel.currentMinHandTrackingConfidence,
                    minPresenceConfidence = viewModel.currentMinHandPresenceConfidence,
                    currentDelegate = viewModel.currentDelegate
                )

            activity?.runOnUiThread {
                fragmentGalleryBinding.videoView.visibility = View.GONE
                fragmentGalleryBinding.progress.visibility = View.VISIBLE
            }

            faceSolver.detectVideoFile(uri, VIDEO_INTERVAL_MS)
                ?.let { resultBundle ->
                    activity?.runOnUiThread { displayVideoResult(resultBundle) }
                }
                ?: run {
                    activity?.runOnUiThread {
                        fragmentGalleryBinding.progress.visibility =
                            View.GONE
                    }
                    Log.e(TAG, "Error running gesture recognizer.")
                }

            faceSolver.close()
        }
    }

    // Setup and display the video.
    private fun displayVideoResult(result: FaceSolver.FaceSolverResult) {

        fragmentGalleryBinding.videoView.visibility = View.VISIBLE
        fragmentGalleryBinding.progress.visibility = View.GONE

        fragmentGalleryBinding.videoView.start()
        val videoStartTimeMs = SystemClock.uptimeMillis()

        backgroundExecutor.scheduleAtFixedRate(
            {
                activity?.runOnUiThread {
                    val videoElapsedTimeMs =
                        SystemClock.uptimeMillis() - videoStartTimeMs
                    val resultIndex =
                        videoElapsedTimeMs.div(VIDEO_INTERVAL_MS).toInt()

                    if (resultIndex >= 1 || fragmentGalleryBinding.videoView.visibility == View.GONE) {
                        // The video playback has finished so we stop drawing bounding boxes
                        setUiEnabled(true)
                        backgroundExecutor.shutdown()
                    } else {
                        fragmentGalleryBinding.overlay.setResults(
                            result,
                            result.inputImageHeight,
                            result.inputImageWidth,
                            RunningMode.VIDEO
                        )
                        val bs = result.result.faceBlendshapes();
                        if (bs.isPresent) {
                            gestureRecognizerResultsAdapter.updateResults(
                                bs.get().first()
                            )
                        }

                        setUiEnabled(false)

//                        fragmentGalleryBinding.bottomSheetLayout.inferenceTimeVal.text =
//                            String.format("%d ms", result.inferenceTime)
                    }
                }
            },
            0,
            VIDEO_INTERVAL_MS,
            TimeUnit.MILLISECONDS
        )
    }

    private fun updateDisplayView(mediaType: MediaType) {
        fragmentGalleryBinding.imageResult.visibility =
            if (mediaType == MediaType.IMAGE) View.VISIBLE else View.GONE
        fragmentGalleryBinding.videoView.visibility =
            if (mediaType == MediaType.VIDEO) View.VISIBLE else View.GONE
        fragmentGalleryBinding.tvPlaceholder.visibility =
            if (mediaType == MediaType.UNKNOWN) View.VISIBLE else View.GONE
    }

    // Check the type of media that user selected.
    private fun loadMediaType(uri: Uri): MediaType {
        val mimeType = context?.contentResolver?.getType(uri)
        mimeType?.let {
            if (mimeType.startsWith("image")) return MediaType.IMAGE
            if (mimeType.startsWith("video")) return MediaType.VIDEO
        }

        return MediaType.UNKNOWN
    }

    private fun setUiEnabled(enabled: Boolean) {
    }

    private fun recognitionError() {
        activity?.runOnUiThread {
            fragmentGalleryBinding.progress.visibility = View.GONE
            setUiEnabled(true)
            updateDisplayView(MediaType.UNKNOWN)
        }
    }

    override fun onError(error: String, errorCode: Int) {
        recognitionError()
        activity?.runOnUiThread {
            Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show()
            if (errorCode == FaceSolver.GPU_ERROR) {
//                fragmentGalleryBinding.bottomSheetLayout.spinnerDelegate.setSelection(
//                    FaceSolver.DELEGATE_CPU,
//                    false
//                )
            }
        }
    }

    override fun onResults(result: FaceSolver.FaceSolverResult) {
        TODO("Not yet implemented")
    }

    companion object {
        private const val TAG = "GalleryFragment"

        // Value used to get frames at specific intervals for inference (e.g. every 300ms)
        private const val VIDEO_INTERVAL_MS = 300L
    }
}
