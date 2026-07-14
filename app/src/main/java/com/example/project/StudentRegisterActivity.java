package com.example.project;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;

public class StudentRegisterActivity extends AppCompatActivity {

    EditText edtRegName, edtRegRollNo, edtRegPassword;
    Button btnRegisterStudent;

    DatabaseReference studentLoginRef;

    private static final String DATABASE_URL =
            "https://faceattendancesystem-5fce9-default-rtdb.asia-southeast1.firebasedatabase.app/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_register);

        edtRegName = findViewById(R.id.edtRegName);
        edtRegRollNo = findViewById(R.id.edtRegRollNo);
        edtRegPassword = findViewById(R.id.edtRegPassword);
        btnRegisterStudent = findViewById(R.id.btnRegisterStudent);

        studentLoginRef = FirebaseDatabase
                .getInstance(DATABASE_URL)
                .getReference("StudentLogin");

        btnRegisterStudent.setOnClickListener(v -> registerStudent());
    }

    private void registerStudent() {
        String name = edtRegName.getText().toString().trim();
        String rollNo = edtRegRollNo.getText().toString().trim();
        String password = edtRegPassword.getText().toString().trim();

        if (name.isEmpty()) {
            edtRegName.setError("Name required");
            return;
        }

        if (rollNo.isEmpty()) {
            edtRegRollNo.setError("Roll No required");
            return;
        }

        if (password.isEmpty()) {
            edtRegPassword.setError("Password required");
            return;
        }

        if (password.length() < 4) {
            edtRegPassword.setError("Password must be at least 4 characters");
            return;
        }

        studentLoginRef.child(rollNo).get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        Toast.makeText(this, "Student already registered", Toast.LENGTH_SHORT).show();
                    } else {
                        HashMap<String, Object> studentData = new HashMap<>();
                        studentData.put("name", name);
                        studentData.put("rollNo", rollNo);
                        studentData.put("password", password);

                        studentLoginRef.child(rollNo).setValue(studentData)
                                .addOnSuccessListener(unused -> {
                                    Toast.makeText(this, "Student Registered Successfully", Toast.LENGTH_SHORT).show();
                                    finish();
                                })
                                .addOnFailureListener(e ->
                                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show()
                                );
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
    }
}