package com.alphacorp.face_recognition_app

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import okhttp3.ResponseBody
import org.json.JSONException
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LandingActivity : AppCompatActivity() {

    private lateinit var apiService: ApiService
    private lateinit var editTextUsername: EditText
    private lateinit var editTextPassword: EditText


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_landing)

        apiService = RetrofitInstance.apiService

        val signupButton: Button = findViewById(R.id.newRegistrationButton)
        editTextUsername = findViewById(R.id.editTextUsername)
        editTextPassword = findViewById(R.id.editTextPassword)

        val loginButton: Button = findViewById(R.id.loginBtn)
        loginButton.setOnClickListener {
            performLogin()
        }

        signupButton.setOnClickListener {
            // Navigate to Signup Activity
            val intent = Intent(this, signup_option_activity::class.java)
            startActivity(intent)// Optional: Finish SignupActivity to prevent going back after successful signup
        }
    }
    private fun performLogin() {
        val username: String = editTextUsername.text.toString().trim()
        val password: String = editTextPassword.text.toString().trim()

        val loginData = LoginData(username, password)

        apiService.login(loginData).enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) {
                    val responseBody: ResponseBody? = response.body()
                    val userTypeJson = responseBody?.string() ?: ""

                    try {
                        val userTypeObj = JSONObject(userTypeJson)
                        val userType = userTypeObj.optString("user_type", "")

                        val intent = when (userType) {
                            "teacher" -> Intent(this@LandingActivity, TeacherHomeActivity::class.java)
                            "student" -> Intent(this@LandingActivity, StudentHomeActivity::class.java)
                            else -> {
                                // Handle unrecognized user type here
                                Log.e("LoginError", "Unrecognized user type: $userType")
                                Toast.makeText(applicationContext, "User type not recognized", Toast.LENGTH_SHORT).show()
                                return
                            }
                        }

                        intent.putExtra("USERNAME", username) // Pass the username to the DashboardActivity
                        startActivity(intent)
                        finish() // Finish LoginActivity
                    } catch (e: JSONException) {
                        Log.e("LoginError", "JSON parsing error: ${e.message}")
                        Toast.makeText(applicationContext, "Error processing server response", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    runOnUiThread {
                        Toast.makeText(applicationContext, "Login failed. Please check your credentials.", Toast.LENGTH_SHORT).show()
                    }
                    Log.e("LoginError", "Error occurred in onResponse: $errorBody")
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                // Handle failure (e.g., network error)
                runOnUiThread {
                    Toast.makeText(applicationContext, "Network error. Please check your internet connection.", Toast.LENGTH_SHORT).show()
                }

                // Log the failure to Logcat
                Log.e("LoginError", "Network error occurred: ${t.message}", t)
            }
        })
    }
}

