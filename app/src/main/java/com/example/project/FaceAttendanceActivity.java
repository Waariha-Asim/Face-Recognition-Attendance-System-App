package com.example.project;

import android.Manifest;
import android.app.AlertDialog;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

@ExperimentalGetImage
public class FaceAttendanceActivity extends AppCompatActivity {

    private PreviewView previewView;
    private TextView txtFaceStatus;

    private DatabaseReference attendanceRef;
    private DatabaseReference studentRef;
    private DatabaseReference studentLoginRef;
    private DatabaseReference sessionRef;

    private FaceRecognitionHelper faceRecognitionHelper;

    private static final int CAMERA_PERMISSION_CODE = 100;

    private boolean eyesOpenDetected = false;
    private boolean eyesClosedDetected = false;
    private boolean blinkVerified = false;
    private boolean attendanceMarked = false;
    private boolean isVerifyingFace = false;
    private boolean attendanceCompleted = false;
    private boolean processingMessageShown = false;

    private android.graphics.Rect detectedFaceRect = null;

    private String rollNo = "";
    private String classId = "";
    private String studentName = "Unknown";

    private FaceDetector detector;

    private static final String DATABASE_URL =
            "https://faceattendancesystem-5fce9-default-rtdb.asia-southeast1.firebasedatabase.app/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_face_attendance);

        previewView = findViewById(R.id.previewView);
        txtFaceStatus = findViewById(R.id.txtFaceStatus);

        rollNo = getIntent().getStringExtra("rollNo");
        classId = getIntent().getStringExtra("classId");

        if (rollNo == null || rollNo.isEmpty()) {
            showErrorDialog("Student roll number missing", true);
            return;
        }

        if (classId == null || classId.isEmpty()) {
            showErrorDialog("Class ID missing", true);
            return;
        }

        FirebaseDatabase database = FirebaseDatabase.getInstance(DATABASE_URL);

        attendanceRef = database.getReference("Attendance");
        studentRef = database.getReference("Students");
        studentLoginRef = database.getReference("StudentLogin");
        sessionRef = database.getReference("Sessions");

        try {
            faceRecognitionHelper = new FaceRecognitionHelper(this);
            Log.d("FACE_MODEL", "Model loaded successfully");
        } catch (Exception e) {
            Log.e("FACE_MODEL", "Model Error: " + e.getMessage());
            showErrorDialog("Model Error: " + e.getMessage(), true);
            return;
        }

        loadStudentName();

        txtFaceStatus.setText("Roll No: " + rollNo + "\nClass: " + classId + "\nShow face and blink");

        checkCameraPermission();
    }

    private void loadStudentName() {
        studentLoginRef.child(rollNo).get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        String name = snapshot.child("name").getValue(String.class);
                        if (name != null && !name.isEmpty()) {
                            studentName = name;
                        }
                    }
                });
    }

    private void checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.CAMERA},
                    CAMERA_PERMISSION_CODE
            );
        }
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                CameraSelector cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA;

                FaceDetectorOptions options =
                        new FaceDetectorOptions.Builder()
                                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                                .build();

                detector = FaceDetection.getClient(options);

                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder().build();

                imageAnalysis.setAnalyzer(
                        ContextCompat.getMainExecutor(this),
                        imageProxy -> detectFace(imageProxy, detector)
                );

                cameraProvider.unbindAll();

                cameraProvider.bindToLifecycle(
                        this,
                        cameraSelector,
                        preview,
                        imageAnalysis
                );

                Log.d("CAMERA_TEST", "Camera Started");

            } catch (Exception e) {
                showErrorDialog("Camera Error: " + e.getMessage(), true);
                Log.e("CAMERA_TEST", "Camera Error: " + e.getMessage());
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void detectFace(ImageProxy imageProxy, FaceDetector detector) {

        if (isVerifyingFace || attendanceCompleted) {
            imageProxy.close();
            return;
        }

        android.media.Image mediaImage = imageProxy.getImage();

        if (mediaImage != null) {
            InputImage image = InputImage.fromMediaImage(
                    mediaImage,
                    imageProxy.getImageInfo().getRotationDegrees()
            );

            detector.process(image)
                    .addOnSuccessListener(faces -> {

                        if (isVerifyingFace || attendanceCompleted) {
                            return;
                        }

                        if (faces.size() > 0) {

                            detectedFaceRect = faces.get(0).getBoundingBox();

                            com.google.mlkit.vision.face.Face face = faces.get(0);

                            Float leftEye = face.getLeftEyeOpenProbability();
                            Float rightEye = face.getRightEyeOpenProbability();

                            if (leftEye != null && rightEye != null) {
                                float avgEyeOpen = (leftEye + rightEye) / 2;

                                if (avgEyeOpen > 0.7 && !eyesOpenDetected) {
                                    eyesOpenDetected = true;
                                    runOnUiThread(() -> txtFaceStatus.setText("Eyes Open Detected"));

                                } else if (avgEyeOpen < 0.3 && eyesOpenDetected && !eyesClosedDetected) {
                                    eyesClosedDetected = true;
                                    runOnUiThread(() -> txtFaceStatus.setText("Blink Detected"));

                                } else if (avgEyeOpen > 0.7 && eyesClosedDetected && !blinkVerified) {
                                    blinkVerified = true;
                                    isVerifyingFace = true;
                                    processingMessageShown = true;

                                    runOnUiThread(() -> txtFaceStatus.setText("Blink Verified - Checking Session..."));
                                    checkActiveSessionThenCheckDuplicate();

                                } else if (!blinkVerified && !processingMessageShown) {
                                    runOnUiThread(() -> txtFaceStatus.setText("Face Detected - Please Blink"));
                                }

                            } else {
                                if (!processingMessageShown) {
                                    runOnUiThread(() -> txtFaceStatus.setText("Face Detected"));
                                }
                            }

                        } else {
                            detectedFaceRect = null;

                            if (!processingMessageShown) {
                                runOnUiThread(() -> txtFaceStatus.setText("No Face Detected"));
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        runOnUiThread(() -> showErrorDialog("Face Detection Error: " + e.getMessage(), false));
                        resetBlink();
                        isVerifyingFace = false;
                    })
                    .addOnCompleteListener(task -> imageProxy.close());
        } else {
            imageProxy.close();
        }
    }

    private void checkActiveSessionThenCheckDuplicate() {
        sessionRef.child(classId)
                .orderByChild("status")
                .equalTo("active")
                .get()
                .addOnSuccessListener(snapshot -> {

                    if (!snapshot.exists()) {
                        runOnUiThread(() -> showErrorDialog("No Active Session for " + classId, false));
                        resetBlink();
                        isVerifyingFace = false;
                        return;
                    }

                    long currentTime = System.currentTimeMillis();
                    boolean validSessionFound = false;

                    for (DataSnapshot sessionSnapshot : snapshot.getChildren()) {
                        Long endTime = sessionSnapshot.child("endTime").getValue(Long.class);

                        if (endTime != null && currentTime <= endTime) {
                            validSessionFound = true;
                            break;
                        }

                        if (endTime != null && currentTime > endTime) {
                            sessionSnapshot.getRef().child("status").setValue("expired");
                        }
                    }

                    if (validSessionFound) {
                        runOnUiThread(() -> txtFaceStatus.setText("Session Active - Checking Attendance..."));
                        checkDuplicateAttendance();
                    } else {
                        runOnUiThread(() -> showErrorDialog("Attendance Time Expired!\nSession has ended.", false));
                        resetBlink();
                        isVerifyingFace = false;
                    }
                })
                .addOnFailureListener(e -> {
                    runOnUiThread(() -> showErrorDialog("Session Error: " + e.getMessage(), false));
                    resetBlink();
                    isVerifyingFace = false;
                });
    }

    private void checkDuplicateAttendance() {
        String date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        attendanceRef.child(classId).child(date).child(rollNo).get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        runOnUiThread(() -> showErrorDialog("Attendance already marked for this class today!", true));
                        attendanceCompleted = true;
                        isVerifyingFace = false;
                    } else {
                        runOnUiThread(() -> txtFaceStatus.setText("Verifying Face..."));
                        verifyFace();
                    }
                })
                .addOnFailureListener(e -> {
                    runOnUiThread(() -> showErrorDialog("Attendance Check Error: " + e.getMessage(), false));
                    resetBlink();
                    isVerifyingFace = false;
                });
    }

    private void verifyFace() {
        if (attendanceMarked || attendanceCompleted) {
            isVerifyingFace = false;
            return;
        }

        Bitmap currentBitmap = previewView.getBitmap();

        if (currentBitmap == null) {
            showErrorDialog("Face image not captured.\nPlease try again.", false);
            resetBlink();
            isVerifyingFace = false;
            return;
        }

        if (detectedFaceRect == null) {
            showErrorDialog("Face Not Detected Properly.\nPlease position your face clearly.", false);
            resetBlink();
            isVerifyingFace = false;
            return;
        }

        if (faceRecognitionHelper == null) {
            showErrorDialog("Face model not loaded.\nPlease restart the app.", true);
            resetBlink();
            isVerifyingFace = false;
            return;
        }

        Bitmap croppedFace = faceRecognitionHelper.cropFace(currentBitmap, detectedFaceRect);
        float[] currentEmbedding = faceRecognitionHelper.getFaceEmbedding(croppedFace);

        studentRef.child(rollNo).child("faceEmbedding").get()
                .addOnSuccessListener(snapshot -> {

                    if (!snapshot.exists()) {
                        showErrorDialog("Face not registered!\nPlease register your face first.", true);
                        resetBlink();
                        isVerifyingFace = false;
                        return;
                    }

                    ArrayList<Float> savedList = new ArrayList<>();

                    for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                        Double value = dataSnapshot.getValue(Double.class);
                        if (value != null) {
                            savedList.add(value.floatValue());
                        }
                    }

                    if (savedList.size() == 0) {
                        showErrorDialog("Registered face data invalid!\nPlease re-register your face.", true);
                        resetBlink();
                        isVerifyingFace = false;
                        return;
                    }

                    float[] savedEmbedding = new float[savedList.size()];

                    for (int i = 0; i < savedList.size(); i++) {
                        savedEmbedding[i] = savedList.get(i);
                    }

                    boolean matched = faceRecognitionHelper.isFaceMatched(savedEmbedding, currentEmbedding);

                    if (matched) {
                        runOnUiThread(() -> txtFaceStatus.setText("Face Verified"));
                        showStudentApprovalDialog();
                    } else {
                        showErrorDialog("Face Not Matched!\nPlease try again with better lighting and clear face.", false);
                        resetBlink();
                        isVerifyingFace = false;
                    }
                })
                .addOnFailureListener(e -> {
                    showErrorDialog("Face Error: " + e.getMessage(), false);
                    resetBlink();
                    isVerifyingFace = false;
                });
    }

    private void showStudentApprovalDialog() {
        runOnUiThread(() -> {
            new AlertDialog.Builder(this)
                    .setTitle("Confirm Student Details")
                    .setMessage(
                            "Name: " + studentName +
                                    "\nRoll No: " + rollNo +
                                    "\nClass: " + classId +
                                    "\n\nIs this your correct information?"
                    )
                    .setCancelable(false)
                    .setPositiveButton("Approve", (dialog, which) -> markAttendance())
                    .setNegativeButton("Not Me", (dialog, which) -> {
                        showErrorDialog("Please verify your face again", false);
                        resetBlink();
                        isVerifyingFace = false;
                    })
                    .show();
        });
    }

    private void markAttendance() {
        if (attendanceMarked || attendanceCompleted) {
            isVerifyingFace = false;
            return;
        }

        attendanceMarked = true;

        String date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        String time = new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(new Date());

        HashMap<String, Object> attendanceData = new HashMap<>();
        attendanceData.put("name", studentName);
        attendanceData.put("rollNo", rollNo);
        attendanceData.put("classId", classId);
        attendanceData.put("date", date);
        attendanceData.put("time", time);
        attendanceData.put("status", "Present");

        attendanceRef.child(classId).child(date).child(rollNo).setValue(attendanceData)
                .addOnSuccessListener(unused -> {
                    attendanceCompleted = true;
                    isVerifyingFace = false;
                    processingMessageShown = true;

                    runOnUiThread(() -> {
                        txtFaceStatus.setText("Attendance Marked Successfully");
                        Toast.makeText(this, "Attendance Marked", Toast.LENGTH_SHORT).show();

                        // Auto close after 2 seconds
                        new android.os.Handler(android.os.Looper.getMainLooper())
        .postDelayed(() -> finish(), 2000);
                    });
                })
                .addOnFailureListener(e -> {
                    attendanceMarked = false;
                    isVerifyingFace = false;
                    showErrorDialog("Attendance Error: " + e.getMessage(), false);
                });
    }

    private void showErrorDialog(String message, boolean finishActivity) {
        runOnUiThread(() -> {
            // Pause face verification while showing dialog
            isVerifyingFace = true;

            AlertDialog.Builder builder = new AlertDialog.Builder(this)
                    .setTitle("Error")
                    .setMessage(message)
                    .setCancelable(false);

            if (finishActivity) {
                // Critical error - only OK button to close activity
                builder.setPositiveButton("OK", (dialog, which) -> finish());
            } else {
                // Non-critical error - Give both options
                builder.setPositiveButton("Try Again", (dialog, which) -> resetAndRetry())
                        .setNegativeButton("Exit", (dialog, which) -> finish());
            }

            builder.show();
        });

    }

    private void resetAndRetry() {
        // Reset all detection flags only - camera continues running
        resetBlink();
        isVerifyingFace = false;
        attendanceMarked = false;
        attendanceCompleted = false;
        processingMessageShown = false;
        detectedFaceRect = null;

        // Reset UI text
        runOnUiThread(() -> {
            txtFaceStatus.setText("Roll No: " + rollNo +
                    "\nClass: " + classId +
                    "\nShow face and blink");
            Toast.makeText(this, "Please try again", Toast.LENGTH_SHORT).show();
        });
    }

    private void resetBlink() {
        blinkVerified = false;
        eyesOpenDetected = false;
        eyesClosedDetected = false;
        processingMessageShown = false;
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera();
            } else {
                showErrorDialog("Camera Permission Denied!\nApp cannot function without camera access.", true);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (detector != null) {
            detector.close();
        }
    }
}