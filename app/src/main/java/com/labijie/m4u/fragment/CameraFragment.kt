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
import android.content.res.Configuration
import android.hardware.camera2.CaptureRequest
import android.os.Bundle
import android.util.Log
import android.util.Range
import android.util.Size
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.camera.camera2.interop.Camera2Interop
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.Navigation
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.illposed.osc.transport.OSCPortOut
import com.labijie.m4u.*
import com.labijie.m4u.R
import com.labijie.m4u.databinding.FragmentCameraBinding
import com.labijie.m4u.osc.OSCClient
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class CameraFragment : Fragment(),
    FaceSolver.FaceLandmarkerListener {

    companion object {
        const val TAG = "M4U Face Capture"
        val logger: Logger = LoggerFactory.getLogger(CameraFragment::class.java)
    }

    private var _fragmentCameraBinding: FragmentCameraBinding? = null
    private var _oscPortOut: OSCPortOut? = null

    private val fragmentCameraBinding
        get() = _fragmentCameraBinding!!

    private lateinit var faceSolver: FaceSolver
    private val viewModel: SettingsViewModel by activityViewModels()
    private var defaultNumResults = 51
    private val faceResultAdapter: FaceSolveResultAdapter by lazy {
        FaceSolveResultAdapter().apply {
            updateResults(emptyList())
        }
    }
    private var preview: Preview? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private var camera: Camera? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var cameraFacing = CameraSelector.LENS_FACING_FRONT

    /** Blocking ML operations are performed using this executor */
    private lateinit var backgroundExecutor: ExecutorService

    override fun onResume() {
        super.onResume()
        // Make sure that all permissions are still present, since the
        // user could have removed them while the app was in paused state.
        if (!PermissionsFragment.hasPermissions(requireContext())) {
            Navigation.findNavController(
                requireActivity(), R.id.fragment_container
            ).navigate(R.id.action_camera_to_permissions)
        }

        fragmentCameraBinding.viewFinder.visibility = if(viewModel.showFace) View.VISIBLE else View.INVISIBLE

        if(this::faceSolver.isInitialized) {
            faceSolver.fpsCounter.clear()
            faceSolver.minDetectionConfidence = viewModel.currentMinDetectionConfidence;
            faceSolver.minTrackingConfidence = viewModel.currentMinTrackingConfidence;
            faceSolver.minPresenceConfidence = viewModel.currentMinPresenceConfidence;
            faceSolver.currentDelegate = viewModel.currentDelegate;

            // Start the GestureRecognizerHelper again when users come back
            // to the foreground.
            backgroundExecutor.execute {
                if (faceSolver.isClosed()) {
                    faceSolver.setup()
                }
            }
        }
        backgroundExecutor.execute {
            OSCClient.connect(viewModel.oscHost, viewModel.oscPort)
        }
    }

    override fun onPause() {
        super.onPause()
        if (this::faceSolver.isInitialized) {
            // Close the Gesture Recognizer helper and release resources
            backgroundExecutor.execute { faceSolver.close() }
        }

        if (_oscPortOut?.isConnected == true) {
            backgroundExecutor.execute {
                // Close the Gesture Recognizer helper and release resources
                backgroundExecutor.execute { OSCClient.disconnect() }
            }
        }
    }

    override fun onDestroyView() {
        _fragmentCameraBinding = null
        super.onDestroyView()

        // Shut down our background executor
        backgroundExecutor.shutdown()
        backgroundExecutor.awaitTermination(
            Long.MAX_VALUE, TimeUnit.NANOSECONDS
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _fragmentCameraBinding =
            FragmentCameraBinding.inflate(inflater, container, false)

        return fragmentCameraBinding.root
    }

    override fun onDestroy() {
        super.onDestroy()
        backgroundExecutor.shutdown()
    }

    private fun onCalibrationClicked(){
        backgroundExecutor.execute{
            OSCClient.sendCommand(RemotingCommands.FaceCalibrationCommand)
        }
    }

    private fun onUnCalibrationClicked(){
        backgroundExecutor.execute{
            OSCClient.sendCommand(RemotingCommands.FaceUnCalibrationCommand)
        }
    }

    @SuppressLint("MissingPermission")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
//        with(fragmentCameraBinding.recyclerviewResults) {
//            layoutManager = LinearLayoutManager(requireContext())
//            adapter = faceResultAdapter
//        }

        // Initialize our background executor
        backgroundExecutor = Executors.newSingleThreadExecutor()

        // Wait for the views to be properly laid out
        fragmentCameraBinding.viewFinder.post {
            fragmentCameraBinding.faceCalibration.setOnClickListener {
                onCalibrationClicked()
            }

            fragmentCameraBinding.faceUndoCalibration.setOnClickListener {
                onUnCalibrationClicked()
            }
            // Set up the camera and its use cases
            setUpCamera()
        }

        // Create the Hand Gesture Recognition Helper that will handle the
        // inference
        faceSolver = FaceSolver(
            context = requireContext(),
            runningMode = RunningMode.LIVE_STREAM,
            minDetectionConfidence = viewModel.currentMinDetectionConfidence,
            minTrackingConfidence = viewModel.currentMinTrackingConfidence,
            minPresenceConfidence = viewModel.currentMinPresenceConfidence,
            currentDelegate = viewModel.currentDelegate,
            listener = this
        )
    }

    // Initialize CameraX, and prepare to bind the camera use cases
    private fun setUpCamera() {
        val cameraProviderFuture =
            ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener(
            {
                // CameraProvider
                cameraProvider = cameraProviderFuture.get()
                // Build and bind the camera use cases
                bindCameraUseCases()
            }, ContextCompat.getMainExecutor(requireContext())
        )
    }

    // Declare and bind preview, capture and analysis use cases
    @SuppressLint("UnsafeOptInUsageError")
    private fun bindCameraUseCases() {

        // CameraProvider
        val cameraProvider = cameraProvider
            ?: throw IllegalStateException("Camera initialization failed.")

        val cameraSelector =
            CameraSelector.Builder().requireLensFacing(cameraFacing)
                .build()

        // Preview. Only using the 4:3 ratio because this is the closest to our models
        val previewBuilder = Preview.Builder()
        val extender = Camera2Interop.Extender(previewBuilder)
        extender.setCaptureRequestOption(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, Range(30, 30))
        preview = previewBuilder.setTargetResolution(Size(720,720))
            .setTargetRotation(fragmentCameraBinding.viewFinder.display.rotation)
            .build()

        // ImageAnalysis. Using RGBA 8888 to match how our models work
        imageAnalyzer =
            ImageAnalysis.Builder()
                .setTargetResolution(Size(480,480))
                .setTargetRotation(fragmentCameraBinding.viewFinder.display.rotation)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .build()
                // The analyzer can then be assigned to the instance
                .also {
                    it.setAnalyzer(backgroundExecutor) { image ->
                        solveFace(image)
                    }
                }

        // Must unbind the use-cases before rebinding them
        cameraProvider.unbindAll()

        try {
            // A variable number of use-cases can be passed here -
            // camera provides access to CameraControl & CameraInfo
            camera = cameraProvider.bindToLifecycle(
                this, cameraSelector, preview, imageAnalyzer
            )

            // Attach the viewfinder's surface provider to preview use case
            preview?.setSurfaceProvider(fragmentCameraBinding.viewFinder.surfaceProvider)
        } catch (exc: Exception) {
            Log.e(TAG, "Use case binding failed", exc)
        }

    }

    private fun solveFace(imageProxy: ImageProxy, ) {
        faceSolver.detectLiveStream(
            isFrontCamera = cameraFacing == CameraSelector.LENS_FACING_FRONT,
            imageProxy = imageProxy,
        )
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        imageAnalyzer?.targetRotation =
            fragmentCameraBinding.viewFinder.display.rotation
    }

    // Update UI after a hand gesture has been recognized. Extracts original
    // image height/width to scale and place the landmarks properly through
    // OverlayView. Only one result is expected at a time. If two or more
    // hands are seen in the camera frame, only one will be processed.
    override fun onResults(result: FaceSolver.FaceSolverResult) {
        backgroundExecutor.execute {
            OSCClient.send(result)
        }
        activity?.runOnUiThread {
            if (_fragmentCameraBinding != null) {

                // Pass necessary information to OverlayView for drawing on the canvas
                fragmentCameraBinding.overlay.setResults(
                    result,
                    result.inputImageHeight,
                    result.inputImageWidth,
                    RunningMode.LIVE_STREAM
                )

                // Force a redraw
                fragmentCameraBinding.overlay.invalidate()
            }
        }
    }

    override fun onError(error: String, errorCode: Int) {
        activity?.runOnUiThread {
            Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show()
            faceResultAdapter.updateResults(emptyList())

            if (errorCode == FaceSolver.GPU_ERROR) {
//                fragmentCameraBinding.bottomSheetLayout.spinnerDelegate.setSelection(
//                    FaceSolver.DELEGATE_CPU, false
//                )
            }
        }
    }
}
