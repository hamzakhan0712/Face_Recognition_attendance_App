package com.alphacorp.face_recognition_app

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class StudentCredentialsActivity : AppCompatActivity() {

    private lateinit var editTextUsername: EditText
    private lateinit var editTextPassword: EditText
    private lateinit var editTextConfirmPassword: EditText
    private lateinit var buttonGenerate: Button
    private var email: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_student_credentials)

        // Initialize views
        editTextUsername = findViewById(R.id.editTextUsername)
        editTextPassword = findViewById(R.id.editTextPassword)
        editTextConfirmPassword = findViewById(R.id.editTextConfirmPassword)
        buttonGenerate = findViewById(R.id.buttonGenerate)

        // Get the verified student ID from the intent
        email = intent.getStringExtra("email")

        // Set up the generate button click listener
        buttonGenerate.setOnClickListener {
            // Handle button click event
            generateCredentials()
        }
    }

    private fun generateCredentials() {
        // Retrieve entered data
        val username = editTextUsername.text.toString()
        val password = editTextPassword.text.toString()
        val confirmPassword = editTextConfirmPassword.text.toString()

        // Validate data, e.g., check if passwords match, not empty, etc.
        val isValidData = validateData(username, password, confirmPassword)

        // Proceed if data is valid
        if (isValidData) {
            // Call a function or API to create student credentials using the verifiedStudentId
            createStudentCredentials(email, username, password)
        } else {
            // Handle invalid data scenario, show error messages, etc.
        }
    }

    private fun validateData(username: String, password: String, confirmPassword: String): Boolean {
        // Implement your validation logic here
        // Example: Check if fields are not empty, passwords match, etc.
        return !username.isEmpty() && password == confirmPassword && password.isNotEmpty()
    }

    private fun createStudentCredentials(email: String?, username: String, password: String) {

        val apiService = RetrofitInstance.apiService
        val credentialsData = StudentCredentialsData(email ?: "", username, password)

        // Make the API call to create student credentials
        apiService.createStudentCredentials(credentialsData).enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) {
                    // Handle success: Credentials created
                    val intent = Intent(this@StudentCredentialsActivity, StudentHomeActivity::class.java)
                    intent.putExtra("USERNAME", username)
                    startActivity(intent)
                    finish() // Finish current activity if navigating to the home activity
                } else {
                    // Handle failure: Credentials creation failed
                    val errorBody = response.errorBody()?.string()
                    if (!errorBody.isNullOrBlank()) {
                        // If there's an error message from the server, show it
                        Toast.makeText(this@StudentCredentialsActivity, "Error: $errorBody", Toast.LENGTH_SHORT).show()
                    } else {
                        // If no error message from the server, show a generic message
                        Toast.makeText(this@StudentCredentialsActivity, "Credentials creation failed", Toast.LENGTH_SHORT).show()
                    }
                    Log.e("CredentialsCreation", "Failed to create credentials: ${response.message()}")
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                // Handle network failures or other errors
                Toast.makeText(this@StudentCredentialsActivity, "Failed: ${t.message}", Toast.LENGTH_SHORT).show()
                Log.e("CredentialsCreation", "Network error: ${t.message}")
            }
        })
    }

}
