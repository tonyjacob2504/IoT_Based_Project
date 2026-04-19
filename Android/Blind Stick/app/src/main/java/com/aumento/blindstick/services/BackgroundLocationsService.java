package com.aumento.blindstick.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.aumento.blindstick.Utils.GlobalPreference;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import java.util.HashMap;
import java.util.Map;


public class BackgroundLocationsService extends Service {

    private static final String TAG = "BackgroundLocationsSvc";
    private static final String CHANNEL_ID = "LocationChannel";

    private FusedLocationProviderClient fusedLocationClient;
    private Handler handler;
    private Runnable locationRunnable;
    private String ip, uid;
    private GlobalPreference globalPreference;

    private final IBinder binder = new LocalBinder();

    // Store last location
    private double lastLat = 0.0;
    private double lastLon = 0.0;

    public class LocalBinder extends Binder {
        public BackgroundLocationsService getService() {
            return BackgroundLocationsService.this;
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        globalPreference = new GlobalPreference(getApplicationContext());
        ip = globalPreference.RetriveIP();
        uid = globalPreference.RetriveUID();

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        handler = new Handler();

        locationRunnable = new Runnable() {
            @Override
            public void run() {
                getLocationAndSend();
                handler.postDelayed(this, 5000); // repeat every 5 sec
            }
        };
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForegroundServiceWithNotification();

        handler.post(locationRunnable); // start repeating task

        return START_STICKY;
    }

    private void startForegroundServiceWithNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Location Background Service",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Location Service Running")
                .setContentText("Submitting location every 5 seconds...")
                .setSmallIcon(android.R.drawable.ic_menu_mylocation)
                .build();

        startForeground(1, notification);
    }

    private void getLocationAndSend() {
        try {
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(location -> {
                        if (location != null) {
                            lastLat = location.getLatitude();
                            lastLon = location.getLongitude();

                            // Send broadcast to activity
                            Intent intent = new Intent("LOCATION_UPDATE");
                            intent.putExtra("latitude", lastLat);
                            intent.putExtra("longitude", lastLon);
                            sendBroadcast(intent);

                            Log.d(TAG, "Lat: " + lastLat + ", Lon: " + lastLon);

                            // TODO: send this to your database (API call, Firebase, etc.)
                            sendLocationToDatabase(lastLat, lastLon);
                        }
                    });
        } catch (SecurityException e) {
            Log.e(TAG, "Location permission missing", e);
        }
    }

    public double getLastLat() {
        return lastLat;
    }

    public double getLastLon() {
        return lastLon;
    }

    private void sendLocationToDatabase(double lat, double lon) {
        // Example: Call your API here
        // You can use Retrofit, Volley, or HttpURLConnection
        Log.d(TAG, "Sending to DB -> Lat: " + lat + ", Lon: " + lon);

        ip = globalPreference.RetriveIP();
        uid = globalPreference.RetriveUID();

        String url = "http://"+ip+"/blindstick/update_location.php"; // Replace with your API endpoint

        StringRequest stringRequest = new StringRequest(Request.Method.POST, url,
                response -> {
                    Log.d(TAG, "Server Response: " + response);
                },
                error -> {
                    Log.e(TAG, "Error sending location: " + error.getMessage());
                }
        )
        {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("latitude", String.valueOf(lat));
                params.put("longitude", String.valueOf(lon));
                params.put("uid", String.valueOf(uid));
                return params;
            }
        };

        // Add request to Volley queue
        Volley.newRequestQueue(getApplicationContext()).add(stringRequest);

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(locationRunnable);
    }

}