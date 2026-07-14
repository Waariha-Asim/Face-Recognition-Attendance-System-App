package com.example.project;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class StudentDashboardActivity extends AppCompatActivity {

    Button btnStudentClassAI101, btnStudentClassSE102;
    TextView txtStudentInfo;
    String rollNo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_dashboard);

        txtStudentInfo = findViewById(R.id.txtStudentInfo);
        btnStudentClassAI101 = findViewById(R.id.btnStudentClassAI101);
        btnStudentClassSE102 = findViewById(R.id.btnStudentClassSE102);

        rollNo = getIntent().getStringExtra("rollNo");

        if (rollNo == null || rollNo.isEmpty()) {
            rollNo = "Unknown";
        }

        txtStudentInfo.setText("Roll No: " + rollNo);

        btnStudentClassAI101.setOnClickListener(v -> openClassOptions("AI101"));
        btnStudentClassSE102.setOnClickListener(v -> openClassOptions("SE102"));
    }

    private void openClassOptions(String classId) {
        Intent intent = new Intent(StudentDashboardActivity.this, StudentClassOptionsActivity.class);
        intent.putExtra("rollNo", rollNo);
        intent.putExtra("classId", classId);
        startActivity(intent);
    }
}