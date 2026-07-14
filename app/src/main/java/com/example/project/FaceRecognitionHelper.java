package com.example.project;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.Rect;

import org.tensorflow.lite.Interpreter;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public class FaceRecognitionHelper {

    private Interpreter interpreter;

    private static final int INPUT_SIZE = 160;
    private static final int EMBEDDING_SIZE = 128;

    public FaceRecognitionHelper(Context context) throws IOException {
        interpreter = new Interpreter(loadModelFile(context));
    }

    private MappedByteBuffer loadModelFile(Context context) throws IOException {
        AssetFileDescriptor fileDescriptor = context.getAssets().openFd("facenet.tflite");
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();

        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();

        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    public Bitmap cropFace(Bitmap bitmap, Rect rect) {
        int left = Math.max(rect.left, 0);
        int top = Math.max(rect.top, 0);
        int right = Math.min(rect.right, bitmap.getWidth());
        int bottom = Math.min(rect.bottom, bitmap.getHeight());

        int width = right - left;
        int height = bottom - top;

        if (width <= 0 || height <= 0) {
            return bitmap;
        }

        return Bitmap.createBitmap(bitmap, left, top, width, height);
    }

    public float[] getFaceEmbedding(Bitmap bitmap) {
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, INPUT_SIZE, INPUT_SIZE, true);

        float[][][][] input = new float[1][INPUT_SIZE][INPUT_SIZE][3];

        for (int y = 0; y < INPUT_SIZE; y++) {
            for (int x = 0; x < INPUT_SIZE; x++) {
                int pixel = resizedBitmap.getPixel(x, y);

                float r = ((pixel >> 16) & 0xFF);
                float g = ((pixel >> 8) & 0xFF);
                float b = (pixel & 0xFF);

                input[0][y][x][0] = (r - 127.5f) / 128.0f;
                input[0][y][x][1] = (g - 127.5f) / 128.0f;
                input[0][y][x][2] = (b - 127.5f) / 128.0f;
            }
        }

        float[][] output = new float[1][EMBEDDING_SIZE];
        interpreter.run(input, output);

        return output[0];
    }

    public double calculateDistance(float[] embedding1, float[] embedding2) {
        double sum = 0.0;

        int length = Math.min(embedding1.length, embedding2.length);

        for (int i = 0; i < length; i++) {
            double diff = embedding1[i] - embedding2[i];
            sum += diff * diff;
        }

        return Math.sqrt(sum);
    }

    public boolean isFaceMatched(float[] savedEmbedding, float[] currentEmbedding) {
        double distance = calculateDistance(savedEmbedding, currentEmbedding);
        android.util.Log.d("FACE_MATCH", "Distance: " + distance);

        return distance < 1.2;
    }
}