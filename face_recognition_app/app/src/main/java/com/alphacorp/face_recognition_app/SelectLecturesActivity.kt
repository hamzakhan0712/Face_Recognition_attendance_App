package com.alphacorp.face_recognition_app


import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response


class SelectLecturesActivity : AppCompatActivity() {

    private lateinit var teacher: String
    private lateinit var linearLayoutLectures: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_select_lectures)

        // Retrieve username from intent
        teacher = intent.getStringExtra("USERNAME") ?: ""

        linearLayoutLectures = findViewById(R.id.linearLayoutLectures)

        // Fetch and display lectures from the server
        getLectures(teacher)

        val editTextAddLecture: EditText = findViewById(R.id.editTextAddLecture)
        val btnAddLecture: Button = findViewById(R.id.btnAddLecture)

        // Add button click listener to add a new lecture
        btnAddLecture.setOnClickListener {
            val newLectureName = editTextAddLecture.text.toString().trim()

            if (newLectureName.isNotEmpty()) {
                // Call API service to add a new lecture
                createLecture(teacher, newLectureName)

                // Clear the EditText after creating a new lecture
                editTextAddLecture.text.clear()

            } else {
                Toast.makeText(this, "Please enter a lecture name", Toast.LENGTH_SHORT).show()
            }
        }

    }

    fun createLecture(teacher: String, subject: String) {
        val lectureData = LectureData(teacher, subject)
        val call = RetrofitInstance.apiService.createLecture(lectureData)

        call.enqueue(object : Callback<LectureResponse> {
            override fun onResponse(
                call: Call<LectureResponse>,
                response: Response<LectureResponse>
            ) {
                if (response.isSuccessful) {
                    // Lecture created successfully, handle the response
                    linearLayoutLectures.removeAllViews()
                    val createdLecture = response.body()

                    getLectures(teacher)

                    // Do something with the createdLecture
                } else {
                    // Handle error response
                }
            }

            override fun onFailure(call: Call<LectureResponse>, t: Throwable) {
                // Handle failure
            }
        })
    }


    fun getLectures(teacher: String) {
        val call = RetrofitInstance.apiService.getLectures(teacher)

        call.enqueue(object : Callback<List<LectureResponse>> {
            override fun onResponse(
                call: Call<List<LectureResponse>>,
                response: Response<List<LectureResponse>>
            ) {
                if (response.isSuccessful) {
                    // Lectures fetched successfully, handle the response
                    val lectures = response.body()
                    // Display the lectures in your UI, for example, add rows dynamically
                    for (lecture in lectures.orEmpty()) {
                        addLectureRow(lecture.id, lecture.subject)
                    }
                } else {
                    // Handle error response
                }
            }

            override fun onFailure(call: Call<List<LectureResponse>>, t: Throwable) {
                // Handle failure
            }
        })
    }

    fun deleteLecture(lectureId: Int) {
        val call = RetrofitInstance.apiService.deleteLecture(lectureId)

        call.enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    // Lecture deleted successfully, handle the response
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


    private fun addLectureRow(lectureId: Int, lectureName: String) {
        // Create a new CardView as a container
        val cardView = CardView(this)
        val cardLayoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        cardLayoutParams.setMargins(0, 0, 0, 16) // Adjust the bottom margin as needed
        cardView.layoutParams = cardLayoutParams

        // Create a GradientDrawable for background and border
        val gradientDrawable = GradientDrawable()
        gradientDrawable.setColor(Color.WHITE) // Set background color
        gradientDrawable.setStroke(2, Color.parseColor("#CCCCCC")) // Set border color and width
        gradientDrawable.cornerRadius = 16f // Set corner radius

        // Set the GradientDrawable as the background of the CardView
        cardView.background = gradientDrawable

        // LinearLayout for the content inside the CardView
        val newRow = LinearLayout(this)
        newRow.orientation = LinearLayout.HORIZONTAL
        newRow.layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        // TextView for lecture name
        val textViewLectureDetails = TextView(this)
        val lectureDetails = "$lectureName"
        textViewLectureDetails.text = lectureDetails
        textViewLectureDetails.textSize = 18f
        textViewLectureDetails.layoutParams = LinearLayout.LayoutParams(
            0,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            2f
        )
        textViewLectureDetails.setPadding(16, 16, 16, 16) // Apply padding as needed

        // Button for delete
        val btnDelete = Button(this)
        btnDelete.text = "Remove"
        btnDelete.setTextColor(Color.RED)
        btnDelete.setBackgroundResource(android.R.color.transparent)
        btnDelete.layoutParams = LinearLayout.LayoutParams(
            0,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            1f
        )
        btnDelete.setOnClickListener {
            // Call API service to delete the lecture
            deleteLecture(lectureId)
            // After successful deletion, remove the row from UI
            linearLayoutLectures.removeView(cardView)
        }

        newRow.addView(textViewLectureDetails)
        newRow.addView(btnDelete)

        cardView.addView(newRow)

        cardView.setOnClickListener {
            // Start a new activity with the selected lecture name
            val intent = Intent(this, FaceRecognitionActivity::class.java)
            intent.putExtra("Subject", lectureName)
            intent.putExtra("Teacher", teacher)
            startActivity(intent)
            finish()
        }

        linearLayoutLectures.addView(cardView)
    }



}