package ru.rut.democamera

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import com.google.common.util.concurrent.ListenableFuture
import ru.rut.democamera.databinding.ActivityVideoBinding
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class VideoActivity : AppCompatActivity() {

    private lateinit var binding: ActivityVideoBinding
    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    private var cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
    private var videoCapture: VideoCapture<Recorder>? = null
    private lateinit var recordExecutor: ExecutorService
    private var recording: Recording? = null

    private var recordStartTime: Long = 0L
    private val timerHandler = Handler(Looper.getMainLooper())
    private val timerRunnable = object : Runnable {
        override fun run() {
            val elapsedSeconds = (System.currentTimeMillis() - recordStartTime) / 1000
            val minutes = elapsedSeconds / 60
            val seconds = elapsedSeconds % 60

            val formattedTime = String.format("%02d:%02d", minutes, seconds)
            binding.recordTimerTxt.text = formattedTime

            timerHandler.postDelayed(this, 1000)
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private val cameraPermissionResult =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val cameraGranted = permissions[Manifest.permission.CAMERA] ?: false
            val audioGranted = permissions[Manifest.permission.RECORD_AUDIO] ?: false

            if (cameraGranted && audioGranted) {
                startCamera()
            } else {
                Snackbar.make(
                    binding.root,
                    "Необходимо разрешение на камеру и микрофон",
                    Snackbar.LENGTH_INDEFINITE
                ).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVideoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        recordExecutor = Executors.newSingleThreadExecutor()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            cameraPermissionResult.launch(arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO
            ))
        } else {
            startCamera()
        }

        binding.switchCameraBtn.setOnClickListener {
            val wasRecording = (recording != null)
            if (wasRecording) {
                stopRecording()
            }
            cameraSelector = if (cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
                CameraSelector.DEFAULT_FRONT_CAMERA
            } else {
                CameraSelector.DEFAULT_BACK_CAMERA
            }
            startCamera()
            if (wasRecording) {
                startRecording()
            }
        }

        binding.recordBtn.setOnClickListener {
            if (recording == null) {
                startRecording()
            } else {
                stopRecording()
            }
        }

        binding.videoGalleryBtn.setOnClickListener {
            val intent = Intent(this, GalleryActivity::class.java)
            startActivity(intent)
        }

        binding.backToPhotoBtn.setOnClickListener {
            finish()
        }
    }

    private fun startCamera() {
        val cameraProvider = cameraProviderFuture.get()

        val preview = androidx.camera.core.Preview.Builder().build().also {
            it.setSurfaceProvider(binding.videoPreview.surfaceProvider)
        }

        val recorder = Recorder.Builder()
            .setQualitySelector(QualitySelector.from(Quality.HD))
            .build()
        videoCapture = VideoCapture.withOutput(recorder)

        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(this, cameraSelector, preview, videoCapture)
        } catch (e: Exception) {
            Log.e("TAG", "Bind to lifecycle failed: ${e.message}")
        }
    }

    private fun startRecording() {
        val videoCapture = this.videoCapture ?: return

        // Проверяем разрешения
        val audioPermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        val cameraPermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        if (!audioPermission || !cameraPermission) {
            Snackbar.make(
                binding.root,
                "Необходимо разрешить доступ к камере и микрофону",
                Snackbar.LENGTH_SHORT
            ).show()
            return
        }

        val fileName = "VID_${System.currentTimeMillis()}.mp4"
        val file = File(externalMediaDirs.firstOrNull(), fileName)
        val outputOptions = FileOutputOptions.Builder(file).build()

        recording = videoCapture.output
            .prepareRecording(this, outputOptions)
            .withAudioEnabled()
            .start(recordExecutor) { recordEvent ->
                when (recordEvent) {
                    is VideoRecordEvent.Start -> {
                        runOnUiThread {
                            binding.recordBtn.setImageResource(R.drawable.record_button_rectangle)
                            startTimer()
                        }
                    }
                    is VideoRecordEvent.Finalize -> {
                        if (!recordEvent.hasError()) {
                            Log.i("TAG", "Видео сохранено: ${file.absolutePath}")
                        } else {
                            Log.e("TAG", "Ошибка записи видео: ${recordEvent.error}")
                        }
                        runOnUiThread {
                            binding.recordBtn.setImageResource(R.drawable.record_button)
                            stopTimer()
                        }
                        recording = null
                    }
                }
            }
    }

    private fun stopRecording() {
        recording?.stop()
        recording = null
        stopTimer()
    }

    private fun startTimer() {
        recordStartTime = System.currentTimeMillis()
        binding.recordTimerTxt.visibility = android.view.View.VISIBLE
        timerHandler.post(timerRunnable)
    }

    private fun stopTimer() {
        timerHandler.removeCallbacks(timerRunnable)
        binding.recordTimerTxt.visibility = android.view.View.GONE
        binding.recordTimerTxt.text = "00:00"
    }

    override fun onDestroy() {
        super.onDestroy()
        recordExecutor.shutdown()
        timerHandler.removeCallbacks(timerRunnable)
    }
}