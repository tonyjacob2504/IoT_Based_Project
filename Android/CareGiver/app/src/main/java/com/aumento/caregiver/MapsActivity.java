package com.aumento.caregiver;

import androidx.fragment.app.FragmentActivity;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.aumento.caregiver.Utils.GlobalPreference;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.aumento.caregiver.databinding.ActivityMapsBinding;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private ActivityMapsBinding binding;

    private Handler handler;
    private Runnable updateRunnable;

    private RequestQueue requestQueue;
    private Marker currentMarker;

    private static final String TAG = "MapsActivity";
    private static String API_URL; // Replace with your endpoint
    private Polyline historyPolyline;
    private String ip, uid;
    private TextView batteryTV;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        GlobalPreference mGlobalPreference= new GlobalPreference(getApplicationContext());
        ip = mGlobalPreference.RetriveIP();
        uid = mGlobalPreference.RetriveUID();
        API_URL = "http://"+ip+"/blindstick/get_user_location.php";

        batteryTV = findViewById(R.id.batteryTextView);

        // Initialize Volley
        requestQueue = Volley.newRequestQueue(this);

        binding = ActivityMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        // Handler for repeating task
        handler = new Handler();
        updateRunnable = new Runnable() {
            @Override
            public void run() {
                fetchLocationFromServer();
                handler.postDelayed(this, 5000); // every 5 sec
            }
        };

        fetchPathHistory(uid);
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Add a marker in Sydney and move the camera
        LatLng sydney = new LatLng(-34, 151);
        mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));

        // Start fetching when map is ready
        handler.post(updateRunnable);
    }

    private void fetchLocationFromServer() {

        StringRequest stringRequest = new StringRequest(
                Request.Method.GET,
                API_URL+ "?uid=" + uid,
                response -> {
                    try {
                        Log.d(TAG, "Server Response: " + response);

                        JSONObject object = new JSONObject(response);
                        JSONObject jsonObject = object.getJSONObject("data");
                        String lat = jsonObject.getString("lat");
                        String lon = jsonObject.getString("lon");
                        double latt = Double.parseDouble(lat);
                        double lonn = Double.parseDouble(lon);
                        updateMarker(latt, lonn);


                        batteryTV = findViewById(R.id.batteryTextView);

                        String battery = object.getString("battery");
                        batteryTV.setText(battery);
                        Log.d(TAG, "fetchLocationFromServer: "+battery);


                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                },
                error -> Log.e(TAG, "Volley Error: " + error.getMessage())
        );

        requestQueue.add(stringRequest);
    }

    private void updateMarker(double lat, double lon) {
        LatLng location = new LatLng(lat, lon);

        runOnUiThread(() -> {
            if (currentMarker != null) {
                currentMarker.remove();
            }

            currentMarker = mMap.addMarker(new MarkerOptions().position(location).title("Live Location"));

            // Move camera smoothly
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(location, 15));
        });
    }

    private void fetchPathHistory(String userId) {

        String url = "http://"+ip+"/blindstick/path_history.php"
                + "?uid=" + userId
                + "&minutes=10";

        StringRequest request = new StringRequest(
                Request.Method.GET,
                url,
                response -> {

                    Log.d("PATH_HISTORY", "Parse error"+response);

                    try {
                        JSONObject json = new JSONObject(response);
                        if (!json.getBoolean("success")) return;

                        JSONArray points = json.getJSONArray("points");
                        List<LatLng> path = new ArrayList<>();

                        for (int i = 0; i < points.length(); i++) {
                            JSONObject p = points.getJSONObject(i);
                            path.add(new LatLng(
                                    p.getDouble("lat"),
                                    p.getDouble("lng")
                            ));
                        }

                        drawHistoryPolyline(path);

                    } catch (Exception e) {
                        Log.e("PATH_HISTORY", "Parse error", e);
                    }
                },
                error -> Log.e("PATH_HISTORY", "API error", error)
        );

        requestQueue.add(request);
    }

    private void drawHistoryPolyline(List<LatLng> points) {
        if (points.isEmpty()) return;

        if (historyPolyline != null) {
            historyPolyline.remove();
        }

        historyPolyline = mMap.addPolyline(
                new PolylineOptions()
                        .addAll(points)
                        .color(Color.BLUE)
                        .width(8f)
        );

        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(points.get(0), 16f));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(updateRunnable);
    }
}