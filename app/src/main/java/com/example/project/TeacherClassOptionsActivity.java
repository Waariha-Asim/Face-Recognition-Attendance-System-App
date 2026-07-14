package com.example.project;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;

public class TeacherClassOptionsActivity extends AppCompatActivity {

    TextView txtClassTitle;
    Button btnStartSession, btnStopSession, btnViewAttendance;

    DatabaseReference sessionRef;
    String selectedClassId;

    private static final String DATABASE_URL =
            "https://faceattendancesystem-5fce9-default-rtdb.asia-southeast1.firebasedatabase.app/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teacher_class_options);

        txtClassTitle = findViewById(R.id.txtClassTitle);
        btnStartSession = findViewById(R.id.btnStartSession);
        btnStopSession = findViewById(R.id.btnStopSession);
        btnViewAttendance = findViewById(R.id.btnViewAttendance);

        selectedClassId = getIntent().getStringExtra("classId");

        if (selectedClassId == null || selectedClassId.isEmpty()) {
            selectedClassId = "Unknown";
        }

        txtClassTitle.setText("Class: " + selectedClassId);

        sessionRef = FirebaseDatabase
                .getInstance(DATABASE_URL)
                .getReference("Sessions");

        btnStartSession.setOnClickListener(v -> checkActiveBeforeDialog());

        btnStopSession.setOnClickListener(v -> stopActiveSession());

        btnViewAttendance.setOnClickListener(v -> {
            Intent intent = new Intent(TeacherClassOptionsActivity.this, AttendanceListActivity.class);
            intent.putExtra("classId", selectedClassId);
            startActivity(intent);
        });
    }

    private void checkActiveBeforeDialog() {
        sessionRef.child(selectedClassId)
                .orderByChild("status")
                .equalTo("active")
                .get()
                .addOnSuccessListener(snapshot -> {

                    long currentTime = System.currentTimeMillis();

                    for (DataSnapshot sessionSnapshot : snapshot.getChildren()) {
                        Long endTime = sessionSnapshot.child("endTime").getValue(Long.class);

                        if (endTime != null && currentTime <= endTime) {
                            Toast.makeText(
                                    this,
                                    "A session is already active for " + selectedClassId + ". Stop it first.",
                                    Toast.LENGTH_LONG
                            ).show();
                            return;
                        }

                        if (endTime != null && currentTime > endTime) {
                            sessionSnapshot.getRef().child("status").setValue("expired");
                        }
                    }

                    showDurationDialog();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
    }

    private void showDurationDialog() {
        EditText input = new EditText(this);
        input.setHint("Enter duration in minutes");
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setPadding(40, 25, 40, 25);

        new AlertDialog.Builder(this)
                .setTitle("Start Session for " + selectedClassId)
                .setMessage("Enter attendance session duration")
                .setView(input)
                .setPositiveButton("Start", (dialog, which) -> {
                    String durationText = input.getText().toString().trim();

                    if (durationText.isEmpty()) {
                        Toast.makeText(this, "Duration required", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    long durationMinutes;

                    try {
                        durationMinutes = Long.parseLong(durationText);
                    } catch (Exception e) {
                        Toast.makeText(this, "Invalid duration", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (durationMinutes <= 0) {
                        Toast.makeText(this, "Duration must be greater than 0", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    startAttendanceSession(durationMinutes);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void startAttendanceSession(long durationMinutes) {
        Log.d("SESSION_FLOW", "Starting session for: " + selectedClassId);

        String sessionId = sessionRef.child(selectedClassId).push().getKey();

        long startTime = System.currentTimeMillis();
        long endTime = startTime + (durationMinutes * 60 * 1000);

        HashMap<String, Object> sessionData = new HashMap<>();
        sessionData.put("sessionId", sessionId);
        sessionData.put("teacher", "Madiha");
        sessionData.put("classId", selectedClassId);
        sessionData.put("subject", selectedClassId);
        sessionData.put("status", "active");
        sessionData.put("startTime", startTime);
        sessionData.put("endTime", endTime);
        sessionData.put("durationMinutes", durationMinutes);

        sessionRef.child(selectedClassId).child(sessionId).setValue(sessionData)
                .addOnSuccessListener(unused ->
                        Toast.makeText(this, "Session started for " + selectedClassId, Toast.LENGTH_SHORT).show()
                )
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
    }

    private void stopActiveSession() {
        sessionRef.child(selectedClassId)
                .orderByChild("status")
                .equalTo("active")
                .get()
                .addOnSuccessListener(snapshot -> {

                    if (!snapshot.exists()) {
                        Toast.makeText(this, "No active session for " + selectedClassId, Toast.LENGTH_SHORT).show();
                        return;
                    }

                    boolean stopped = false;
                    long currentTime = System.currentTimeMillis();

                    for (DataSnapshot sessionSnapshot : snapshot.getChildren()) {
                        Long endTime = sessionSnapshot.child("endTime").getValue(Long.class);

                        if (endTime != null && currentTime <= endTime) {
                            sessionSnapshot.getRef().child("status").setValue("stopped");
                            sessionSnapshot.getRef().child("stoppedAt").setValue(currentTime);
                            stopped = true;
                        } else if (endTime != null && currentTime > endTime) {
                            sessionSnapshot.getRef().child("status").setValue("expired");
                        }
                    }

                    if (stopped) {
                        Toast.makeText(this, "Session stopped for " + selectedClassId, Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "No running session found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
    }
}