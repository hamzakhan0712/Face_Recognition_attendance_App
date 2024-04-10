package com.alphacorp.face_recognition_app

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import androidx.cardview.widget.CardView
import androidx.core.net.ParseException
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.*

class StudentHomeActivity : AppCompatActivity() {
    private lateinit var student: String
    private lateinit var linearLayoutStudentAttendance: LinearLayout
    private lateinit var editTextSearch: EditText
    private var originalStudentAttendances: List<StudentAttendanceResponse> = emptyList()
    private var displayedStudentAttendances: List<StudentAttendanceResponse> = emptyList()

    private val searchHandler = Handler(Looper.getMainLooper())
    private val searchDelay: Long = 500

    private var presentCount: Int = 0
    private var absentCount: Int = 0
    private var totalDays: Int = 0


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_student_dashboard)
        editTextSearch = findViewById(R.id.editTextSearch)

        // Your home screen logic here
        student = intent.getStringExtra("USERNAME") ?: ""
        val textUsername: TextView = findViewById(R.id.textUsername)
        textUsername.text = student

        linearLayoutStudentAttendance = findViewById(R.id.linearLayoutStudentAttendance)

        // Fetch and display student attendance from the server
        getOriginalStudentAttendances()

        val refreshButton: Button = findViewById(R.id.btnRefresh)

        refreshButton.setOnClickListener {
            // Call the method to restart the activity
            restartActivity()
        }

        editTextSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // Do nothing
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // Handle text changes and update the displayed cards accordingly
                // Use the handler to delay API calls during typing
                searchHandler.removeCallbacksAndMessages(null)
                searchHandler.postDelayed({
                    filterStudentAttendance(s.toString())
                }, searchDelay)
            }

            override fun afterTextChanged(s: Editable?) {
                // Do nothing
            }
        })
    }

    private fun getOriginalStudentAttendances() {
        // Fetch and store the original list of student attendance from the server
        val call = RetrofitInstance.apiService.getStudentAttendance(student)

        call.enqueue(object : Callback<List<StudentAttendanceResponse>> {
            override fun onResponse(
                call: Call<List<StudentAttendanceResponse>>,
                response: Response<List<StudentAttendanceResponse>>
            ) {
                if (response.isSuccessful) {
                    originalStudentAttendances = response.body().orEmpty()
                    displayedStudentAttendances = originalStudentAttendances
                    updateDisplayedCards(displayedStudentAttendances)
                } else {
                    // Handle error response
                    Log.e("API", "Error: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<List<StudentAttendanceResponse>>, t: Throwable) {
                // Handle failure
                Log.e("API", "Error: ${t.message}")
            }
        })
    }

    private fun filterStudentAttendance(query: String) {
        // Trigger API call to Django server based on the search query
        searchStudentAttendance(query)
    }

    private fun searchStudentAttendance(query: String) {
        if (query.isNotEmpty()) {
            val apiService = RetrofitInstance.apiService
            val call = apiService.searchEachStudentAttendance(student, query)

            call.enqueue(object : Callback<List<StudentAttendanceResponse>> {
                override fun onResponse(
                    call: Call<List<StudentAttendanceResponse>>,
                    response: Response<List<StudentAttendanceResponse>>
                ) {
                    if (response.isSuccessful) {
                        // Student attendance fetched successfully, handle the response
                        val studentAttendances = response.body().orEmpty()
                        displayedStudentAttendances = studentAttendances
                        updateDisplayedCards(displayedStudentAttendances)
                    } else {
                        // Handle error response
                        Log.e("SearchAPI", "Error: ${response.code()}")
                    }
                }

                override fun onFailure(call: Call<List<StudentAttendanceResponse>>, t: Throwable) {
                    // Handle failure
                    Log.e("SearchAPI", "Error: ${t.message}")
                }
            })
        } else {
            // If the search query is empty, show all original attendances
            displayedStudentAttendances = originalStudentAttendances
            updateDisplayedCards(displayedStudentAttendances)
        }
    }

    private fun updateDisplayedCards(studentAttendances: List<StudentAttendanceResponse>) {
        linearLayoutStudentAttendance.removeAllViews() // Clear existing views

        // Reset counts
        presentCount = 0
        absentCount = 0
        totalDays = studentAttendances.size

        for (studentAttendance in studentAttendances) {
            addStudentAttendanceRow(studentAttendance)
            if (studentAttendance.isPresent()) {
                presentCount++
            } else {
                absentCount++
            }
        }

        // Update counts and percentage
        updateCountsAndPercentage()
    }

    private fun updateCountsAndPercentage() {
        // Now, update the TextViews with the calculated values
        val textPresentCount: TextView = findViewById(R.id.textPresentCount)
        val textAbsentCount: TextView = findViewById(R.id.textAbsentCount)
        val textAverage: TextView = findViewById(R.id.textAverage)

        textPresentCount.text = "Present: $presentCount"

        // Calculate absent days by subtracting present days from total days
        val absentCount = totalDays - presentCount
        textAbsentCount.text = "Absent: $absentCount"

        // Calculate and display percentage based on present count and total days
        val percentage = if (totalDays > 0) (presentCount.toFloat() / totalDays.toFloat()) * 100 else 0f
        textAverage.text = "Percentage: ${String.format("%.2f", percentage)}%"
    }


    private fun restartActivity() {
        val intent = Intent(this, StudentHomeActivity::class.java)
        intent.putExtra("USERNAME", student)
        finish()
        startActivity(intent)
    }


    fun addStudentAttendanceRow(studentAttendance: StudentAttendanceResponse) {
        // Create a new CardView as a container
        val cardView = CardView(this)
        val cardLayoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        cardLayoutParams.setMargins(0, 0, 0, 16) // Adjust the bottom margin as needed
        cardView.layoutParams = cardLayoutParams
        cardView.radius = 16f // Set corner radius
        cardView.cardElevation = 8f // Set card elevation


        cardView.setCardBackgroundColor(ContextCompat.getColor(this, R.color.white))

        // Create a LinearLayout for the content inside the CardView
        val newRow = LinearLayout(this)
        newRow.orientation = LinearLayout.VERTICAL
        newRow.layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        // TextView for subject, department, and year details
        val textViewDetails = TextView(this)
        val details = "Subject: ${studentAttendance.lecture.subject}"
        textViewDetails.text = details
        textViewDetails.textSize = 18f
        textViewDetails.setPadding(16, 10, 16, 8)


        // TextView for teacher details
        val textViewTeacher = TextView(this)
        val teacherDetails = "Teacher: ${studentAttendance.teacher}"
        textViewTeacher.text = teacherDetails
        textViewTeacher.textSize = 16f
        textViewTeacher.setPadding(16, 0, 16, 8)


        // TextView for date and time
        val textViewDateTime = TextView(this)
        try {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS", Locale.getDefault())
            val parsedDate = dateFormat.parse(studentAttendance.datetime)

            // Format the parsed date
            val formattedDateTime =
                SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault()).format(parsedDate!!)
            textViewDateTime.text = "$formattedDateTime"
        } catch (e: ParseException) {
            e.printStackTrace()
            textViewDateTime.text = "Error parsing date"
        }

        textViewDateTime.textSize = 14f
        textViewDateTime.setPadding(16, 0, 16, 10)



        // Add views to the LinearLayout
        newRow.addView(textViewDetails)
        newRow.addView(textViewTeacher)
        newRow.addView(textViewDateTime)

        // Add the LinearLayout to the CardView
        cardView.addView(newRow)

        // Add the CardView to the parent layout
        linearLayoutStudentAttendance.addView(cardView)
    }

}
