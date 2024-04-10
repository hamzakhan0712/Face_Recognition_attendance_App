package com.alphacorp.face_recognition_app

import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import android.app.Dialog
import android.app.ProgressDialog
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.view.PreviewView
import okhttp3.MediaType
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class FaceRecognitionActivity : AppCompatActivity() {

    private val retrofitInstance = RetrofitInstance.retrofit
    private val apiService = retrofitInstance.create(ApiService::class.java)

    private lateinit var cameraExecutor: ExecutorService
    private lateinit var cameraProvider: ProcessCameraProvider
    private lateinit var preview: Preview
    private var imageCapture: ImageCapture? = null
    private lateinit var imageAnalysis: ImageAnalysis
    private var cameraSelector: CameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
    private val CAMERA_PERMISSION_CODE = 100
    private var photoFile: File? = null
    private lateinit var progressDialog: ProgressDialog
    private var countdownTextView: TextView? = null
    private var countdownTimer: CountDownTimer? = null
    private var countdownDurationMillis = 4000L
    private var teacherUsername: String = ""
    private var subject: String = ""

    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_face_recognition)

        cameraExecutor = Executors.newSingleThreadExecutor()

        teacherUsername = intent.getStringExtra("Teacher") ?: ""
        subject = intent.getStringExtra("Subject") ?: ""

        countdownTextView = findViewById(R.id.countdownTextView)

        startCamera(cameraSelector)

        val switchCameraButton = findViewById<Button>(R.id.switchCameraButton)
        switchCameraButton.setOnClickListener {
            switchCamera()
        }
        startAutomaticCapture()
    }

    private fun startAutomaticCapture() {
        countdownTimer = object : CountDownTimer(countdownDurationMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                countdownDurationMillis = millisUntilFinished
                updateCountdownText()
            }

            override fun onFinish() {
                countdownDurationMillis = 4000L
                updateCountdownText()
                captureImage()
                handler.postDelayed({
                    startAutomaticCapture()
                }, 4000)
            }
        }

        countdownTimer?.start()
    }

    private fun updateCountdownText() {
        runOnUiThread {
            val seconds = (countdownDurationMillis / 1000).toInt()
            countdownTextView?.text = seconds.toString()
            countdownTextView?.visibility = View.VISIBLE
        }
    }

    private fun captureImage() {
        val imageCapture = imageCapture ?: return
        photoFile = File(
            getExternalFilesDir(Environment.DIRECTORY_PICTURES),
            "${System.currentTimeMillis()}.jpeg"
        )

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile!!)
            .build()

        try {
            cameraProvider.bindToLifecycle(
                this, cameraSelector, imageCapture
            )

            imageCapture.takePicture(
                outputOptions,
                ContextCompat.getMainExecutor(this),
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                        showLoadingIndicator()
                        uploadImageToServer(photoFile)
                    }

                    override fun onError(exception: ImageCaptureException) {
                        exception.printStackTrace()
                        showToastAndLog("Error capturing image: ${exception.message}")
                        handler.postDelayed({
                            startAutomaticCapture()
                        }, 4000)
                    }
                }
            )
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("CaptureImage", "Error capturing image: ${e.message}")
        }
    }

    private fun showLoadingIndicator() {
        progressDialog = ProgressDialog(this)
        progressDialog.setMessage("Sending image to server...")
        progressDialog.setCancelable(false)
        progressDialog.show()
    }

    private fun hideLoadingIndicator() {
        // Hide loading indicator, you can implement this based on your UI
        if (progressDialog.isShowing) {
            progressDialog.dismiss()
        }
    }

    private fun uploadImageToServer(photoFile: File?) {
        photoFile ?: return

        try {
            val compressedImageFile = compressImage(photoFile)
            val fileBytes = compressedImageFile.readBytes()
            val requestFile = fileBytes.toRequestBody("image/jpeg".toMediaTypeOrNull())
            val photoBody =
                MultipartBody.Part.createFormData("face_data", compressedImageFile.name, requestFile)
            val teacherUsernameBody =
                teacherUsername.toRequestBody("text/plain".toMediaTypeOrNull())
            val subjectBody =
                subject.toRequestBody("text/plain".toMediaTypeOrNull())
            apiService.markAttendance(teacherUsernameBody,subjectBody, photoBody)
                .enqueue(object : Callback<FaceData> {
                    override fun onResponse(call: Call<FaceData>, response: Response<FaceData>) {
                        hideLoadingIndicator()
                        handleResponse(response)

                    }

                    override fun onFailure(call: Call<FaceData>, t: Throwable) {
                        hideLoadingIndicator()
                        handleFailure(t.message ?: "Unknown error")
                    }
                })

        } catch (e: IOException) {
            e.printStackTrace()
            Log.e("UploadImageToServer", "Error uploading image: ${e.message}")
        }
    }

    private fun handleResponse(response: Response<FaceData>) {
        when (response.code()) {
            200 -> handleSuccess(response.body()?.face_data)
            409 -> handleConflict("Attendance already marked for this face data")
            else -> handleFailure(response.errorBody()?.string() ?: "Unknown error")
        }
    }

    private fun handleSuccess(faceData: String?) {

    }

    private fun handleConflict(errorMessage: String) {

    }

    private fun handleFailure(errorMessage: String) {
        showToastAndLog("Failed to mark attendance: ${cleanResponse(errorMessage)}", 1000000)
    }

    private fun cleanResponse(response: String): String {
        return response.replace("[", "").replace("]", "").replace("{", "").replace("}", "")
    }

    private fun showToastAndLog(message: String, durationMillis: Long = 4000L) {
        val toast = Toast.makeText(this, message, Toast.LENGTH_LONG)
        toast.show()

        Log.d("Attendance", message)

        handler.postDelayed({
            toast.cancel()
        }, durationMillis)
    }

    private fun compressImage(photoFile: File): File {
        val compressedImageFile = File(
            getExternalFilesDir(Environment.DIRECTORY_PICTURES),
            "${System.currentTimeMillis()}_compressed.jpeg"
        )

        try {
            val bitmap = BitmapFactory.decodeFile(photoFile.absolutePath)
            val outputStream = FileOutputStream(compressedImageFile)

            val exif = ExifInterface(photoFile.absolutePath)
            val orientation =
                exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)

            val rotatedBitmap = when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> rotateBitmap(bitmap, 90)
                ExifInterface.ORIENTATION_ROTATE_180 -> rotateBitmap(bitmap, 180)
                ExifInterface.ORIENTATION_ROTATE_270 -> rotateBitmap(bitmap, 270)
                else -> bitmap
            }

            rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
            outputStream.flush()
            outputStream.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }

        return compressedImageFile
    }

    private fun rotateBitmap(bitmap: Bitmap, degrees: Int): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(degrees.toFloat())
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun startCamera(cameraSelector: CameraSelector) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
            preview = Preview.Builder().build()
            imageCapture = ImageCapture.Builder().build()
            imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            try {
                cameraProvider.unbindAll()

                val camera = cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture, imageAnalysis
                )
                val previewView = findViewById<PreviewView>(R.id.cameraView)
                preview.setSurfaceProvider(previewView.surfaceProvider)

            } catch (exc: Exception) {
                exc.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun switchCamera() {
        cameraSelector = if (cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA)
            CameraSelector.DEFAULT_FRONT_CAMERA
        else
            CameraSelector.DEFAULT_BACK_CAMERA

        startCamera(cameraSelector)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera(cameraSelector)
            } else {
                Toast.makeText(
                    this,
                    "Camera permission denied.",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}
