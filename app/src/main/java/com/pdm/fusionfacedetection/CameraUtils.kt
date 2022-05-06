package com.pdm.fusionfacedetection

import android.content.Context
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.util.Size
import androidx.camera.core.CameraSelector

object CameraUtils {

    fun getCameraBackBestResolution(context: Context): Size? {
        val cameraCharacteristics = getCameraCharacteristics(context, CameraSelector.LENS_FACING_BACK)
        return cameraCharacteristics?.let {
            val map = it.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
            val outputSizes = map!!.getOutputSizes(
                SurfaceTexture::class.java
            )
            outputSizes[0]
        }?: run {
            null
        }
    }

    fun getCameraFrontBestResolution(context: Context) : Size? {
        val cameraCharacteristics = getCameraCharacteristics(context, CameraSelector.LENS_FACING_FRONT)
        return cameraCharacteristics?.let {
            val map = it.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
            val outputSizes = map!!.getOutputSizes(
                SurfaceTexture::class.java
            )
            outputSizes[0]
        }?: run {
            null
        }
    }

    private fun getCameraCharacteristics(context: Context, lensFacing: Int): CameraCharacteristics? {
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try {
            val cameraList = listOf(*cameraManager.cameraIdList)
            for (availableCameraId in cameraList) {
                val availableCameraCharacteristics =
                    cameraManager.getCameraCharacteristics(availableCameraId)
                val availableLensFacing =
                    availableCameraCharacteristics.get(CameraCharacteristics.LENS_FACING)
                        ?: continue
                if (availableLensFacing == lensFacing) {
                    return availableCameraCharacteristics
                }
            }
        } catch (e: CameraAccessException) {
            // Accessing camera ID info got error
        }
        return null
    }
}