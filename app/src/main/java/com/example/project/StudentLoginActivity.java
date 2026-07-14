package com.example.project;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class StudentLoginActivity extends AppCompatActivity {

    EditText edtStudentRollNo, edtStudentPassword;
    Button btnStudentLogin;
    DatabaseReference studentLoginRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_login);

        edtStudentRollNo = findViewById(R.id.edtStudentRollNo);
        edtStudentPassword = findViewById(R.id.edtStudentPassword);
        btnStudentLogin = findViewById(R.id.btnStudentLogin);

        studentLoginRef = FirebaseDatabase
                .getInstance("https://faceattendancesystem-5fce9-default-rtdb.asia-southeast1.firebasedatabase.app/")
                .getReference("StudentLogin");

        btnStudentLogin.setOnClickListener(v -> loginStudent());
    }

    private void loginStudent() {
        String rollNo = edtStudentRollNo.getText().toString().trim();
        String password = edtStudentPassword.getText().toString().trim();

        if (rollNo.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Enter roll no and password", Toast.LENGTH_SHORT).show();
            return;
        }

        studentLoginRef.child(rollNo).get()
                .addOnSuccessListener(snapshot -> {
                    if (!snapshot.exists()) {
                        Toast.makeText(this, "Student not found", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String dbPassword = snapshot.child("password").getValue(String.class);

                    if (password.equals(dbPassword)) {
                        Intent intent = new Intent(StudentLoginActivity.this, StudentDashboardActivity.class);
                        intent.putExtra("rollNo", rollNo);
                        startActivity(intent);
                        finish();
                    } else {
                        Toast.makeText(this, "Invalid password", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
    }
}