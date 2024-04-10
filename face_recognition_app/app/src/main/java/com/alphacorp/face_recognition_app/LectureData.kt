package com.alphacorp.face_recognition_app

import com.google.gson.annotations.SerializedName

data class LectureData(
    @SerializedName("teacher") val teacher: String,
    @SerializedName("subject") val subject: String
)