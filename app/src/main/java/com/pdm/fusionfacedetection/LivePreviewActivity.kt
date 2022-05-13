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

import android.annotation.SuppressLint
import android.content.ContentValues
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.common.annotation.KeepName
import com.google.mlkit.common.MlKitException
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.pdm.fusionfacedetection.databinding.ActivityLivePreviewBinding
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/** Live preview demo for ML Kit APIs. */
@KeepName
class LivePreviewActivity :
    AppCompatActivity(), FaceResults {

    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: ExecutorService
    private var allRequirementsMet = false
    private var analysisUseCase: ImageAnalysis? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var previewUseCase: Preview? = null
    private var cameraSelector: CameraSelector? = null
    private var imageProcessor: FaceDetectorProcessorX? = null

    private lateinit var binding: ActivityLivePreviewBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate")
        cameraSelector = CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build()

        binding = ActivityLivePreviewBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        binding.imgTakePicture.setOnClickListener {
            if (allRequirementsMet) {
                takePhoto()
            }
        }

        cameraExecutor = Executors.newSingleThreadExecutor()

        ViewModelProvider(this, ViewModelProvider.AndroidViewModelFactory.getInstance(application))
            .get(CameraXViewModel::class.java)
            .getProcessCameraProvider()
            ?.observe(
                this
            ) { provider: ProcessCameraProvider? ->
                cameraProvider = provider
                bindAllCameraUseCases()
            }
    }

    public override fun onResume() {
        super.onResume()
        bindAllCameraUseCases()
    }

    override fun onPause() {
        super.onPause()
        imageProcessor?.run { this.stop() }
    }

    public override fun onDestroy() {
        super.onDestroy()
        imageProcessor?.run { this.stop() }
        cameraExecutor.shutdown()
    }

    private fun takePhoto() {
        // Get a stable reference of the modifiable image capture use case
        val imageCapture = imageCapture ?: return

        val name = SimpleDateFormat(FILENAME_FORMAT, Locale.US)
            .format(System.currentTimeMillis())

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Face-Detection")
            }
        }

        // Create output options object which contains file + metadata
        val outputOptions = ImageCapture.OutputFileOptions
            .Builder(
                contentResolver,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues
            )
            .build()

        // Set up image capture listener, which is triggered after photo has
        // been taken
        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val msg = "Photo capture succeeded: ${output.savedUri}"
                    Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
                    Log.d(TAG, msg)
                }
            }
        )
    }

    private fun bindAllCameraUseCases() {
        if (cameraProvider != null) {
            // As required by CameraX API, unbinds all use cases before trying to re-bind any of them.
            cameraProvider!!.unbindAll()
            bindPreviewUseCase()
            bindAnalysisUseCase()
        }
    }

    private fun bindPreviewUseCase() {
        if (cameraProvider == null) {
            return
        }
        if (previewUseCase != null) {
            cameraProvider!!.unbind(previewUseCase)
        }

        previewUseCase = Preview.Builder().build().also {
            it.setSurfaceProvider(binding.previewView.surfaceProvider)
        }

        imageCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()

        cameraProvider!!.bindToLifecycle(/* lifecycleOwner= */ this, cameraSelector!!, previewUseCase, imageCapture)
    }

    @SuppressLint("UnsafeOptInUsageError")
    private fun bindAnalysisUseCase() {
        if (cameraProvider == null) {
            return
        }
        if (analysisUseCase != null) {
            cameraProvider!!.unbind(analysisUseCase)
        }
        if (imageProcessor != null) {
            imageProcessor!!.stop()
        }

        val faceDetectorOptions = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .setMinFaceSize(MIN_FACE_SIZE)
            .enableTracking()
            .build()

        imageProcessor = FaceDetectorProcessorX(faceDetectorOptions, this)

        analysisUseCase = ImageAnalysis.Builder().build()

        analysisUseCase?.setAnalyzer(
            // imageProcessor.processImageProxy will use another thread to run the detection underneath,
            // thus we can just runs the analyzer itself on main thread. cameraExecutor
            ContextCompat.getMainExecutor(this)
        ) { imageProxy: ImageProxy ->
            try {
                imageProcessor!!.processImageProxy(imageProxy)
            } catch (e: MlKitException) {
                Log.e(TAG, "Failed to process image. Error: " + e.localizedMessage)
                Toast.makeText(applicationContext, e.localizedMessage, Toast.LENGTH_SHORT).show()
            }
        }
        cameraProvider!!.bindToLifecycle(/* lifecycleOwner= */ this, cameraSelector!!, analysisUseCase)
    }

    override fun faceVisibility(visible: Boolean) {
        binding.imagesGroup.visibility = if (visible) View.VISIBLE else View.INVISIBLE
        binding.txtFaceNoDetected.visibility = if (visible) View.INVISIBLE else View.VISIBLE
    }

    override fun neutralExpression(isNeutral: Boolean) {
        if (isNeutral) {
            binding.imgExpressionStatus.setOkImage()
        } else {
            binding.imgExpressionStatus.setErrorImage()
        }
    }

    override fun leftEyeStatus(isOpen: Boolean) {
        if (isOpen) {
            binding.imgLeftEyeStatus.setOkImage()
        } else {
            binding.imgLeftEyeStatus.setErrorImage()
        }
    }

    override fun rightEyeStatus(isOpen: Boolean) {
        if (isOpen) {
            binding.imgRightEyeStatus.setOkImage()
        } else {
            binding.imgRightEyeStatus.setErrorImage()
        }
    }

    override fun headPose(headPoseResult: HeadPoseResult) {
        if (headPoseResult.isValid) {
            binding.imgHeadStatus.setOkImage()
            binding.imgMoveTo.visibility = View.INVISIBLE
        } else {
            binding.imgHeadStatus.setErrorImage()
            binding.imgMoveTo.visibility = View.VISIBLE
            headPoseResult.idImage?.let {
                binding.imgMoveTo.setImageResource(it)
            }
        }
    }

    override fun requirementsMet(value: Boolean) {
        allRequirementsMet = value
        if (value) {
            binding.imgTakePicture.setImageResource(R.drawable.ic_camera_ok)
        } else {
            binding.imgTakePicture.setImageResource(R.drawable.ic_camera_error)
        }
    }

    companion object {
        private const val TAG = "LivePreviewActivity"
        const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val MIN_FACE_SIZE = 0.15f
    }
}
