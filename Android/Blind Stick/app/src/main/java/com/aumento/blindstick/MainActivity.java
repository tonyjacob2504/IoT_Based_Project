package com.aumento.blindstick;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.AspectRatio;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.aumento.blindstick.Utils.GlobalPreference;
import com.aumento.blindstick.Utils.OverlayView;
import com.aumento.blindstick.databinding.ActivityMainBinding;
import com.aumento.blindstick.objectdetection.BoundingBox;
import com.aumento.blindstick.objectdetection.Constants;
import com.aumento.blindstick.objectdetection.Detector;
import com.aumento.blindstick.services.BackgroundLocationsService;
import com.aumento.blindstick.services.ObjectDetector;
import com.google.common.util.concurrent.ListenableFuture;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * MainActivity
 *
 * Core controller of the Blind Stick app.
 * Responsibilities:
 * - Initializes CameraX
 * - Runs real-time object detection
 * - Handles Text-To-Speech output
 * - Manages background location service
 * - Sends SOS & battery status to server
 */
public class MainActivity extends AppCompatActivity
        implements Detector.DetectorListener{

    private TextToSpeech tts;
    private String lastSpokenLabel = null;
    private long lastSpokenTime = 0;

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;

    private static final int SMS_PERMISSION_CODE = 200;

    private BackgroundLocationsService myService;
    private boolean isBound = false;


    private ActivityMainBinding binding;
    private static final boolean isFrontCamera = false;

    private Preview preview;
    private ImageAnalysis imageAnalyzer;
    private Camera camera;
    private ProcessCameraProvider cameraProvider;
    private Detector detector;

    private ExecutorService cameraExecutor;

    private static final String TAG = "Camera";
    private static final int REQUEST_CODE_PERMISSIONS = 10;
    private static final String[] REQUIRED_PERMISSIONS =
            new String[]{Manifest.permission.CAMERA};

    private RequestQueue requestQueue;
    private final Handler apiHandler = new Handler(Looper.getMainLooper());
    private boolean apiCheckRunner = false;
    private boolean alertShowing = false;

    /*private final Runnable apiCheckRunnable = new Runnable() {
        @Override
        public void run() {
            checkStatusApi();
            if(apiCheckRunner){
                apiHandler.postDelayed(this, 1000);  // every 1 second
            }
        }
    };*/
    private String ip, uid;
    private String emg_contact;

    /**
     * onCreate()
     * Initializes UI, permissions, detector, camera, TTS,
     * network queue, and background services.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        requestQueue = Volley.newRequestQueue(this);

        EdgeToEdge.enable(this);
//        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.camera_container), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        GlobalPreference globalPreference = new GlobalPreference(this);
        emg_contact = globalPreference.RetriveEmgContact();
        ip = globalPreference.RetriveIP();
        uid = globalPreference.RetriveUID();

        if (checkAndRequestPermissions()) {
            Toast.makeText(this, "Perm", Toast.LENGTH_SHORT).show();
            startLocationService();
        }

        // Start service
        Intent serviceIntent = new Intent(this, BackgroundLocationsService.class);
//        startService(serviceIntent);

        // Stop service
        stopService(serviceIntent);


        Log.d(TAG, "onCreate: **********************************");

        // Initialize TensorFlow Lite detector and load model
        detector = new Detector(
                getBaseContext(),
                Constants.MODEL_PATH,
                Constants.LABELS_PATH,
                MainActivity.this
        );
        detector.setup();

        if (allPermissionsGranted()) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(
                    this,
                    REQUIRED_PERMISSIONS,
                    REQUEST_CODE_PERMISSIONS
            );
        }

        // Background thread for image analysis (prevents UI blocking)
        cameraExecutor = Executors.newSingleThreadExecutor();

        // Initialize Text-To-Speech engine for voice feedback
        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                tts.setLanguage(Locale.US);
                tts.setSpeechRate(0.9f);
            }
        });

//        apiCheckRunnable.run();

    }

    /**
     * startCamera()
     * Asynchronously obtains ProcessCameraProvider
     * and binds camera use cases when ready.
     */
    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();
                bindCameraUseCases();
            } catch (Exception e) {
                Log.e(TAG, "Camera provider error", e);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    /**
     * bindCameraUseCases()
     * Configures:
     * - Camera selector (back camera)
     * - Preview use case
     * - ImageAnalysis use case
     * - Frame processing pipeline
     */
    private void bindCameraUseCases() {

        // Safety check: CameraProvider must be initialized before binding use cases
        if (cameraProvider == null) {
            throw new IllegalStateException("Camera initialization failed.");
        }

        // Get current device screen rotation to align camera output correctly
        int rotation = binding.viewFinder.getDisplay().getRotation();

        // Select back camera for obstacle detection
        CameraSelector cameraSelector =
                new CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                        .build();

        // Configure Preview use case (what user sees on screen)
        preview =
                new Preview.Builder()
                        .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                        .setTargetRotation(rotation)
                        .build();

        // Configure ImageAnalysis use case (used for frame-by-frame object detection)
        imageAnalyzer =
                new ImageAnalysis.Builder()
                        .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                        // Keep only the latest frame to avoid frame queue buildup and lag
                        .setBackpressureStrategy(
                                ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .setTargetRotation(rotation)
                        // Request RGBA format for easier Bitmap conversion
                        .setOutputImageFormat(
                                ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                        .build();

        // Real-time frame processing pipeline
        imageAnalyzer.setAnalyzer(cameraExecutor, imageProxy -> {
            try {
                // Allocate bitmap buffer matching camera frame dimensions
                Bitmap bitmapBuffer = Bitmap.createBitmap(
                        imageProxy.getWidth(),
                        imageProxy.getHeight(),
                        Bitmap.Config.ARGB_8888
                );

                // Copy pixel data from ImageProxy into Bitmap
                bitmapBuffer.copyPixelsFromBuffer(
                        imageProxy.getPlanes()[0].getBuffer()
                );

                // Matrix used to rotate (and optionally mirror) the frame
                Matrix matrix = new Matrix();
                // Rotate frame based on sensor orientation metadata
                matrix.postRotate(
                        imageProxy.getImageInfo().getRotationDegrees()
                );

                // Mirror image horizontally if using front camera (not used currently)
                if (isFrontCamera) {
                    matrix.postScale(
                            -1f,
                            1f,
                            imageProxy.getWidth(),
                            imageProxy.getHeight()
                    );
                }

                // Create final correctly-oriented bitmap for ML model input
                Bitmap rotatedBitmap = Bitmap.createBitmap(
                        bitmapBuffer,
                        0,
                        0,
                        bitmapBuffer.getWidth(),
                        bitmapBuffer.getHeight(),
                        matrix,
                        true
                );

                // Run object detection on processed frame
                detector.detect(rotatedBitmap);

            } finally {
                // IMPORTANT: Always close frame to prevent memory leaks and camera freeze
                imageProxy.close();
            }
        });

        // Unbind previous use cases before rebinding (prevents conflicts)
        cameraProvider.unbindAll();

        try {
            // Bind Preview + ImageAnalysis to lifecycle
            camera =
                    cameraProvider.bindToLifecycle(
                            this,
                            cameraSelector,
                            preview,
                            imageAnalyzer
                    );

            // Connect Preview output to PreviewView UI component
            preview.setSurfaceProvider(
                    binding.viewFinder.getSurfaceProvider()
            );

        } catch (Exception exc) {
            Log.e(TAG, "Use case binding failed", exc);
        }
    }

    private boolean allPermissionsGranted() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(
                    getBaseContext(),
                    permission
            ) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    private boolean checkAndRequestPermissions() {

        ActivityCompat.requestPermissions(this,
                new String[]{
                        Manifest.permission.CAMERA
                },
                120);

        // Always check FINE + COARSE
        boolean fineLocation = ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;

        boolean coarseLocation = ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;

        // On Android 10+ also check BACKGROUND
        boolean backgroundLocation = true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            backgroundLocation = ContextCompat.checkSelfPermission(this,
                    android.Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED;
        }

        if (fineLocation && coarseLocation && backgroundLocation) {
            return true; // All permissions granted
        } else {
            // Request permissions
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ActivityCompat.requestPermissions(this,
                        new String[]{
                                android.Manifest.permission.ACCESS_FINE_LOCATION,
                                android.Manifest.permission.ACCESS_COARSE_LOCATION,
                                android.Manifest.permission.ACCESS_BACKGROUND_LOCATION
                        },
                        LOCATION_PERMISSION_REQUEST_CODE);
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{
                                android.Manifest.permission.ACCESS_FINE_LOCATION,
                                android.Manifest.permission.ACCESS_COARSE_LOCATION
                        },
                        LOCATION_PERMISSION_REQUEST_CODE);
            }
            return false;
        }
    }

    private void startLocationService() {
        Intent serviceIntent = new Intent(this, BackgroundLocationsService.class);
        startService(serviceIntent);
    }

    private void stopLocationService() {
        Intent serviceIntent = new Intent(this, BackgroundLocationsService.class);
        stopService(serviceIntent);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }

            if (allGranted) {
                startLocationService();
            } else {
                // Permission denied → Show message
            }
        }
    }


    /**
     * send_Sos()
     * Sends emergency SMS with current GPS location.
     * Triggered manually or remotely from server.
     */
    public void send_Sos() {
        if (isBound && myService != null) {
            double lat = myService.getLastLat();
            double lon = myService.getLastLon();
            Toast.makeText(this, "Lat: " + lat + ", Lon: " + lon, Toast.LENGTH_SHORT).show();

            sendSms(emg_contact,"https://www.google.com/maps/dir/?api=1&destination="+lat+","+lon);
        } else {
            Toast.makeText(this, "Service not bound yet", Toast.LENGTH_SHORT).show();
        }
    }

    // Handles SMS permission check and sends emergency message
    private void sendSms(String phoneNumber, String message) {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.SEND_SMS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{ android.Manifest.permission.SEND_SMS},
                    SMS_PERMISSION_CODE);
        } else {
            try {
                SmsManager smsManager = SmsManager.getDefault();
                smsManager.sendTextMessage(phoneNumber, null, message, null, null);
                Toast.makeText(this, "SMS Sent!", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(this, "SMS Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }

    private BroadcastReceiver locationReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            double lat = intent.getDoubleExtra("latitude", 0);
            double lon = intent.getDoubleExtra("longitude", 0);

            // Example: auto-send SOS SMS when location arrives
            sendSms("+911234567890", "SOS! Current location: " + lat + "," + lon);
        }
    };

    // Service binding to access live GPS data from BackgroundLocationsService
    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            BackgroundLocationsService.LocalBinder binder = (BackgroundLocationsService.LocalBinder) service;
            myService = binder.getService();
            isBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            isBound = false;
        }
    };

    // Starts and binds background location service
    @Override
    protected void onStart() {
        super.onStart();
        // Start + bind to service
        Intent intent = new Intent(this, BackgroundLocationsService.class);
        startService(intent); // keeps it alive
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (isBound) {
            unbindService(serviceConnection);
            isBound = false;
        }
    }

    // Registers broadcast receivers (location + battery)
    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter("LOCATION_UPDATE");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(locationReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(locationReceiver, filter);
        }

        registerReceiver(
                batteryReceiver,
                new IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        );

        apiCheckRunner = true;
//        apiHandler.post(apiCheckRunnable);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(locationReceiver);
        unregisterReceiver(batteryReceiver);
    }

    /**
     * onDestroy()
     * Cleans up:
     * - Detector resources
     * - Camera executor
     * - TextToSpeech engine
     * - Network queue
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        detector.clear();
        cameraExecutor.shutdown();
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        if (requestQueue != null) {
            requestQueue.stop();
        }

        apiCheckRunner = false;
//        apiHandler.removeCallbacks(apiCheckRunnable);
    }

    @Override
    public void onEmptyDetect() {
        binding.overlay.invalidate();
    }

    /**
     * onDetect()
     * Callback from Detector.
     * Updates overlay UI and speaks detected object label.
     */
    @Override
    public void onDetect(
            @NonNull List<BoundingBox> boundingBoxes,
            long inferenceTime
    ) {
        // Switch to main thread because UI updates must not run on background thread
        runOnUiThread(() -> {
            // If at least one object detected, process the top result
            if (!boundingBoxes.isEmpty()) {
                // Get class label of highest-confidence detection
                String clsName = boundingBoxes.get(0).getClsName();
                Log.d(TAG, "onDetect: "+clsName);
                // Trigger voice feedback for detected object
                speakLabel(clsName);
            }

            // Display model inference time (performance monitoring)
            binding.inferenceTime.setText(inferenceTime + "ms");
            // Pass detection results to overlay for drawing bounding boxes
            binding.overlay.setResults(boundingBoxes);
            // Force overlay to redraw with new detection results
            binding.overlay.invalidate();
        });
    }

    // Speaks detected object name with repetition control (2s delay rule)
    private void speakLabel(String label) {
        long now = System.currentTimeMillis();

        // Avoid repeating same label continuously
        if (label.equals(lastSpokenLabel) && (now - lastSpokenTime) < 2000) {
            return;
        }

        lastSpokenLabel = label;
        lastSpokenTime = now;

        tts.speak(
                label,
                TextToSpeech.QUEUE_FLUSH,
                null,
                label
        );
    }

    private void checkStatusApi() {

        String url = "http://"+ip+"/blindstick/get_sos_status.php";

        StringRequest request = new StringRequest(
                Request.Method.POST,
                url,
                response -> {

                    Log.d(TAG, "checkStatusApi: "+response);

                    try {
                        JSONObject jsonObject = new JSONObject(response);
                        String status = jsonObject.getString("sos_status");
                        if (status.equals("1"))
                            showAlertOnce();

                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }

                },
                error -> Log.e("BATTERY", "Upload failed", error)
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("uid",uid); // or UID
                return params;
            }
        };

        requestQueue.add(request);
    }

    private void showAlertOnce() {
        if (alertShowing) return; // prevent multiple dialogs

        alertShowing = true;

        send_Sos();

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Alert")
                .setMessage("SOS TRIGGERED")
                .setCancelable(false)
                .setPositiveButton("OK", (dialog, which) -> {
                    alertShowing = false; // allow future alerts
                    dialog.dismiss();
                })
                .show();

    }

    // Monitors battery percentage and uploads status to server
    private BroadcastReceiver batteryReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

            if (level >= 0 && scale > 0) {
                int batteryPercent = (int) ((level * 100f) / scale);
                Log.d("BATTERY", "Battery: " + batteryPercent + "%");

                // Insert into DB / call API
                sendBatteryToServer(batteryPercent);
            }
        }
    };

    // Sends battery percentage to backend via Volley POST request
    private void sendBatteryToServer(int battery) {
        String url = "http://"+ip+"/blindstick/send_battery_precentage.php";

        StringRequest request = new StringRequest(
                Request.Method.POST,
                url,
                response -> Log.d("BATTERY", "Uploaded"),
                error -> Log.e("BATTERY", "Upload failed", error)
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("battery", String.valueOf(battery));
                params.put("uid", uid); // or UID
                return params;
            }
        };

        requestQueue.add(request);
    }

    public void sendNav(View view) {

        double latitude = 10.016894;   // example
        double longitude = 76.675916; // example

        Uri uri = Uri.parse(
                "google.navigation:q=" + latitude + "," + longitude + "&mode=d"
        );

        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        intent.setPackage("com.google.android.apps.maps");

        startActivity(intent);
    }


    public void sendSos(View view) {
        send_Sos();
    }
}
