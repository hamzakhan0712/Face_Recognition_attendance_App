package com.alphacorp.face_recognition_app
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.alphacorp.face_recognition_app.RetrofitInstance
import com.alphacorp.face_recognition_app.StudentVerifyData
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
class VerifyStudentActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_verify_student)

        val editTextFullName = findViewById<EditText>(R.id.editTextFullName)
        val emailEditText = findViewById<EditText>(R.id.emailEditText)
        val editTextPhoneNumber = findViewById<EditText>(R.id.editTextPhoneNumber)
        val yearSpinner = findViewById<Spinner>(R.id.yearSpinner)
        val divisionSpinner = findViewById<Spinner>(R.id.divisionSpinner)
        val verifyButton = findViewById<Button>(R.id.verifyButton)

        verifyButton.setOnClickListener {
            val fullName = editTextFullName.text.toString()
            val email = emailEditText.text.toString()
            val phoneNumber = editTextPhoneNumber.text.toString()
            val selectedYear = yearSpinner.selectedItem.toString()
            val selectedDivision = divisionSpinner.selectedItem.toString()

            val studentVerifyData = StudentVerifyData(
                fullName,
                email,
                phoneNumber,
                selectedYear,
                selectedDivision
            )

            val apiService = RetrofitInstance.apiService

            apiService.verifyStudent(studentVerifyData)
                .enqueue(object : Callback<ResponseBody> {
                    override fun onResponse(
                        call: Call<ResponseBody>,
                        response: Response<ResponseBody>
                    ) {
                        if (response.isSuccessful) {

                            val errorBody = response.errorBody()?.string()
                            Log.e("VerifyStudentActivity", "Verification PASSED: $errorBody")
                            // Student verification successful
                            val intent = Intent(this@VerifyStudentActivity, StudentCredentialsActivity::class.java)
                            intent.putExtra("email", emailEditText.text.toString())
                            startActivity(intent)
                            finish()
                        } else {
                            // Student verification failed
                            val errorBody = response.errorBody()?.string()
                            Log.e("VerifyStudentActivity", "Verification Failed: $errorBody")
                            Toast.makeText(
                                this@VerifyStudentActivity,
                                "Student verification failed",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }

                    override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                        // Handle network failures or other errors
                        Log.e("VerifyStudentData", "Error: ${t.message}")
                        Toast.makeText(
                            this@VerifyStudentActivity,
                            "Error: ${t.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                })

        }
    }
}
