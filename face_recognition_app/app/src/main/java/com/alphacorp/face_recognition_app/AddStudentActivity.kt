package com.alphacorp.face_recognition_app

import android.app.Activity
import android.Manifest
import android.app.DatePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class AddStudentActivity : AppCompatActivity() {

    private lateinit var fullNameEditText: EditText
    private lateinit var studentEmailEditText: EditText
    private lateinit var phoneNumberEditText: EditText
    private lateinit var addressEditText: EditText
    private lateinit var yearSpinner: Spinner
    private lateinit var divisionSpinner: Spinner
    private lateinit var genderSpinner: Spinner
    private lateinit var ageEditText: EditText
    private lateinit var dateOfBirthEditText: EditText
    private lateinit var capturedImageView: ImageView
    private var teacherUsername: String = ""
    private lateinit var calendar: Calendar
    private val REQUEST_IMAGE_CAPTURE = 1
    private val CAMERA_PERMISSION_CODE = 100
    private var photoFile: File? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_student)

        fullNameEditText = findViewById(R.id.fullNameEditText)
        studentEmailEditText = findViewById(R.id.studentEmailEditText)
        phoneNumberEditText = findViewById(R.id.phoneNumberEditText)
        addressEditText = findViewById(R.id.addressEditText)
        yearSpinner = findViewById(R.id.yearSpinner)
        divisionSpinner = findViewById(R.id.divisionSpinner)
        genderSpinner = findViewById(R.id.genderSpinner)
        ageEditText = findViewById(R.id.ageEditText)
        dateOfBirthEditText = findViewById(R.id.dateOfBirthEditText)
        capturedImageView = findViewById(R.id.capturedImageView)

        teacherUsername = intent.getStringExtra("USERNAME") ?: ""

        calendar = Calendar.getInstance()

        val capturePhotoButton: Button = findViewById(R.id.capturePhotoButton)
        capturePhotoButton.setOnClickListener {
            requestCameraPermissionAndCapture()
        }

        val addStudentButton: Button = findViewById(R.id.addStudentButton)
        addStudentButton.setOnClickListener {
            addStudent()
        }

        dateOfBirthEditText.setOnClickListener {
            showDatePickerDialog()
        }
    }

    private fun requestCameraPermissionAndCapture() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            // Permission already granted, proceed with capturing
            dispatchTakePictureIntent()
        } else {
            // Request CAMERA permission
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), CAMERA_PERMISSION_CODE)
        }
    }



    private fun dispatchTakePictureIntent() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (takePictureIntent.resolveActivity(packageManager) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == Activity.RESULT_OK) {
            val imageBitmap = data?.extras?.get("data") as Bitmap
            capturedImageView.setImageBitmap(imageBitmap)
            photoFile = createImageFile(imageBitmap)
        }
    }


    private fun createImageFile(bitmap: Bitmap): File {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val imageFile = File.createTempFile(
            "JPEG_${timeStamp}_",
            ".jpg",
            storageDir
        )
        try {
            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
            val byteArray = stream.toByteArray()

            val fos = FileOutputStream(imageFile)
            fos.write(byteArray)
            fos.flush()
            fos.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return imageFile
    }

    private fun showDatePickerDialog() {
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePicker = DatePickerDialog(
            this,
            { _, selectedYear, selectedMonth, selectedDay ->
                val selectedDate = Calendar.getInstance()
                selectedDate.set(Calendar.YEAR, selectedYear)
                selectedDate.set(Calendar.MONTH, selectedMonth)
                selectedDate.set(Calendar.DAY_OF_MONTH, selectedDay)

                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                dateOfBirthEditText.setText(dateFormat.format(selectedDate.time))
            },
            year,
            month,
            day
        )
        datePicker.show()
    }

    private fun addStudent() {
        val fullName = fullNameEditText.text.toString()
        val email = studentEmailEditText.text.toString()
        val phoneNumber = phoneNumberEditText.text.toString()
        val address = addressEditText.text.toString()
        val year = yearSpinner.selectedItem.toString()
        val department = divisionSpinner.selectedItem.toString()
        val gender = genderSpinner.selectedItem.toString()
        val age = ageEditText.text.toString().toIntOrNull() ?: 0
        val dateOfBirth = dateOfBirthEditText.text.toString()

        if (fullName.isNotEmpty() && email.isNotEmpty() && phoneNumber.isNotEmpty() &&
            address.isNotEmpty() && age > 0 && dateOfBirth.isNotEmpty() && photoFile != null
        ) {
            val requestFile = photoFile!!.asRequestBody("image/*".toMediaTypeOrNull())
            val photoBody = MultipartBody.Part.createFormData("student_photo", photoFile!!.name, requestFile)

            val fullNameBody = fullName.toRequestBody("text/plain".toMediaTypeOrNull())
            val studentEmailBody = email.toRequestBody("text/plain".toMediaTypeOrNull())
            val phoneNumberBody = phoneNumber.toRequestBody("text/plain".toMediaTypeOrNull())
            val addressBody = address.toRequestBody("text/plain".toMediaTypeOrNull())
            val yearBody = year.toRequestBody("text/plain".toMediaTypeOrNull())
            val departmentBody = department.toRequestBody("text/plain".toMediaTypeOrNull())
            val genderBody = gender.toRequestBody("text/plain".toMediaTypeOrNull())
            val ageBody = age.toString().toRequestBody("text/plain".toMediaTypeOrNull())
            val dateOfBirthBody = dateOfBirth.toRequestBody("text/plain".toMediaTypeOrNull())
            val teacherUsernameBody = teacherUsername.toRequestBody("text/plain".toMediaTypeOrNull())

            RetrofitInstance.apiService.addRegisteredStudent(
                fullNameBody,
                studentEmailBody,
                phoneNumberBody,
                addressBody,
                yearBody,
                departmentBody,
                genderBody,
                ageBody,
                dateOfBirthBody,
                teacherUsernameBody,
                photoBody
            ).enqueue(object : Callback<RegisteredStudent> {
                override fun onResponse(
                    call: Call<RegisteredStudent>,
                    response: Response<RegisteredStudent>
                ) {
                    if (response.isSuccessful) {
                        val intent = Intent(this@AddStudentActivity, TeacherHomeActivity::class.java)
                        intent.putExtra("USERNAME", teacherUsername)
                        startActivity(intent)

                        Toast.makeText(this@AddStudentActivity, "Student added successfully", Toast.LENGTH_SHORT).show()
                        finish()
                    } else {
                        val errorBody = response.errorBody()?.string()
                        Log.e("AddStudentActivity", "Error: $errorBody")
                        Toast.makeText(this@AddStudentActivity, "Failed to add student", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<RegisteredStudent>, t: Throwable) {
                    Log.e("AddStudentActivity", "Failed to add student", t)
                    Toast.makeText(this@AddStudentActivity, "Failed to add student", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            CAMERA_PERMISSION_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // CAMERA permission granted, proceed with capturing
                    dispatchTakePictureIntent()
                } else {
                    // CAMERA permission denied, handle accordingly (show a message, etc.)
                    Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
