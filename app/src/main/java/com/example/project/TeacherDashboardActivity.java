package com.example.project;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class TeacherDashboardActivity extends AppCompatActivity {

    Button btnClassAI101, btnClassSE102;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teacher_dashboard);

        btnClassAI101 = findViewById(R.id.btnClassAI101);
        btnClassSE102 = findViewById(R.id.btnClassSE102);

        btnClassAI101.setOnClickListener(v -> openClassOptions("AI101"));
        btnClassSE102.setOnClickListener(v -> openClassOptions("SE102"));
    }

    private void openClassOptions(String classId) {
        Intent intent = new Intent(TeacherDashboardActivity.this, TeacherClassOptionsActivity.class);
        intent.putExtra("classId", classId);
        startActivity(intent);
    }
}