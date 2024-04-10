package com.alphacorp.face_recognition_app

import com.google.gson.annotations.SerializedName

data class FaceData(
    val teacher_username: String,
    val subject: String,
    val face_data: String // Assuming that the face_data is a String, you may need to adjust the type
)

