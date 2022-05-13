package com.pdm.fusionfacedetection

import android.app.Application
import android.util.Log
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import java.util.concurrent.ExecutionException

class CameraXViewModel(application: Application) : AndroidViewModel(application) {

    var cameraProviderLiveData: MutableLiveData<ProcessCameraProvider>? = null

    fun getProcessCameraProvider(): LiveData<ProcessCameraProvider?>? {
        if (cameraProviderLiveData == null) {
            cameraProviderLiveData = MutableLiveData()
            val cameraProviderFuture =
                ProcessCameraProvider.getInstance(getApplication<Application>())
            cameraProviderFuture.addListener(
                {
                    try {
                        cameraProviderLiveData!!.setValue(cameraProviderFuture.get())
                    } catch (e: ExecutionException) {
                        // Handle any errors (including cancellation) here.
                        Log.e(
                            TAG,
                            "Unhandled exception",
                            e
                        )
                    } catch (e: InterruptedException) {
                        Log.e(
                            TAG,
                            "Unhandled exception",
                            e
                        )
                    }
                },
                ContextCompat.getMainExecutor(getApplication<Application>())
            )
        }
        return cameraProviderLiveData
    }
    companion object {
        private const val TAG = "CameraXViewModel"
    }
}
