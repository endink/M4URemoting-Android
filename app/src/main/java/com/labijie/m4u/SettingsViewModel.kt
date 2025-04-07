// Copyright (c) 2025 Anders Xiao. All rights reserved.
// https://github.com/endink
package com.labijie.m4u

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel

class SettingsViewModel(application: Application)  : AndroidViewModel(application) {



    private var _delegate: Int = FaceSolver.DELEGATE_GPU
    private var _minDetectionConfidence: Float =
        FaceSolver.DEFAULT_HAND_DETECTION_CONFIDENCE
    private var _minTrackingConfidence: Float = FaceSolver.DEFAULT_HAND_TRACKING_CONFIDENCE
    private var _minPresenceConfidence: Float = FaceSolver.DEFAULT_HAND_PRESENCE_CONFIDENCE

    val currentDelegate: Int get() = _delegate

    val currentMinDetectionConfidence: Float
        get() = _minDetectionConfidence
    val currentMinTrackingConfidence: Float
        get() = _minTrackingConfidence
    val currentMinPresenceConfidence: Float
        get() = _minPresenceConfidence

    var oscHost: String = ""
    var oscPort: Int = 22345
    var showFace: Boolean = true


    fun setDelegate(delegate: Int) {
        _delegate = delegate
    }

    fun setMinDetectionConfidence(confidence: Float) {
        _minDetectionConfidence = confidence
    }

    fun setMinTrackingConfidence(confidence: Float) {
        _minTrackingConfidence = confidence
    }

    fun setMinPresenceConfidence(confidence: Float) {
        _minPresenceConfidence = confidence
    }

    init {
        oscHost = M4UUtils.getLatestHost(application.applicationContext)
        oscPort = M4UUtils.getLatestPort(application.applicationContext)
    }

    fun save(ctx: Context){
        M4UUtils.setLatestHost(ctx, oscHost)
        M4UUtils.setLatestPort(ctx, oscPort)
    }
}