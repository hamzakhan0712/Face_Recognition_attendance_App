package com.alphacorp.face_recognition_app

import android.content.Intent
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputLayout
import okhttp3.ResponseBody


class TeacherRegistrationActivity : AppCompatActivity() {

    private lateinit var apiService: ApiService
    private lateinit var editTextFullName: EditText
    private lateinit var editTextEmail: EditText
    private lateinit var editTextContactNo: EditText
    private lateinit var editTextUsername: EditText
    private lateinit var editTextPassword: EditText
    private lateinit var editTextConfirmPassword: EditText
    private lateinit var genderSpinner: Spinner


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_teacher_registration)

        apiService = RetrofitInstance.apiService

        // Initialize EditText fields after setContentView
        editTextFullName = findViewById(R.id.editTextFullName)
        editTextEmail = findViewById(R.id.editTextEmail)
        editTextContactNo = findViewById(R.id.editTextContactNo)
        editTextUsername = findViewById(R.id.editTextUsername)
        editTextPassword = findViewById(R.id.editTextPassword)
        editTextConfirmPassword = findViewById(R.id.editTextConfirmPassword)
        genderSpinner = findViewById(R.id.genderSpinner)

        val textInputLayoutPassword:
                TextInputLayout = findViewById(R.id.textInputLayoutPassword)
        textInputLayoutPassword.endIconMode = TextInputLayout.END_ICON_PASSWORD_TOGGLE
        val registerButton: Button = findViewById(R.id.registerButton)
        registerButton.setOnClickListener {
            performTeacherRegistration()
        }
    }

    private fun performTeacherRegistration() {
        val fullName: String = editTextFullName.text.toString().trim()
        val email: String = editTextEmail.text.toString().trim()
        val contactNo: String = editTextContactNo.text.toString().trim()
        val username: String = editTextUsername.text.toString().trim()
        val password: String = editTextPassword.text.toString().trim()
        val confirmPassword: String = editTextConfirmPassword.text.toString().trim()
        val selectedgender: String = genderSpinner.selectedItem.toString()

        if (password == confirmPassword) {
            val teacherData = TeacherData(fullName, email, contactNo,selectedgender, username, password, )

            apiService.registerTeacher(teacherData).enqueue(object : Callback<ResponseBody> {
                override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                    if (response.isSuccessful) {
                        val intent = Intent(this@TeacherRegistrationActivity, LandingActivity::class.java)
                        intent.putExtra("USERNAME", username)
                        startActivity(intent)
                        finish()
                    } else {
                        val errorBody = response.errorBody()?.string()
                        Log.e("RegistrationError", "Complete Error Body: $errorBody")

                        // Parse the error message to extract field-specific errors
                        val regex = Regex("(\"[a-zA-Z_]+\":\\[\".*?\"\\])")
                        val matchResults = regex.findAll(errorBody ?: "")
                        val fieldErrors = matchResults.map { it.value }.toList()

                        // Display only field-specific errors, excluding non_field_errors
                        val errorMessage = fieldErrors.joinToString(", ") // Concatenate field-specific errors

                        Toast.makeText(applicationContext, errorMessage, Toast.LENGTH_SHORT).show()
                        Log.e("RegistrationError", "Error body: $errorMessage")
                    }

                }

                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    val networkErrorMessage = "Network error. Please check your internet connection."
                    Toast.makeText(applicationContext, networkErrorMessage, Toast.LENGTH_SHORT).show()
                    Log.e("RegistrationError", "Failure: ${t.message}")
                }
            })
        } else {
            Toast.makeText(applicationContext, "Passwords do not match.", Toast.LENGTH_SHORT).show()
        }
    }
}
