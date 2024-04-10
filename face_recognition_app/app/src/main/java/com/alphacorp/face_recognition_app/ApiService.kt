package com.alphacorp.face_recognition_app

import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.*

interface ApiService {
    @POST("login/")
    fun login(@Body loginData: LoginData): Call<ResponseBody>

    @POST("signup_teacher/") // Update the endpoint URL accordingly
    fun registerTeacher(@Body teacherData: TeacherData): Call<ResponseBody>

    @POST("verify_student/") // Update the endpoint URL accordingly
    fun verifyStudent(@Body studentVerifyData: StudentVerifyData): Call<ResponseBody>

    @POST("create_student_credentials/") // Update the endpoint URL accordingly
    fun createStudentCredentials(@Body studentCredentialsData: StudentCredentialsData): Call<ResponseBody>

    @Multipart
    @POST("registered-students/")
    fun addRegisteredStudent(
        @Part("full_name") fullName: RequestBody,
        @Part("email") email: RequestBody,
        @Part("phone_number") phoneNumber: RequestBody,
        @Part("address") address: RequestBody,
        @Part("year") year: RequestBody,
        @Part("department") department: RequestBody,
        @Part("gender") gender: RequestBody,
        @Part("age") age: RequestBody,
        @Part("date_of_birth") dateOfBirth: RequestBody,
        @Part("teacher") teacher_username: RequestBody,
        @Part photo: MultipartBody.Part // This represents the photo file
    ): Call<RegisteredStudent>

    @GET("registered-students/")
    fun getAllRegisteredStudents(@Query("teacher") teacher: String): Call<List<RegisteredStudent>>

    @GET("search-student/")
    fun searchStudent(
        @Query("teacher") teacher: String,
        @Query("query") query: String
    ): Call<List<RegisteredStudent>>


    @PUT("update-registered-students/{id}/")
    fun updateStudent(@Path("id") studentId: Int, @Body student: RegisteredStudent): Call<RegisteredStudent>

    @DELETE("delete-registered-students/{id}/")
    fun deleteRegisteredStudent(@Path("id") studentId: Int): Call<Void>


    @Multipart
    @POST("mark_attendance/")
    fun markAttendance(
        @Part("teacher_username") teacher: RequestBody,
        @Part("subject") subject: RequestBody,
        @Part photo: MultipartBody.Part // This represents the face image file
    ): Call<FaceData>

    @POST("lectures/") // Replace with your Django API endpoint for creating lectures
    fun createLecture(@Body lectureData: LectureData): Call<LectureResponse>

    @GET("lectures/") // Replace with your Django API endpoint for fetching lectures
    fun getLectures(@Query("teacher_username") teacherUsername: String): Call<List<LectureResponse>>

    @DELETE("delete_lecture/{id}/")
    fun deleteLecture(@Path("id") lectureId: Int): Call<Void>

    @GET("get_all_attendance/")
    fun getAllAttendance(@Query("teacher") teacher: String): Call<List<AllAttendanceData>>

    @DELETE("delete_attendance/{id}/")
    fun deleteAttendance(@Path("id") attendanceId: Int): Call<Void>

    @GET("search-attendance/")
    fun searchStudentAttendance(
        @Query("teacher") teacher: String,
        @Query("query") query: String
    ): Call<List<AllAttendanceData>>

    @GET("student_attendance/")
    fun getStudentAttendance(@Query("student") studentUsername: String): Call<List<StudentAttendanceResponse>>

    @GET("search-student-attendance/")
    fun searchEachStudentAttendance(
        @Query("student") teacher: String,
        @Query("query") query: String
    ): Call<List<StudentAttendanceResponse>>

}

