package com.alphacorp.face_recognition_app

import com.google.gson.annotations.SerializedName

data class LectureResponse(
    @SerializedName("id") val id: Int,
    @SerializedName("subject") val subject: String,

)