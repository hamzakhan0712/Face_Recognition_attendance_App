package com.alphacorp.face_recognition_app

import com.google.gson.annotations.SerializedName
import java.util.Date

data class RegisteredStudent(
    val id: Int,
    val full_name: String,
    val email: String,
    val phone_number: String,
    val address: String,
    val year: String,
    val department: String,
    val gender: String,
    val age: Int,
    val date_of_birth: String, // Consider converting to a proper date format in Kotlin
    val student_photo: String?, // Make it nullable
    val teacher: String?
)


