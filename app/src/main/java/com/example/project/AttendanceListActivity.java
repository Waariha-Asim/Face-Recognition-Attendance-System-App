package com.example.project;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;

public class AttendanceListActivity extends AppCompatActivity {

    ListView listAttendance;
    ArrayList<String> attendanceList;
    ArrayAdapter<String> adapter;
    DatabaseReference attendanceRef;

    String classId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_attendance_list);

        listAttendance = findViewById(R.id.listAttendance);

        classId = getIntent().getStringExtra("classId");

        if (classId == null || classId.isEmpty()) {
            Toast.makeText(this, "Class ID missing", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        attendanceList = new ArrayList<>();

        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, attendanceList) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);

                TextView textView = view.findViewById(android.R.id.text1);
                textView.setTextColor(getResources().getColor(R.color.soft_white));
                textView.setTextSize(15);
                textView.setPadding(18, 18, 18, 18);

                return view;
            }
        };

        listAttendance.setAdapter(adapter);

        attendanceRef = FirebaseDatabase
                .getInstance("https://faceattendancesystem-5fce9-default-rtdb.asia-southeast1.firebasedatabase.app/")
                .getReference("Attendance")
                .child(classId);

        loadAttendance();
    }

    private void loadAttendance() {
        attendanceRef.get().addOnSuccessListener(snapshot -> {
            attendanceList.clear();

            if (!snapshot.exists()) {
                attendanceList.add("No attendance records found for " + classId);
                adapter.notifyDataSetChanged();
                return;
            }

            for (DataSnapshot dateSnapshot : snapshot.getChildren()) {
                String date = dateSnapshot.getKey();

                for (DataSnapshot studentSnapshot : dateSnapshot.getChildren()) {
                    String name = studentSnapshot.child("name").getValue(String.class);
                    String rollNo = studentSnapshot.child("rollNo").getValue(String.class);
                    String time = studentSnapshot.child("time").getValue(String.class);
                    String status = studentSnapshot.child("status").getValue(String.class);
                    String recordClassId = studentSnapshot.child("classId").getValue(String.class);

                    String record =
                            "Class: " + recordClassId +
                                    "\nDate: " + date +
                                    "\nName: " + name +
                                    "\nRoll No: " + rollNo +
                                    "\nTime: " + time +
                                    "\nStatus: " + status;

                    attendanceList.add(record);
                }
            }

            adapter.notifyDataSetChanged();

        }).addOnFailureListener(e ->
                Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show()
        );
    }
}