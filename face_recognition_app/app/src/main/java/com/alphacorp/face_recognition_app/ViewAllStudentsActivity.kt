package com.alphacorp.face_recognition_app

import android.app.DatePickerDialog
import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import okhttp3.ResponseBody
import org.json.JSONException
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import androidx.core.content.ContextCompat


class ViewAllStudentsActivity : AppCompatActivity() {

    private lateinit var apiService: ApiService
    private lateinit var teacher: String
    private lateinit var editTextSearch: EditText
    private lateinit var linearLayoutStudentAttendance: LinearLayout
    private lateinit var calendar: Calendar

    private lateinit var originalRegisteredStudents: List<RegisteredStudent>
    private lateinit var displayedRegisteredStudents: List<RegisteredStudent>
    private val searchHandler = Handler(Looper.getMainLooper())
    private val searchDelay: Long = 500

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_all_students)
        editTextSearch = findViewById(R.id.editTextSearch)

        apiService = RetrofitInstance.apiService
        teacher = intent.getStringExtra("USERNAME") ?: ""
        val textUsername: TextView = findViewById(R.id.textUsername)
        textUsername.text = teacher
        calendar = Calendar.getInstance()

        linearLayoutStudentAttendance = findViewById(R.id.linearLayoutStudentAttendance)
        originalRegisteredStudents()

        editTextSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // Do nothing
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // Handle text changes and update the displayed cards accordingly
                // Use the handler to delay API calls during typing
                searchHandler.removeCallbacksAndMessages(null)
                searchHandler.postDelayed({
                    filterStudent(s.toString())
                }, searchDelay)
            }

            override fun afterTextChanged(s: Editable?) {
                // Do nothing
            }
        })
    }

    private fun originalRegisteredStudents() {
        // Fetch and store the original list of registered students from the server
        val call = RetrofitInstance.apiService.getAllRegisteredStudents(teacher)

        call.enqueue(object : Callback<List<RegisteredStudent>> {
            override fun onResponse(
                call: Call<List<RegisteredStudent>>,
                response: Response<List<RegisteredStudent>>
            ) {
                if (response.isSuccessful) {
                    originalRegisteredStudents = response.body().orEmpty()
                    displayedRegisteredStudents = originalRegisteredStudents
                    updateDisplayedCards(displayedRegisteredStudents)
                } else {
                    // Handle error response
                }
            }

            override fun onFailure(call: Call<List<RegisteredStudent>>, t: Throwable) {
                // Handle failure
            }
        })
    }

    private fun filterStudent(query: String) {
        // Trigger API call to Django server based on the search query
        searchStudent(query)
    }

    private fun searchStudent(query: String) {
        if (query.isNotEmpty()) {
            val apiService = RetrofitInstance.apiService
            val call = apiService.searchStudent(teacher, query)

            call.enqueue(object : Callback<List<RegisteredStudent>> {
                override fun onResponse(
                    call: Call<List<RegisteredStudent>>,
                    response: Response<List<RegisteredStudent>>
                ) {
                    if (response.isSuccessful) {
                        // Registered students fetched successfully, handle the response
                        val registeredStudents = response.body().orEmpty()
                        displayedRegisteredStudents = registeredStudents
                        updateDisplayedCards(displayedRegisteredStudents)
                    } else {
                        // Handle error response
                        Log.e("SearchAPI", "Error: ${response.code()}")
                    }
                }

                override fun onFailure(call: Call<List<RegisteredStudent>>, t: Throwable) {
                    // Handle failure
                    Log.e("SearchAPI", "Error: ${t.message}")
                }
            })
        } else {
            // If the search query is empty, show all original registered students
            displayedRegisteredStudents = originalRegisteredStudents
            updateDisplayedCards(displayedRegisteredStudents)
        }
    }

    private fun updateDisplayedCards(registeredStudents: List<RegisteredStudent>) {
        linearLayoutStudentAttendance.removeAllViews() // Clear existing views

        registeredStudents.forEach { registeredStudent ->
            addRegisteredStudentRow(registeredStudent)
        }
    }



    private fun getAllStudents(teacher: String) {
        apiService.getAllRegisteredStudents(teacher).enqueue(object : Callback<List<RegisteredStudent>> {
            override fun onResponse(call: Call<List<RegisteredStudent>>, response: Response<List<RegisteredStudent>>) {
                if (response.isSuccessful) {
                    val studentsList = response.body()
                    displayStudents(studentsList)
                } else {
                    // Handle unsuccessful response
                }
            }

            override fun onFailure(call: Call<List<RegisteredStudent>>, t: Throwable) {
                // Handle failure
            }
        })
    }

    private fun updateStudent(studentId: Int, updatedStudent: RegisteredStudent) {
        apiService.updateStudent(studentId, updatedStudent).enqueue(object : Callback<RegisteredStudent> {
            override fun onResponse(call: Call<RegisteredStudent>, response: Response<RegisteredStudent>) {
                if (response.isSuccessful) {
                    // Handle successful update
                    // You might want to refresh the list after updating
                    getAllStudents(teacher)
                } else {
                    // Handle unsuccessful update
                    val errorMessage = "Update failed with response code: ${response.code()}"
                    Log.e("UpdateStudent", errorMessage)

                    // You can also log the response body or any other relevant information
                    val responseBody = response.errorBody()?.string()
                    if (!responseBody.isNullOrBlank()) {
                        Log.e("UpdateStudent", "Response Body: $responseBody")
                    }
                }
            }

            override fun onFailure(call: Call<RegisteredStudent>, t: Throwable) {
                // Handle failure
                val errorMessage = "Update failed due to network failure: ${t.message}"
                Log.e("UpdateStudent", errorMessage)
            }
        })
    }


    private fun deleteStudent(studentId: Int) {
        apiService.deleteRegisteredStudent(studentId).enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    // Handle successful deletion
                    // You might want to refresh the list after deleting
                    getAllStudents(teacher)
                } else {
                    // Handle unsuccessful deletion
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                // Handle failure
            }
        })
    }


    private fun displayStudents(studentsList: List<RegisteredStudent>?) {
        linearLayoutStudentAttendance.removeAllViews()

        studentsList?.forEach { student ->
            addRegisteredStudentRow(student)
        }
    }

    private fun addRegisteredStudentRow(registeredStudent: RegisteredStudent) {
        val cardView = CardView(this)
        val cardLayoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        cardLayoutParams.setMargins(0, 0, 0, 16)
        cardView.layoutParams = cardLayoutParams

        val detailsLayout = LinearLayout(this)
        detailsLayout.orientation = LinearLayout.VERTICAL
        cardView.addView(detailsLayout)

        val name = registeredStudent.full_name ?: "N/A"
        val department = registeredStudent.department ?: "N/A"
        val year = registeredStudent.year ?: "N/A"
        val gender = registeredStudent.gender ?: "N/A"
        val age = registeredStudent.age?.toString() ?: "N/A"
        val dob = registeredStudent.date_of_birth ?: "N/A"

        val nameTextView = TextView(this)
        nameTextView.text = "Name: $name"
        nameTextView.textSize = 18f
        nameTextView.setTypeface(null, Typeface.BOLD)
        nameTextView.setTextColor(ContextCompat.getColor(this, R.color.black))
        nameTextView.setPadding(32, 16, 32, 8)

        val detailsTextView = TextView(this)
        val details = "Dept: $department\nYear: $year\nGender: $gender\nAge: $age\nDOB: $dob"
        detailsTextView.text = details
        detailsTextView.textSize = 16f
        detailsTextView.setTextColor(ContextCompat.getColor(this, R.color.gray))
        detailsTextView.setPadding(32, 0, 32, 16)

        detailsLayout.addView(nameTextView)
        detailsLayout.addView(detailsTextView)

        linearLayoutStudentAttendance.addView(cardView)

        // Set up long-click listener
        cardView.setOnLongClickListener {
            showPopupMenu(it, registeredStudent)
            true
        }
    }


    private fun showPopupMenu(view: View, student: RegisteredStudent) {
        val popupMenu = PopupMenu(this, view)
        popupMenu.menuInflater.inflate(R.menu.student_card_menu, popupMenu.menu)

        popupMenu.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.menuUpdateStudent -> {
                    // Handle update student action
                    showUpdateDialog(student)
                    true
                }
                R.id.menuDeleteStudent -> {
                    // Handle delete student action
                    showDeleteConfirmationDialog(student)
                    true
                }
                else -> false
            }
        }

        popupMenu.show()
    }



    private fun showUpdateDialog(student: RegisteredStudent) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Update Student")

        // Inflate a custom layout for the dialog
        val view = layoutInflater.inflate(R.layout.update_student_dialog, null)
        // Set initial values in the input fields
        view.findViewById<EditText>(R.id.inputFullName).setText(student.full_name)
        // Set values for other input fields


        // Set initial values in the input fields
        // Set initial values in the input fields
        view.findViewById<EditText>(R.id.inputFullName).setText(student.full_name)
        view.findViewById<EditText>(R.id.inputEmail).setText(student.email)
        view.findViewById<EditText>(R.id.inputPhoneNumber).setText(student.phone_number)
        view.findViewById<EditText>(R.id.inputAddress).setText(student.address)
        view.findViewById<EditText>(R.id.inputAge).setText(student.age.toString())
        view.findViewById<EditText>(R.id.inputDateOfBirth).setText(student.date_of_birth)

        // Set values for spinners
        setSpinnerSelection(view.findViewById(R.id.inputYearSpinner), student.year)
        setSpinnerSelection(view.findViewById(R.id.inputDivisionSpinner), student.department)
        setSpinnerSelection(view.findViewById(R.id.inputGenderSpinner), student.gender)

        view.findViewById<EditText>(R.id.inputDateOfBirth).setOnClickListener {
            showDatePickerDialog(view.findViewById<EditText>(R.id.inputDateOfBirth))
        }
        // Add other input fields for remaining attributes

        builder.setView(view)

        builder.setPositiveButton("Update") { dialog, _ ->
            // Handle update action
            val updatedFullName = view.findViewById<EditText>(R.id.inputFullName).text.toString()
            val updatedEmail = view.findViewById<EditText>(R.id.inputEmail).text.toString()
            val updatedPhoneNumber = view.findViewById<EditText>(R.id.inputPhoneNumber).text.toString()
            val updatedAddress = view.findViewById<EditText>(R.id.inputAddress).text.toString()
            val updatedAge = view.findViewById<EditText>(R.id.inputAge).text.toString().toIntOrNull() ?: 0
            val updatedDateOfBirth = view.findViewById<EditText>(R.id.inputDateOfBirth).text.toString()

            // Retrieve values for spinners
            val updatedYear = getSpinnerSelection(view.findViewById(R.id.inputYearSpinner))
            val updatedDepartment = getSpinnerSelection(view.findViewById(R.id.inputDivisionSpinner))
            val updatedGender = getSpinnerSelection(view.findViewById(R.id.inputGenderSpinner))

            // Retrieve values for other updated fields

            // Create a new RegisteredStudent with the updated values
            val updatedStudent = RegisteredStudent(
                student.id,
                updatedFullName,
                updatedEmail,
                updatedPhoneNumber,
                updatedAddress,
                updatedYear,
                updatedDepartment,
                updatedGender,
                updatedAge,
                updatedDateOfBirth,
                null,
                null

            )

            // Call the API service to update the student details
            updateStudent(student.id, updatedStudent)

            // You may also refresh the student list after a successful update
        }

        builder.setNegativeButton("Cancel") { dialog, _ ->
            dialog.cancel()
        }

        builder.show()
    }

    private fun setSpinnerSelection(spinner: Spinner, value: String?) {
        val position = (spinner.adapter as ArrayAdapter<String>).getPosition(value)
        if (position >= 0) {
            spinner.setSelection(position)
        }
    }

    // Helper function to get spinner selection
    private fun getSpinnerSelection(spinner: Spinner): String {
        return spinner.selectedItem.toString()
    }



    // Update the showDatePickerDialog function to take an EditText parameter
    private fun showDatePickerDialog(dateOfBirthEditText: EditText) {
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


    private fun showDeleteConfirmationDialog(student: RegisteredStudent) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Delete Student")
        builder.setMessage("Are you sure you want to delete ${student.full_name}?")

        builder.setPositiveButton("Delete") { dialog, _ ->
            // Handle delete action
            // Call the API service to delete the student
            deleteStudent(student.id)

            // You may also refresh the student list after successful deletion
        }

        builder.setNegativeButton("Cancel") { dialog, _ ->
            dialog.cancel()
        }

        builder.show()
    }


}
