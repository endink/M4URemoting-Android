// Copyright (c) 2025 Anders Xiao. All rights reserved.
// https://github.com/endink
package com.labijie.m4u

class FPSCounter {
    private var deltaMills: Long = 0L
    private var frameCount: Int = 0
    private var fps:Float = 0.0f

    fun getFps():Float { return fps }

    fun clear() {
        frameCount = 0
        deltaMills = 0L
    }

    fun count(): Boolean {
        if (deltaMills <= 0) {
            deltaMills = System.currentTimeMillis()
            return false
        }
        frameCount++
        val now = System.currentTimeMillis()
        val duration = System.currentTimeMillis() - deltaMills
        if(duration > 500)
        {
            fps = frameCount / (duration / 1000.0f)
            frameCount = 0
            deltaMills = now
            return true
        }
        return false
    }
}