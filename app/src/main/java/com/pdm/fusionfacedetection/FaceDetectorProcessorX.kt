package com.pdm.fusionfacedetection

import android.util.Log
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageProxy
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.face.FaceLandmark
import java.util.Locale

class FaceDetectorProcessorX(
    detectorOptions: FaceDetectorOptions?,
    private val faceResults: FaceResults
) {

    private val detector: FaceDetector

    init {
        val options = detectorOptions
            ?: FaceDetectorOptions.Builder()
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                .enableTracking()
                .build()

        detector = FaceDetection.getClient(options)

        Log.v(VisionProcessorBase.MANUAL_TESTING_LOG, "Face detector options: $options")
    }

    @ExperimentalGetImage
    fun processImageProxy(image: ImageProxy) {
        detectInImage(InputImage.fromMediaImage(image.image!!, image.imageInfo.rotationDegrees))
            .addOnSuccessListener { results ->
                faceResults.faceVisibility(results.isNotEmpty())
                if (results.isNotEmpty()) {
                    processFaceResults(results.first())
                    for (face in results) {
                        logExtrasForTesting(face)
                    }
                }
            }
            .addOnFailureListener { e: Exception ->
                Log.e(TAG, "Face detection failed $e")
            }
            .addOnCompleteListener { image.close() }
    }

    private fun detectInImage(image: InputImage): Task<List<Face>> {
        return detector.process(image)
    }

    fun stop() {
        detector.close()
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
        private const val TAG = "FaceDetectorProcessorX"
        private fun logExtrasForTesting(face: Face?) {
            if (face != null) {
                Log.v(
                    VisionProcessorBase.MANUAL_TESTING_LOG,
                    "face bounding box: " + face.boundingBox.flattenToString()
                )
                Log.v(
                    VisionProcessorBase.MANUAL_TESTING_LOG,
                    "face Euler Angle X: " + face.headEulerAngleX
                )
                Log.v(
                    VisionProcessorBase.MANUAL_TESTING_LOG,
                    "face Euler Angle Y: " + face.headEulerAngleY
                )
                Log.v(
                    VisionProcessorBase.MANUAL_TESTING_LOG,
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
                            VisionProcessorBase.MANUAL_TESTING_LOG,
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
                            VisionProcessorBase.MANUAL_TESTING_LOG,
                            "Position for face landmark: " +
                                landMarkTypesStrings[i] +
                                " is :" +
                                landmarkPositionStr
                        )
                    }
                }
                Log.v(
                    VisionProcessorBase.MANUAL_TESTING_LOG,
                    "face left eye open probability: " + face.leftEyeOpenProbability
                )
                Log.v(
                    VisionProcessorBase.MANUAL_TESTING_LOG,
                    "face right eye open probability: " + face.rightEyeOpenProbability
                )
                Log.v(
                    VisionProcessorBase.MANUAL_TESTING_LOG,
                    "face smiling probability: " + face.smilingProbability
                )
                Log.v(
                    VisionProcessorBase.MANUAL_TESTING_LOG,
                    "face tracking id: " + face.trackingId
                )
            }
        }
    }
}
