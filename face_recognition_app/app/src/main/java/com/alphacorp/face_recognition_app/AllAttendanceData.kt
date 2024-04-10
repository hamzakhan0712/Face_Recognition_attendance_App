package com.alphacorp.face_recognition_app

import com.google.gson.annotations.SerializedName

data class AllAttendanceData(
    @SerializedName("id") val id: Int,
    @SerializedName("student") val student: RegisteredStudent,
    @SerializedName("teacher") val teacher: String,
    @SerializedName("lecture") val lecture: LectureData,
    @SerializedName("datetime") val datetime: String,
    @SerializedName("isPresent") val isPresent: Boolean

){
    fun matchesQuery(query: String): Boolean {
        return student.full_name.contains(query, true) ||
                student.department.contains(query, true) ||
                student.year.contains(query, true) ||
                lecture.subject.contains(query, true) ||
                student.id.toString().contains(query, true)
    }
}
