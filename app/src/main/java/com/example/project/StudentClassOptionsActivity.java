package com.example.project;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class StudentClassOptionsActivity extends AppCompatActivity {

    TextView txtStudentClassTitle, txtStudentRollNo;
    Button btnRegisterFace, btnMarkAttendance;

    String rollNo, classId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_class_options);

        txtStudentClassTitle = findViewById(R.id.txtStudentClassTitle);
        txtStudentRollNo = findViewById(R.id.txtStudentRollNo);
        btnRegisterFace = findViewById(R.id.btnRegisterFace);
        btnMarkAttendance = findViewById(R.id.btnMarkAttendance);

        rollNo = getIntent().getStringExtra("rollNo");
        classId = getIntent().getStringExtra("classId");

        if (rollNo == null || rollNo.isEmpty()) {
            Toast.makeText(this, "Student roll number missing", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        if (classId == null || classId.isEmpty()) {
            Toast.makeText(this, "Class ID missing", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        txtStudentClassTitle.setText("Class: " + classId);
        txtStudentRollNo.setText("Roll No: " + rollNo);

        btnRegisterFace.setOnClickListener(v -> {
            Intent intent = new Intent(StudentClassOptionsActivity.this, FaceRegistrationActivity.class);
            intent.putExtra("rollNo", rollNo);
            intent.putExtra("classId", classId);
            startActivity(intent);
        });

        btnMarkAttendance.setOnClickListener(v -> {
            Intent intent = new Intent(StudentClassOptionsActivity.this, FaceAttendanceActivity.class);
            intent.putExtra("rollNo", rollNo);
            intent.putExtra("classId", classId);
            startActivity(intent);
        });
    }
}