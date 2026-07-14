package com.example.project;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    Button btnTeacher, btnStudent;
    Button btnStudentRegister;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnTeacher = findViewById(R.id.btnTeacher);
        btnStudent = findViewById(R.id.btnStudent);

        btnTeacher.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, TeacherLoginActivity.class);
            startActivity(intent);
        });

        btnStudent.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, StudentLoginActivity.class);
            startActivity(intent);
        });
        btnStudentRegister = findViewById(R.id.btnStudentRegister);

        btnStudentRegister.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, StudentRegisterActivity.class);
            startActivity(intent);
        });
    }
}