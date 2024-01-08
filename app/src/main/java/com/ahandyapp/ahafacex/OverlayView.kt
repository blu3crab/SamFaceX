package com.ahandyapp.ahafacex

/*
 * Copyright 2023 The TensorFlow Authors. All Rights Reserved.
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
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.util.Log
import android.view.View
import androidx.core.content.ContextCompat
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarker
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarkerResult
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class OverlayView(context: Context?, attrs: AttributeSet?) :
    View(context, attrs) {

    private var mark: Boolean = true
    private var markTimeMs = 0L
    private var markIntervalCount = 0
    private var markLandmarkList: List<NormalizedLandmark>? = null
    var sumDeltaX = 0f
    var sumDeltaY = 0f

    private var result: FaceLandmarkerResult? = null
    private var linePaint = Paint()
    private var pointPaint = Paint()

    private var scaleFactor: Float = 1f
    private var imageWidth: Int = 1
    private var imageHeight: Int = 1



    init {
        initPaints()
    }

    fun clear() {
        result = null
        linePaint.reset()
        pointPaint.reset()
        invalidate()
        initPaints()
    }

    private fun initPaints() {
        linePaint.color =
            ContextCompat.getColor(context!!, R.color.mp_color_primary)
        linePaint.strokeWidth = LANDMARK_STROKE_WIDTH
        linePaint.style = Paint.Style.STROKE

        pointPaint.color = Color.YELLOW
        pointPaint.strokeWidth = LANDMARK_STROKE_WIDTH
        pointPaint.style = Paint.Style.FILL
    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)
        if(result == null || result!!.faceLandmarks().isEmpty()) {
            clear()
            return
        }

        result?.let { faceLandmarkerResult ->

            if( faceLandmarkerResult.faceBlendshapes().isPresent) {
                faceLandmarkerResult.faceBlendshapes().get().forEach {
                    it.forEach {
                        Log.e(TAG, it.displayName() + " " + it.score())
                    }
                }
            }

            for(landmark in faceLandmarkerResult.faceLandmarks()) {
                for(normalizedLandmark in landmark) {
                    canvas.drawPoint(normalizedLandmark.x() * imageWidth * scaleFactor, normalizedLandmark.y() * imageHeight * scaleFactor, pointPaint)
                }
            }

            FaceLandmarker.FACE_LANDMARKS_CONNECTORS.forEach {
                canvas.drawLine(
                    faceLandmarkerResult.faceLandmarks().get(0).get(it!!.start()).x() * imageWidth * scaleFactor,
                    faceLandmarkerResult.faceLandmarks().get(0).get(it.start()).y() * imageHeight * scaleFactor,
                    faceLandmarkerResult.faceLandmarks().get(0).get(it.end()).x() * imageWidth * scaleFactor,
                    faceLandmarkerResult.faceLandmarks().get(0).get(it.end()).y() * imageHeight * scaleFactor,
                    linePaint)
            }
        }
    }

    fun setResult(
        faceLandmarkerResult: FaceLandmarkerResult,
        imageHeight: Int,
        imageWidth: Int,
        runningMode: RunningMode = RunningMode.IMAGE
    ) {
        result = faceLandmarkerResult

        // TODO-> log result (for subset)
        // eyes-> 37, 40, 43, 46
        //Log.d(TAG, result.toString())
        val landmarkList = result!!.faceLandmarks()[0]  // first (zero-ith) face
        val landmarkListSize = landmarkList.size
        val timeMs = result!!.timestampMs()


        if (landmarkListSize > LANDMARK_RIGHT_OUTER) {
            if (mark) {
                Log.d(
                    TAG, "${markIntervalCount} landmarks at $timeMs:" +
                            "\n...left outer ->${landmarkList[LANDMARK_LEFT_OUTER].x()}, ${landmarkList[LANDMARK_LEFT_OUTER].y()}, ${landmarkList[LANDMARK_LEFT_OUTER].z()}" +
                            "\n...left inner ->${landmarkList[LANDMARK_LEFT_INNER].x()}, ${landmarkList[LANDMARK_LEFT_INNER].y()}, ${landmarkList[LANDMARK_LEFT_INNER].z()}" +
                            "\n...right inner->${landmarkList[LANDMARK_RIGHT_INNER].x()}, ${landmarkList[LANDMARK_RIGHT_INNER].y()}, ${landmarkList[LANDMARK_RIGHT_INNER].z()}" +
                            "\n...right outer->${landmarkList[LANDMARK_RIGHT_OUTER].x()}, ${landmarkList[LANDMARK_RIGHT_OUTER].y()}, ${landmarkList[LANDMARK_RIGHT_OUTER].z()}"
                )
                // if landmarks are available from previous interval
                markLandmarkList?.let {
//                    // delta landmarks
//                    val deltaLeftOuterX =
//                        landmarkList[LANDMARK_LEFT_OUTER].x() - markLandmarkList!![LANDMARK_LEFT_OUTER].x()
//                    val deltaLeftInnerX =
//                        landmarkList[LANDMARK_LEFT_INNER].x() - markLandmarkList!![LANDMARK_LEFT_INNER].x()
//                    val deltaRightInnerX =
//                        landmarkList[LANDMARK_RIGHT_INNER].x() - markLandmarkList!![LANDMARK_RIGHT_INNER].x()
//                    val deltaRightOuterX =
//                        landmarkList[LANDMARK_RIGHT_OUTER].x() - markLandmarkList!![LANDMARK_RIGHT_OUTER].x()
//                    val deltaLeftOuterY =
//                        landmarkList[LANDMARK_LEFT_OUTER].y() - markLandmarkList!![LANDMARK_LEFT_OUTER].y()
//                    val deltaLeftInnerY =
//                        landmarkList[LANDMARK_LEFT_INNER].y() - markLandmarkList!![LANDMARK_LEFT_INNER].y()
//                    val deltaRightInnerY =
//                        landmarkList[LANDMARK_RIGHT_INNER].y() - markLandmarkList!![LANDMARK_RIGHT_INNER].y()
//                    val deltaRightOuterY =
//                        landmarkList[LANDMARK_RIGHT_OUTER].y() - markLandmarkList!![LANDMARK_RIGHT_OUTER].y()
//                    Log.d(
//                        TAG, "${markIntervalCount} landmarks at $timeMs:" +
//                                "\n...left outer delta X, Y->$deltaLeftOuterX, $deltaLeftOuterY" +
//                                "\n...left inner delta X, Y ->$deltaLeftInnerX, $deltaLeftInnerY" +
//                                "\n...right inner delta X, Y->$deltaRightInnerX, $deltaRightInnerY" +
//                                "\n...right outer delta X, Y->$deltaRightOuterX, $deltaRightOuterY"
//                    )
//                    // sum delta X, Y -> X > Y !Yes, Y > X !No
//                    sumDeltaX = abs(deltaLeftOuterX + deltaLeftInnerX + deltaRightInnerX + deltaRightOuterX)
//                    sumDeltaY = abs(deltaLeftOuterY + deltaLeftInnerY + deltaRightInnerY + deltaRightOuterY)
                    if (sumDeltaX > sumDeltaY) {
                        Log.d(
                            TAG, "Nod NO!" +
                                    "\n...sum delta X, Y ->$sumDeltaX, $sumDeltaY"
                        )
                    }
                    else if (sumDeltaY > sumDeltaX) {
                        Log.d(
                            TAG, "Nod YES!" +
                                    "\n...sum delta X, Y ->$sumDeltaX, $sumDeltaY"
                        )
                    } else {
                        Log.d(
                            TAG, "Nod RANDOM!" +
                                    "\n...sum delta X, Y ->$sumDeltaX, $sumDeltaY"
                        )
                    }

                }
                // reset for next interval
                mark = false
                markTimeMs = timeMs
                markLandmarkList = landmarkList
                markIntervalCount = 0
                sumDeltaX = 0f
                sumDeltaY = 0f
            } else {
                markIntervalCount++

                if (timeMs - markTimeMs > 1000) {
                    // mark interval reached
                    mark = true
                    // if landmarks are available from previous interval
                    markLandmarkList?.let {
                        // delta landmarks
                        val deltaLeftOuterX =
                            landmarkList[LANDMARK_LEFT_OUTER].x() - markLandmarkList!![LANDMARK_LEFT_OUTER].x()
                        val deltaLeftInnerX =
                            landmarkList[LANDMARK_LEFT_INNER].x() - markLandmarkList!![LANDMARK_LEFT_INNER].x()
                        val deltaRightInnerX =
                            landmarkList[LANDMARK_RIGHT_INNER].x() - markLandmarkList!![LANDMARK_RIGHT_INNER].x()
                        val deltaRightOuterX =
                            landmarkList[LANDMARK_RIGHT_OUTER].x() - markLandmarkList!![LANDMARK_RIGHT_OUTER].x()
                        val deltaLeftOuterY =
                            landmarkList[LANDMARK_LEFT_OUTER].y() - markLandmarkList!![LANDMARK_LEFT_OUTER].y()
                        val deltaLeftInnerY =
                            landmarkList[LANDMARK_LEFT_INNER].y() - markLandmarkList!![LANDMARK_LEFT_INNER].y()
                        val deltaRightInnerY =
                            landmarkList[LANDMARK_RIGHT_INNER].y() - markLandmarkList!![LANDMARK_RIGHT_INNER].y()
                        val deltaRightOuterY =
                            landmarkList[LANDMARK_RIGHT_OUTER].y() - markLandmarkList!![LANDMARK_RIGHT_OUTER].y()
                        Log.d(
                            TAG, "${markIntervalCount} landmarks at $timeMs:" +
                                    "\n...left outer delta X, Y->$deltaLeftOuterX, $deltaLeftOuterY" +
                                    "\n...left inner delta X, Y ->$deltaLeftInnerX, $deltaLeftInnerY" +
                                    "\n...right inner delta X, Y->$deltaRightInnerX, $deltaRightInnerY" +
                                    "\n...right outer delta X, Y->$deltaRightOuterX, $deltaRightOuterY"
                        )
                        // sum delta X, Y -> X > Y !Yes, Y > X !No
                        sumDeltaX =
                            abs(deltaLeftOuterX + deltaLeftInnerX + deltaRightInnerX + deltaRightOuterX)
                        sumDeltaY =
                            abs(deltaLeftOuterY + deltaLeftInnerY + deltaRightInnerY + deltaRightOuterY)
                    }
                }
            }
        }
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
        invalidate()
    }

    companion object {
        private const val LANDMARK_STROKE_WIDTH = 8F
        //private const val TAG = "Face Landmarker Overlay"
        private const val TAG = "AhaFaceX"

        const val LANDMARK_LEFT_OUTER = 37
        const val LANDMARK_LEFT_INNER = 40
        const val LANDMARK_RIGHT_INNER = 43
        const val LANDMARK_RIGHT_OUTER = 46

    }
}
