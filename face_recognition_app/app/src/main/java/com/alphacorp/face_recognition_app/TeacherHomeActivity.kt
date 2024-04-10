package com.alphacorp.face_recognition_app


import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.ViewGroup
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.core.net.ParseException
import java.text.SimpleDateFormat
import java.util.Locale
import android.util.Log
import android.view.Gravity
import android.view.View
import androidx.constraintlayout.widget.ConstraintSet
import androidx.constraintlayout.widget.ConstraintLayout


class TeacherHomeActivity : AppCompatActivity() {
    private lateinit var teacher: String
    private lateinit var linearLayoutAllAttendance: LinearLayout
    private lateinit var editTextSearch: EditText
    private var originalStudentAttendances: List<AllAttendanceData> = emptyList()
    private var displayedStudentAttendances: List<AllAttendanceData> = emptyList()


    // Use a handler to delay API calls for better performance during typing
    private val searchHandler = Handler(Looper.getMainLooper())
    private val searchDelay: Long = 500 // Adjust the delay as needed

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_teacher_dashboard)
        editTextSearch = findViewById(R.id.editTextSearch)

        // Your home screen logic here
        teacher = intent.getStringExtra("USERNAME") ?: ""
        val textUsername: TextView = findViewById(R.id.textUsername)
        textUsername.text = teacher

        linearLayoutAllAttendance = findViewById(R.id.linearLayoutStudentAttendance)

        // Fetch and display student attendance from the server
        getOriginalStudentAttendances()

        val startAttendanceButton: Button = findViewById(R.id.btnFaceRecognition)

        startAttendanceButton.setOnClickListener {
            val intent = Intent(this, SelectLecturesActivity::class.java)
            intent.putExtra("USERNAME", teacher) // Pass the teacher to FaceRecognitionActivity
            startActivity(intent)
        }

        val addStudentButton: Button = findViewById(R.id.btnAddStudent)

        addStudentButton.setOnClickListener {
            val intent = Intent(this, AddStudentActivity::class.java)
            intent.putExtra("USERNAME", teacher) // Pass the teacher to AddStudentActivity
            startActivity(intent)
        }

        val viewAllStudentsButton: Button = findViewById(R.id.btnViewAllStudents)

        viewAllStudentsButton.setOnClickListener {
            val intent = Intent(this, ViewAllStudentsActivity::class.java)
            intent.putExtra("USERNAME", teacher) // Pass the teacher to AddStudentActivity
            startActivity(intent)
        }

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
        val call = RetrofitInstance.apiService.getAllAttendance(teacher)

        call.enqueue(object : Callback<List<AllAttendanceData>> {
            override fun onResponse(
                call: Call<List<AllAttendanceData>>,
                response: Response<List<AllAttendanceData>>
            ) {
                if (response.isSuccessful) {
                    originalStudentAttendances = response.body().orEmpty()
                    displayedStudentAttendances = originalStudentAttendances
                    updateDisplayedCards(displayedStudentAttendances)
                } else {
                    // Handle error response
                }
            }

            override fun onFailure(call: Call<List<AllAttendanceData>>, t: Throwable) {
                // Handle failure
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
            val call = apiService.searchStudentAttendance(teacher, query)

            call.enqueue(object : Callback<List<AllAttendanceData>> {
                override fun onResponse(
                    call: Call<List<AllAttendanceData>>,
                    response: Response<List<AllAttendanceData>>
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

                override fun onFailure(call: Call<List<AllAttendanceData>>, t: Throwable) {
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

    private fun updateDisplayedCards(studentAttendances: List<AllAttendanceData>) {
        linearLayoutAllAttendance.removeAllViews() // Clear existing views

        for (studentAttendance in studentAttendances) {
            addStudentAttendanceRow(studentAttendance)
        }
    }

    private fun restartActivity() {
        val intent = Intent(this, TeacherHomeActivity::class.java)
        intent.putExtra("USERNAME", teacher)
        finish()
        startActivity(intent)
    }

    // Retrofit API service method for getting student attendance
    fun getStudentAttendance(teacher: String) {
        val call = RetrofitInstance.apiService.getAllAttendance(teacher)

        call.enqueue(object : Callback<List<AllAttendanceData>> {
            override fun onResponse(
                call: Call<List<AllAttendanceData>>,
                response: Response<List<AllAttendanceData>>
            ) {
                if (response.isSuccessful) {
                    // Student attendance fetched successfully, handle the response
                    val studentAttendances = response.body()
                    // Display the student attendances in your UI, for example, add rows dynamically
                    for (studentAttendance in studentAttendances.orEmpty()) {
                        addStudentAttendanceRow(studentAttendance)
                    }
                } else {
                    // Handle error response
                }
            }

            override fun onFailure(call: Call<List<AllAttendanceData>>, t: Throwable) {
                // Handle failure
            }
        })
    }


    // Retrofit API service method for deleting student attendance
    fun deleteStudentAttendance(studentAttendanceId: Int) {
        val call = RetrofitInstance.apiService.deleteAttendance(studentAttendanceId)

        call.enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    // Student attendance deleted successfully, handle the response
                    // Remove the corresponding row from UI, if needed
                } else {
                    // Handle error response
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                // Handle failure
            }
        })
    }

    fun addStudentAttendanceRow(studentAttendance: AllAttendanceData) {
        // Create a new CardView as a container
        val cardView = CardView(this)
        val cardLayoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        cardLayoutParams.setMargins(0, 0, 0, 16) // Adjust the bottom margin as needed
        cardView.layoutParams = cardLayoutParams
        cardView.radius = 16f // Set corner radius

        cardView.setCardBackgroundColor(ContextCompat.getColor(this, R.color.white))

        // Create a LinearLayout for the content inside the CardView
        val newRow = LinearLayout(this)
        newRow.orientation = LinearLayout.VERTICAL
        newRow.layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        // TextView for student name
        val textViewStudentName = TextView(this)
        textViewStudentName.text = "${studentAttendance.id}:${studentAttendance.student.full_name}"
        textViewStudentName.textSize = 20f
        textViewStudentName.setTextColor(ContextCompat.getColor(this, R.color.darkgreen))
        textViewStudentName.setPadding(16, 8, 16, 8)

        // TextView for subject, department, and year details
        val textViewDetails = TextView(this)
        val details =
            "Subject: ${studentAttendance.lecture.subject}, Dept: ${studentAttendance.student.department}, Year: ${studentAttendance.student.year}"
        textViewDetails.text = details
        textViewDetails.textSize = 16f
        textViewDetails.setPadding(16, 0, 16, 8)

        // TextView for date and time
        val textViewDateTime = TextView(this)
        try {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS", Locale.getDefault())
            val parsedDate = dateFormat.parse(studentAttendance.datetime)

            // Format the parsed date
            val formattedDateTime =
                SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault()).format(parsedDate!!)

            textViewDateTime.text = formattedDateTime
        } catch (e: ParseException) {
            e.printStackTrace()
            textViewDateTime.text = "Error parsing date"
        }

        textViewDateTime.textSize = 16f
        textViewDateTime.setPadding(16, 0, 16, 8)

        // Button for delete
        val btnDelete = Button(this)
        btnDelete.text = "Remove"
        btnDelete.setTextColor(Color.RED)
        btnDelete.setBackgroundResource(android.R.color.transparent)
        val btnParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        btnParams.gravity = Gravity.END
        btnDelete.layoutParams = btnParams
        btnDelete.setOnClickListener {
            // Call API service to delete the student attendance
            deleteStudentAttendance(studentAttendance.id)
            // After successful deletion, remove the row from UI
            linearLayoutAllAttendance.removeView(cardView)
        }

        // Add views to the LinearLayout
        newRow.addView(textViewStudentName)
        newRow.addView(textViewDetails)
        newRow.addView(textViewDateTime)
        newRow.addView(btnDelete)

        // Add the LinearLayout to the CardView
        cardView.addView(newRow)

        cardView.setOnClickListener {
            // Implement the logic to handle click on the student attendance entry
        }

        // Add the CardView to the parent layout
        linearLayoutAllAttendance.addView(cardView)
    }


}
