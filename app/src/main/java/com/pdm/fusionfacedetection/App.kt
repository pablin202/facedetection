package com.pdm.fusionfacedetection

import android.annotation.SuppressLint
import android.app.Application
import androidx.camera.camera2.Camera2Config
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraXConfig

class App : Application(), CameraXConfig.Provider {
    @SuppressLint("UnsafeOptInUsageError")
    override fun getCameraXConfig(): CameraXConfig {
        return CameraXConfig.Builder.fromConfig(Camera2Config.defaultConfig())
            .setAvailableCamerasLimiter(CameraSelector.DEFAULT_BACK_CAMERA)
            .build()
    }
}
