package com.example.project;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class TeacherLoginActivity extends AppCompatActivity {

    EditText edtTeacherUsername, edtTeacherPassword;
    Button btnTeacherLogin;
    DatabaseReference teacherLoginRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teacher_login);

        edtTeacherUsername = findViewById(R.id.edtTeacherUsername);
        edtTeacherPassword = findViewById(R.id.edtTeacherPassword);
        btnTeacherLogin = findViewById(R.id.btnTeacherLogin);

        teacherLoginRef = FirebaseDatabase
                .getInstance("https://faceattendancesystem-5fce9-default-rtdb.asia-southeast1.firebasedatabase.app/")
                .getReference("TeacherLogin");

        btnTeacherLogin.setOnClickListener(v -> loginTeacher());
    }

    private void loginTeacher() {
        String username = edtTeacherUsername.getText().toString().trim();
        String password = edtTeacherPassword.getText().toString().trim();

        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Enter username and password", Toast.LENGTH_SHORT).show();
            return;
        }

        teacherLoginRef.get().addOnSuccessListener(snapshot -> {
            boolean found = false;

            for (com.google.firebase.database.DataSnapshot teacherSnapshot : snapshot.getChildren()) {
                String dbUsername = teacherSnapshot.child("username").getValue(String.class);
                String dbPassword = teacherSnapshot.child("password").getValue(String.class);

                if (username.equals(dbUsername) && password.equals(dbPassword)) {
                    found = true;
                    Intent intent = new Intent(TeacherLoginActivity.this, TeacherDashboardActivity.class);
                    startActivity(intent);
                    finish();
                    break;
                }
            }

            if (!found) {
                Toast.makeText(this, "Invalid teacher credentials", Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(e ->
                Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show()
        );
    }
}