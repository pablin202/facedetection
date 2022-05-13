/*
 * Copyright 2020 Google LLC. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.pdm.fusionfacedetection

import android.content.Context
import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.face.FaceLandmark
import java.util.Locale

/** Face Detector Demo.  */
class FaceDetectorProcessor(
    context: Context,
    detectorOptions: FaceDetectorOptions?,
    private val faceResults: FaceResults
) :
    VisionProcessorBase<List<Face>>(context) {

    private val detector: FaceDetector

    init {
        val options = detectorOptions
            ?: FaceDetectorOptions.Builder()
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                .enableTracking()
                .build()

        detector = FaceDetection.getClient(options)

        Log.v(MANUAL_TESTING_LOG, "Face detector options: $options")
    }

    override fun stop() {
        super.stop()
        detector.close()
    }

    override fun detectInImage(image: InputImage): Task<List<Face>> {
        return detector.process(image)
    }

    override fun onSuccess(results: List<Face>, graphicOverlay: GraphicOverlay) {
        faceResults.faceVisibility(results.isNotEmpty())
        if (results.isNotEmpty()) {
            processFaceResults(results.first())
            for (face in results) {
                // graphicOverlay.add(FaceGraphic(graphicOverlay, face))`
                logExtrasForTesting(face)
            }
        }
    }

    override fun onFailure(e: Exception) {
        Log.e(TAG, "Face detection failed $e")
    }

    private fun processFaceResults(face: Face) {

        var headPoseValid: Boolean
        var expressionOk = false
        var leftEyeOk = false
        var rightEyeOk = false

        face.smilingProbability?.let {
            expressionOk = it <= 0.2f
            faceResults.neutralExpression(it <= 0.2f)
        }

        face.leftEyeOpenProbability?.let {
            leftEyeOk = it > 0.6f
            faceResults.rightEyeStatus(it > 0.6f)
        }

        face.rightEyeOpenProbability?.let {
            rightEyeOk = it > 0.6f
            faceResults.leftEyeStatus(it > 0.6f)
        }

        face.headEulerAngleY.let {
            headPoseValid = when {
                it < -3.5f -> {
                    faceResults.headPose(HeadPoseResult.YAngleToLeft())
                    false
                }
                it > 3.5f -> {
                    faceResults.headPose(HeadPoseResult.YAngleToRight())
                    false
                }
                else -> {
                    true
                }
            }
        }

        if (headPoseValid) {
            face.headEulerAngleX.let {
                headPoseValid = when {
                    it < -5.5f -> {
                        faceResults.headPose(HeadPoseResult.XAngleToDownward())
                        false
                    }
                    it > 3.5f -> {
                        faceResults.headPose(HeadPoseResult.XAngleToUpward())
                        false
                    }
                    else -> {
                        true
                    }
                }
            }
        }

        if (headPoseValid) {
            face.headEulerAngleZ.let {
                headPoseValid = when {
                    it < -2.5f -> {
                        faceResults.headPose(HeadPoseResult.ZAngleToLeft())
                        false
                    }
                    it > 2.5f -> {
                        faceResults.headPose(HeadPoseResult.ZAngleToRight())
                        false
                    }
                    else -> {
                        faceResults.headPose(HeadPoseResult.CorrectPose())
                        true
                    }
                }
            }
        }

        if (headPoseValid) {
            faceResults.headPose(HeadPoseResult.CorrectPose())
        }

        faceResults.requirementsMet(expressionOk && headPoseValid && leftEyeOk && rightEyeOk)
    }

    companion object {
        private const val TAG = "FaceDetectorProcessor"
        private fun logExtrasForTesting(face: Face?) {
            if (face != null) {
                Log.v(
                    MANUAL_TESTING_LOG,
                    "face bounding box: " + face.boundingBox.flattenToString()
                )
                Log.v(
                    MANUAL_TESTING_LOG,
                    "face Euler Angle X: " + face.headEulerAngleX
                )
                Log.v(
                    MANUAL_TESTING_LOG,
                    "face Euler Angle Y: " + face.headEulerAngleY
                )
                Log.v(
                    MANUAL_TESTING_LOG,
                    "face Euler Angle Z: " + face.headEulerAngleZ
                )
                // All landmarks
                val landMarkTypes = intArrayOf(
                    FaceLandmark.MOUTH_BOTTOM,
                    FaceLandmark.MOUTH_RIGHT,
                    FaceLandmark.MOUTH_LEFT,
                    FaceLandmark.RIGHT_EYE,
                    FaceLandmark.LEFT_EYE,
                    FaceLandmark.RIGHT_EAR,
                    FaceLandmark.LEFT_EAR,
                    FaceLandmark.RIGHT_CHEEK,
                    FaceLandmark.LEFT_CHEEK,
                    FaceLandmark.NOSE_BASE
                )
                val landMarkTypesStrings = arrayOf(
                    "MOUTH_BOTTOM",
                    "MOUTH_RIGHT",
                    "MOUTH_LEFT",
                    "RIGHT_EYE",
                    "LEFT_EYE",
                    "RIGHT_EAR",
                    "LEFT_EAR",
                    "RIGHT_CHEEK",
                    "LEFT_CHEEK",
                    "NOSE_BASE"
                )
                for (i in landMarkTypes.indices) {
                    val landmark = face.getLandmark(landMarkTypes[i])
                    if (landmark == null) {
                        Log.v(
                            MANUAL_TESTING_LOG,
                            "No landmark of type: " + landMarkTypesStrings[i] + " has been detected"
                        )
                    } else {
                        val landmarkPosition = landmark.position
                        val landmarkPositionStr =
                            String.format(
                                Locale.US,
                                "x: %f , y: %f",
                                landmarkPosition.x,
                                landmarkPosition.y
                            )
                        Log.v(
                            MANUAL_TESTING_LOG,
                            "Position for face landmark: " +
                                landMarkTypesStrings[i] +
                                " is :" +
                                landmarkPositionStr
                        )
                    }
                }
                Log.v(
                    MANUAL_TESTING_LOG,
                    "face left eye open probability: " + face.leftEyeOpenProbability
                )
                Log.v(
                    MANUAL_TESTING_LOG,
                    "face right eye open probability: " + face.rightEyeOpenProbability
                )
                Log.v(
                    MANUAL_TESTING_LOG,
                    "face smiling probability: " + face.smilingProbability
                )
                Log.v(
                    MANUAL_TESTING_LOG,
                    "face tracking id: " + face.trackingId
                )
            }
        }
    }
}
