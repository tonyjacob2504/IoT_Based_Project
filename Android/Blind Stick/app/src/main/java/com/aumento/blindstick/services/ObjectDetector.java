package com.aumento.blindstick.services;

import android.content.Context;
import android.graphics.Bitmap;

import androidx.annotation.NonNull;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;

import com.aumento.blindstick.Utils.ImageUtil;
import com.aumento.blindstick.Utils.OverlayView;
import com.aumento.blindstick.Utils.TFLiteUtil;

import org.tensorflow.lite.Interpreter;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

/**
 * ObjectDetector
 *
 * Acts as the CameraX ImageAnalysis.Analyzer.
 * Responsibilities:
 * - Receives camera frames (ImageProxy)
 * - Converts them to Bitmap
 * - Runs TensorFlow Lite inference
 * - Sends detection results to OverlayView
 */
public class ObjectDetector implements ImageAnalysis.Analyzer {

    // TensorFlow Lite interpreter used to run object detection model
    private Interpreter tflite;

    // Custom view responsible for drawing bounding boxes on screen
    private OverlayView overlay;

    // Flag to prevent overlapping inference calls (avoids parallel execution)
    private boolean isProcessing = false;

    // Timestamp of last detection run (used for throttling)
    private long lastRun = 0;

    /**
     * Constructor
     * Loads the TFLite model and stores overlay reference.
     */
    public ObjectDetector(Context context, OverlayView overlay) {
        this.overlay = overlay;
        // Load TensorFlow Lite model from assets
        tflite = TFLiteUtil.loadModel(context, "detect.tflite");
    }

    /**
     * analyze()
     * Called automatically for every camera frame.
     * Controls frame throttling and triggers detection.
     */
    @Override
    public void analyze(@NonNull ImageProxy image) {
        // Skip frame if:
        // 1) Previous inference still running
        // 2) Last run was too recent (throttle to ~8 FPS)
        if (isProcessing ||
                System.currentTimeMillis() - lastRun < 120) {
            image.close();
            return;
        }

        isProcessing = true;
        lastRun = System.currentTimeMillis();

        // Convert ImageProxy (camera frame) into Bitmap format
        Bitmap bitmap = ImageUtil.toBitmap(image);
        // Perform model inference on converted bitmap
        runDetection(bitmap, image.getWidth(), image.getHeight());

        // IMPORTANT: Always close ImageProxy to prevent camera freeze
        image.close();
        isProcessing = false;
    }

    /**
     * runDetection()
     * Prepares input tensor, runs inference,
     * and forwards detection results to overlay.
     */
    private void runDetection(Bitmap bitmap, int w, int h) {
        // Resize input image to model-required size (300x300)
        Bitmap resized = Bitmap.createScaledBitmap(bitmap, 300, 300, true);
        // Convert resized Bitmap into ByteBuffer (model input tensor)
        ByteBuffer input = ImageUtil.bitmapToBuffer(resized);

        // Output tensor: bounding box coordinates [1][numDetections][4]
        float[][][] boxes = new float[1][10][4];
        // Output tensor: confidence scores for each detection
        float[][] scores = new float[1][10];
        // Output tensor: predicted class indices
        float[][] classes = new float[1][10];

        // Map model output indices to output arrays
        Map<Integer, Object> outputs = new HashMap<>();
        outputs.put(0, boxes);
        outputs.put(1, classes);
        outputs.put(2, scores);

        // Run inference using multiple output tensors
        tflite.runForMultipleInputsOutputs(new Object[]{input}, outputs);

        // Send detection results to overlay for drawing on preview
        overlay.setResults(boxes[0], scores[0], classes[0], w, h);
    }
}