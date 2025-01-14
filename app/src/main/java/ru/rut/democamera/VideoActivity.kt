package ru.rut.democamera

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
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

    @RequiresApi(Build.VERSION_CODES.M)
    private val cameraPermissionResult =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            // Проверяем, все ли разрешения получены
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

        // Запрашиваем сразу два разрешения
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            cameraPermissionResult.launch(arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO
            ))
        } else {
            startCamera()
        }

        // Кнопка переключения камеры
        binding.switchCameraBtn.setOnClickListener {
            cameraSelector = if (cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
                CameraSelector.DEFAULT_FRONT_CAMERA
            } else {
                CameraSelector.DEFAULT_BACK_CAMERA
            }
            startCamera()
        }

        // Кнопка записи
        binding.recordBtn.setOnClickListener {
            if (recording == null) {
                startRecording()
            } else {
                stopRecording()
            }
        }

        // Переход в галерею
        binding.videoGalleryBtn.setOnClickListener {
            val intent = Intent(this, GalleryActivity::class.java)
            startActivity(intent)
        }

        // Возврат к фото
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

        // Проверяем, есть ли разрешение на аудио
        val audioPermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        // Проверяем, есть ли разрешение на камеру
        val cameraPermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        if (!audioPermission || !cameraPermission) {
            // Либо запросить разрешения ещё раз, либо уведомить пользователя
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

        // Разрешение проверено, можно включить аудио
        recording = videoCapture.output
            .prepareRecording(this, outputOptions)
            .withAudioEnabled()
            .start(recordExecutor) { recordEvent ->
                when (recordEvent) {
                    is VideoRecordEvent.Start -> {
                        runOnUiThread {
                            binding.recordBtn.setImageResource(android.R.drawable.ic_media_pause)
                        }
                    }
                    is VideoRecordEvent.Finalize -> {
                        if (!recordEvent.hasError()) {
                            Log.i("TAG", "Видео сохранено: ${file.absolutePath}")
                        } else {
                            Log.e("TAG", "Ошибка записи видео: ${recordEvent.error}")
                        }
                        runOnUiThread {
                            binding.recordBtn.setImageResource(android.R.drawable.ic_media_play)
                        }
                        recording = null
                    }
                }
            }
    }

    private fun stopRecording() {
        // Останавливаем запись
        recording?.stop()
        recording = null
    }

    override fun onDestroy() {
        super.onDestroy()
        recordExecutor.shutdown()
    }
}