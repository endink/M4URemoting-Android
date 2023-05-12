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
package com.labijie.m4u

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.google.mediapipe.tasks.vision.core.RunningMode
import java.math.RoundingMode
import java.text.DecimalFormat
import kotlin.math.max
import kotlin.math.min

class OverlayView(context: Context?, attrs: AttributeSet?) :
    View(context, attrs) {

    private var results: FaceSolver.FaceSolverResult? = null
    private var linePaint = Paint()
    private var pointPaint = Paint()
    private var textPaint = Paint()

    private var scaleFactor: Float = 1f
    private var imageWidth: Int = 1
    private var imageHeight: Int = 1

    private var offsetLeft: Int = 1
    private var offsetTop: Int = 1
    private var fps: Float = 0.0f

    init {
        initPaints()
    }

    fun clear() {
        results = null
        linePaint.reset()
        pointPaint.reset()
        textPaint.reset()
        invalidate()
        initPaints()
    }

    private fun initPaints() {
        linePaint.color = Color.WHITE
        linePaint.strokeWidth = LANDMARK_STROKE_WIDTH
        linePaint.style = Paint.Style.STROKE

        pointPaint.color =  ContextCompat.getColor(context!!, R.color.overlay_point_color)
        pointPaint.strokeWidth = LANDMARK_STROKE_WIDTH + 2
        pointPaint.style = Paint.Style.FILL

        textPaint.color = Color.GREEN
        textPaint.textSize = 36f


    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)
        results?.let { faceResult ->
            val lines = mutableListOf<Float>()
            val points = mutableListOf<Float>()

            for (landmarks in faceResult.result.faceLandmarks()) {
                for(indexArray in FaceLandmarksConnections.FACE_LANDMARKS_CONNECTORS)
                {
                    for (i in indexArray.indices step 2) {
                        val startX =
                            landmarks[indexArray[i]].x() * imageWidth * scaleFactor - offsetLeft
                        val startY =
                            landmarks[indexArray[i]].y() * imageHeight * scaleFactor - offsetTop
                        val endX =
                            landmarks[indexArray[i + 1]].x() * imageWidth * scaleFactor - offsetLeft
                        val endY =
                            landmarks[indexArray[i + 1]].y() * imageHeight * scaleFactor - offsetTop
                        lines.add(startX)
                        lines.add(startY)
                        lines.add(endX)
                        lines.add(endY)
                        points.add(startX)
                        points.add(startY)
                    }
                    canvas.drawLines(lines.toFloatArray(), linePaint)
                    canvas.drawPoints(points.toFloatArray(), pointPaint)
                }

            }

            val offset = textPaint.textSize + 16
            canvas.drawText("FPS: ${getFloatString(fps)}", offset, offset, textPaint)
            canvas.drawText("Frame: $imageWidth * $imageHeight", offset, offset * 2, textPaint)
            canvas.drawText("Inference: ${faceResult.inferenceTime} ms", offset, offset * 3,textPaint)
        }
    }

    private fun getFloatString(value:Float): String {
        val format = DecimalFormat("#.#")
        format.roundingMode = RoundingMode.FLOOR
        return format.format(value) ?: "0.0"
    }

    fun setResults(
        faceResult: FaceSolver.FaceSolverResult,
        imageHeight: Int,
        imageWidth: Int,
        runningMode: RunningMode = RunningMode.IMAGE,
    ) {
        results = faceResult
        this.fps = faceResult.fps
        this.imageHeight = imageHeight
        this.imageWidth = imageWidth

        scaleFactor = when (runningMode) {
            RunningMode.IMAGE,
            RunningMode.VIDEO -> {
                min(width * 1f / imageWidth, height * 1f / imageHeight)
            }
            RunningMode.LIVE_STREAM -> {
                // PreviewView is in FILL_START mode. So we need to scale up the
                // landmarks to match with the size that the captured images will be
                // displayed.
                max(width * 1f / imageWidth, height * 1f / imageHeight)
            }
        }

        val scaledWidth = (imageWidth * scaleFactor).toInt()
        val scaledHeight = (imageHeight * scaleFactor).toInt()

        offsetLeft = (scaledWidth - width) / 2
        offsetTop =  (scaledHeight - height) / 2


        invalidate()
    }

    companion object {
        private const val LANDMARK_STROKE_WIDTH = 10F

    }
}
