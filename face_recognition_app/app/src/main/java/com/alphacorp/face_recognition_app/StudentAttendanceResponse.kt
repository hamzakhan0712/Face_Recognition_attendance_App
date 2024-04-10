package com.alphacorp.face_recognition_app

import com.google.gson.annotations.SerializedName

data class StudentAttendanceResponse(
    val id: Int,
    val student: StudentData,
    val teacher: Any, // Change the type to handle both String and TeacherData
    val lecture: LectureData,
    val datetime: String,
    val is_present: Boolean
) {
    // Other existing code

    fun matchesQuery(query: String): Boolean {
        // Check if any attribute matches the query
        return lecture.subject.contains(query, true) ||
                student.division.contains(query, true) ||
                student.year.contains(query, true) ||
                datetime.contains(query, true) ||
                teacher.toString().contains(query, true) // Handle teacher field
    }
    fun isPresent(): Boolean {
        return is_present
    }
}
