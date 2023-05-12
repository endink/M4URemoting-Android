package com.labijie.m4u.fragment

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.labijie.m4u.R
import com.labijie.m4u.SettingsViewModel
import com.labijie.m4u.databinding.FragmentSettingsBinding
import com.labijie.m4u.osc.OSCClient
import org.slf4j.LoggerFactory
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.regex.Matcher
import java.util.regex.Pattern

class SettingsFragment : Fragment() {

    companion object{
        val logger = LoggerFactory.getLogger(SettingsFragment::class.java)
    }

    private var _fragmentSettingsBinding: FragmentSettingsBinding? = null

    private val fragmentSettingsBinding
        get() = _fragmentSettingsBinding!!

    private val viewModel: SettingsViewModel by activityViewModels()
    private lateinit var backgroundExecutor: ExecutorService

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _fragmentSettingsBinding =
            FragmentSettingsBinding.inflate(inflater, container, false)

        return fragmentSettingsBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        backgroundExecutor = Executors.newSingleThreadExecutor()
        initControls(view)
    }

    override fun onDestroy() {
        super.onDestroy()
        backgroundExecutor.shutdown()
    }

    private fun getCheckResult(line: String): Int { // System.out.println("控制台输出的结果为:"+line);
        val pattern: Pattern = Pattern.compile("(\\d+ms)(\\s+)(TTL=\\d+)", Pattern.CASE_INSENSITIVE)
        val matcher: Matcher = pattern.matcher(line)
        while (matcher.find()) {
            return 1
        }
        return 0
    }

    private fun pingHost() : Boolean
    {
        return try {
            OSCClient.sendCommand("ping")
        } catch (e:Exception) {
            logger.warn("check host fault.\n $e")
            false
        }
    }


    private fun initControls(view: View) {
        // init bottom sheet settings
        updateControlsUi()

//        fragmentSettingsBinding.testConnection.setOnClickListener {
//            fragmentSettingsBinding.hostInputLayout.isErrorEnabled = false;
//            fragmentSettingsBinding.portInputLayout.isErrorEnabled = false;
//
//            val host: String = fragmentSettingsBinding.hostInput.text.toString()
//            val portText = fragmentSettingsBinding.portInput.text.toString()
//
//            val port = portText.toIntOrNull()
//            if (port == null || (port < 1 || port > 65535)) {
//                fragmentSettingsBinding.portInputLayout.error =
//                    getString(R.string.invalid_port_value)
//            } else {
//                fragmentSettingsBinding.testConnection.isEnabled = false
//                backgroundExecutor.execute {
//                    val connected = OSCClient.connect(host, port)
//                    OSCClient.disconnect()
//                    if (connected) {
//                        if (!pingHost())
//                        {
//                            view.post {
//                                Toast.makeText(
//                                    context,
//                                    "Unable to connect to the remote host ( $host:$port ).",
//                                    Toast.LENGTH_LONG
//                                ).show()
//
//                                fragmentSettingsBinding.testConnection.isEnabled = true
//                            }
//                        } else {
//                            view.post {
//                                Toast.makeText(
//                                    context,
//                                    "Connect successfully.",
//                                    Toast.LENGTH_LONG
//                                ).show()
//
//                                fragmentSettingsBinding.testConnection.isEnabled = true
//                            }
//                        }
//                    } else {
//                        view.post {
//                            Toast.makeText(
//                                context,
//                                "Unable to connect to the remote host ( $host:$port ).",
//                                Toast.LENGTH_LONG
//                            ).show()
//                            fragmentSettingsBinding.testConnection.isEnabled = true
//                        }
//                    }
//                }
//            }
//
//        }

        // When clicked, lower hand detection score threshold floor
        fragmentSettingsBinding.detectionThresholdMinus.setOnClickListener {
            if (viewModel.currentMinDetectionConfidence >= 0.2) {
                val v = viewModel.currentMinDetectionConfidence - 0.1f
                viewModel.setMinDetectionConfidence(v)

                updateControlsUi()
            }
        }

        // When clicked, raise hand detection score threshold floor
        fragmentSettingsBinding.detectionThresholdPlus.setOnClickListener {
            if (viewModel.currentMinDetectionConfidence <= 0.8) {
                val v = viewModel.currentMinDetectionConfidence + 0.1f
                viewModel.setMinDetectionConfidence(v)

                updateControlsUi()
            }
        }

        // When clicked, lower hand tracking score threshold floor
        fragmentSettingsBinding.trackingThresholdMinus.setOnClickListener {

            if (viewModel.currentMinTrackingConfidence >= 0.2) {
                val v = viewModel.currentMinTrackingConfidence - 0.1f
                viewModel.setMinTrackingConfidence(v)
                updateControlsUi()
            }
        }

        // When clicked, raise hand tracking score threshold floor
        fragmentSettingsBinding.trackingThresholdPlus.setOnClickListener {
            if (viewModel.currentMinTrackingConfidence <= 0.8) {
                val v = viewModel.currentMinTrackingConfidence + 0.1f
                viewModel.setMinTrackingConfidence(v)
                updateControlsUi()
            }
        }

        // When clicked, lower hand presence score threshold floor
        fragmentSettingsBinding.presenceThresholdMinus.setOnClickListener {
            if (viewModel.currentMinPresenceConfidence >= 0.2) {
                val v = viewModel.currentMinPresenceConfidence - 0.1f
                viewModel.setMinPresenceConfidence(v)
                updateControlsUi()
            }
        }

        // When clicked, raise hand presence score threshold floor
        fragmentSettingsBinding.presenceThresholdPlus.setOnClickListener {
            if (viewModel.currentMinPresenceConfidence <= 0.8) {
                val v = viewModel.currentMinPresenceConfidence + 0.1f
                viewModel.setMinPresenceConfidence(v)
                updateControlsUi()
            }
        }

        // When clicked, change the underlying hardware used for inference.
        // Current options are CPU and GPU
        fragmentSettingsBinding.spinnerDelegate.setSelection(
            viewModel.currentDelegate, false
        )
        fragmentSettingsBinding.spinnerDelegate.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long
                ) {
                    try {
                        viewModel.setDelegate(p2)
                    } catch (e: UninitializedPropertyAccessException) {
                        Log.e(
                            CameraFragment.TAG,
                            "GestureRecognizerHelper has not been initialized yet."
                        )

                    }
                }

                override fun onNothingSelected(p0: AdapterView<*>?) {
                    /* no op */
                }
            }
    }

    private fun updateControlsUi() {
        fragmentSettingsBinding.detectionThresholdValue.text =
            String.format(
                Locale.US, "%.2f", viewModel.currentMinDetectionConfidence
            )
        fragmentSettingsBinding.trackingThresholdValue.text =
            String.format(
                Locale.US, "%.2f", viewModel.currentMinTrackingConfidence
            )
        fragmentSettingsBinding.presenceThresholdValue.text =
            String.format(
                Locale.US, "%.2f", viewModel.currentMinPresenceConfidence
            )

        fragmentSettingsBinding.showFace.isChecked = viewModel.showFace
        fragmentSettingsBinding.hostInput.setText(viewModel.oscHost)
        fragmentSettingsBinding.portInput.setText(viewModel.oscPort.toString())
    }

    override fun onPause() {
        super.onPause()
        viewModel.showFace = fragmentSettingsBinding.showFace.isChecked
        viewModel.oscHost = fragmentSettingsBinding.hostInput.text.toString()
        viewModel.oscPort = fragmentSettingsBinding.portInput.text.toString().toIntOrNull() ?: 0
        activity?.applicationContext?.also {
            viewModel.save(it)
        }
    }
}