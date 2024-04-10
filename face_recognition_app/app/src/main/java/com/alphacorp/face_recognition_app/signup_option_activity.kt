package com.alphacorp.face_recognition_app

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class signup_option_activity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup_option)

        val newTeacherRegistration: Button = findViewById(R.id.buttonTeacherRegistration)
        newTeacherRegistration.setOnClickListener {
            val intent = Intent(this, TeacherRegistrationActivity::class.java)
            startActivity(intent)
            finish()
        }

        val newStudentRegistration: Button = findViewById(R.id.buttonStudentRegistration)
        newStudentRegistration.setOnClickListener {
            val intent = Intent(this, VerifyStudentActivity::class.java)
            startActivity(intent)
            finish()
        }

    }
}
