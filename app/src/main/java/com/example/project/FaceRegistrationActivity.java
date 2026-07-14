package com.example.project;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Bundle;
import android.widget.Button;
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
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import java.util.ArrayList;
import java.util.HashMap;

@ExperimentalGetImage
public class FaceRegistrationActivity extends AppCompatActivity {

    Button btnSaveStudent;
    TextView txtRegisterStatus, txtRegisterStudentInfo;
    PreviewView registerPreviewView;

    DatabaseReference studentRef, studentLoginRef;
    FaceRecognitionHelper faceRecognitionHelper;

    private boolean faceDetected = false;
    private boolean alreadyRegistered = false;
    private Rect detectedFaceRect = null;

    private String rollNo, classId, studentName = "";

    private static final int CAMERA_PERMISSION_CODE = 101;
    private static final String DATABASE_URL =
            "https://faceattendancesystem-5fce9-default-rtdb.asia-southeast1.firebasedatabase.app/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_face_registration);

        txtRegisterStudentInfo = findViewById(R.id.txtRegisterStudentInfo);
        btnSaveStudent = findViewById(R.id.btnSaveStudent);
        txtRegisterStatus = findViewById(R.id.txtRegisterStatus);
        registerPreviewView = findViewById(R.id.registerPreviewView);

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

        FirebaseDatabase database = FirebaseDatabase.getInstance(DATABASE_URL);
        studentRef = database.getReference("Students");
        studentLoginRef = database.getReference("StudentLogin");

        try {
            faceRecognitionHelper = new FaceRecognitionHelper(this);
        } catch (Exception e) {
            Toast.makeText(this, "Model Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }

        txtRegisterStudentInfo.setText("Roll No: " + rollNo + "\nClass: " + classId);

        loadStudentDetailsAndCheckFace();

        btnSaveStudent.setOnClickListener(v -> saveStudentFace());

        checkCameraPermission();
    }

    private void loadStudentDetailsAndCheckFace() {
        studentLoginRef.child(rollNo).get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        studentName = snapshot.child("name").getValue(String.class);

                        if (studentName == null || studentName.isEmpty()) {
                            studentName = "Unknown";
                        }

                        txtRegisterStudentInfo.setText(
                                "Name: " + studentName +
                                        "\nRoll No: " + rollNo +
                                        "\nClass: " + classId
                        );
                    }

                    checkAlreadyRegistered();
                })
                .addOnFailureListener(e -> {
                    txtRegisterStatus.setText("Student Load Error: " + e.getMessage());
                    checkAlreadyRegistered();
                });
    }

    private void checkAlreadyRegistered() {
        studentRef.child(rollNo).child("faceEmbedding").get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        alreadyRegistered = true;
                        btnSaveStudent.setEnabled(false);
                        txtRegisterStatus.setText("Face already registered. Registration not allowed again.");
                    } else {
                        alreadyRegistered = false;
                        btnSaveStudent.setEnabled(true);
                        txtRegisterStatus.setText("Face not registered. Show face to register.");
                    }
                })
                .addOnFailureListener(e ->
                        txtRegisterStatus.setText("Check Error: " + e.getMessage())
                );
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
                preview.setSurfaceProvider(registerPreviewView.getSurfaceProvider());

                CameraSelector cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA;

                FaceDetectorOptions options =
                        new FaceDetectorOptions.Builder()
                                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                                .build();

                FaceDetector detector = FaceDetection.getClient(options);

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

                if (!alreadyRegistered) {
                    txtRegisterStatus.setText("Camera Started");
                }

            } catch (Exception e) {
                txtRegisterStatus.setText(e.getMessage());
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void detectFace(ImageProxy imageProxy, FaceDetector detector) {
        android.media.Image mediaImage = imageProxy.getImage();

        if (mediaImage != null) {
            InputImage image = InputImage.fromMediaImage(
                    mediaImage,
                    imageProxy.getImageInfo().getRotationDegrees()
            );

            detector.process(image)
                    .addOnSuccessListener(faces -> {

                        if (alreadyRegistered) {
                            return;
                        }


                        if (faces.size() > 0) {
                            faceDetected = true;
                            detectedFaceRect = faces.get(0).getBoundingBox();
                            txtRegisterStatus.setText("Face Detected - Ready to Register");
                        } else {
                            faceDetected = false;
                            detectedFaceRect = null;
                            txtRegisterStatus.setText("No Face Detected");
                        }
                    })
                    .addOnFailureListener(e ->
                            txtRegisterStatus.setText(e.getMessage())
                    )
                    .addOnCompleteListener(task ->
                            imageProxy.close()
                    );
        } else {
            imageProxy.close();
        }
    }

    private void saveStudentFace() {
        if (alreadyRegistered) {
            Toast.makeText(this, "Face already registered", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!faceDetected || detectedFaceRect == null) {
            Toast.makeText(this, "Please show your face clearly", Toast.LENGTH_SHORT).show();
            return;
        }

        Bitmap bitmap = registerPreviewView.getBitmap();

        if (bitmap == null) {
            Toast.makeText(this, "Face image not captured", Toast.LENGTH_SHORT).show();
            return;
        }

        if (faceRecognitionHelper == null) {
            Toast.makeText(this, "Face model not loaded", Toast.LENGTH_SHORT).show();
            return;
        }

        Bitmap croppedFace = faceRecognitionHelper.cropFace(bitmap, detectedFaceRect);
        float[] embedding = faceRecognitionHelper.getFaceEmbedding(croppedFace);

        ArrayList<Float> embeddingList = new ArrayList<>();
        for (float value : embedding) {
            embeddingList.add(value);
        }

        HashMap<String, Object> studentData = new HashMap<>();
        studentData.put("name", studentName);
        studentData.put("rollNo", rollNo);
        studentData.put("faceEmbedding", embeddingList);
        studentData.put("registeredClassId", classId);

        studentRef.child(rollNo).setValue(studentData)
                .addOnSuccessListener(unused -> {
                    alreadyRegistered = true;
                    btnSaveStudent.setEnabled(false);
                    Toast.makeText(this, "Face Registered Successfully", Toast.LENGTH_SHORT).show();
                    txtRegisterStatus.setText("Face Registered Successfully");
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera();
            } else {
                txtRegisterStatus.setText("Camera Permission Denied");
            }
        }
    }
}