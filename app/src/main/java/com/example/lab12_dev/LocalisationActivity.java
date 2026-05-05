package com.example.lab12_dev;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class LocalisationActivity extends AppCompatActivity {

    private static final int LOCATION_PERMISSION_CODE = 100;
    private static final long UPDATE_TIME_MS = 60000;
    private static final float UPDATE_DISTANCE_M = 150;

    private TextView latitudeView;
    private TextView longitudeView;
    private RequestQueue networkQueue;
    private LocationManager geoManager;

    private final String SERVER_INSERT_URL = "http://10.0.2.2:8888/localisation/insertPointGeo.php";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_localisation);

        latitudeView = findViewById(R.id.display_latitude);
        longitudeView = findViewById(R.id.display_longitude);
        Button mapButton = findViewById(R.id.action_view_map);

        networkQueue = Volley.newRequestQueue(this);
        geoManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        mapButton.setOnClickListener(v -> {
            Intent mapIntent = new Intent(LocalisationActivity.this, MapDisplayActivity.class);
            startActivity(mapIntent);
        });

        checkAndRequestLocationPermission();
    }

    private void checkAndRequestLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_CODE);
        } else {
            startGPSMonitoring();
        }
    }

    @SuppressLint("MissingPermission")
    private void startGPSMonitoring() {
        if (geoManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            geoManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    UPDATE_TIME_MS,
                    UPDATE_DISTANCE_M,
                    locationListener
            );
            Toast.makeText(this, "Recherche de position GPS...", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Veuillez activer le GPS", Toast.LENGTH_LONG).show();
        }
    }

    private final LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(@NonNull Location location) {
            double lat = location.getLatitude();
            double lon = location.getLongitude();

            latitudeView.setText(getString(R.string.latitude_label) + " " + lat);
            longitudeView.setText(getString(R.string.longitude_label) + " " + lon);

            sendPositionToServer(lat, lon);
        }

        @Override
        public void onProviderEnabled(@NonNull String provider) {}

        @Override
        public void onProviderDisabled(@NonNull String provider) {}

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {}
    };

    private void sendPositionToServer(double latitudeVal, double longitudeVal) {
        StringRequest postRequest = new StringRequest(
                Request.Method.POST,
                SERVER_INSERT_URL,
                response -> {
                    // Succès silencieux
                },
                (VolleyError error) -> {
                    String errorMsg = error.getMessage() != null ? error.getMessage() : "Erreur réseau";
                    Toast.makeText(LocalisationActivity.this, "Erreur d'envoi: " + errorMsg, Toast.LENGTH_SHORT).show();
                }
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> parameters = new HashMap<>();
                SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

                parameters.put("latitude", String.valueOf(latitudeVal));
                parameters.put("longitude", String.valueOf(longitudeVal));
                parameters.put("date", dateFormatter.format(new Date()));
                parameters.put("imei", obtainDeviceIdentifier());

                return parameters;
            }
        };

        networkQueue.add(postRequest);
    }

    private String obtainDeviceIdentifier() {
        @SuppressLint("HardwareIds")
        String androidId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        return (androidId != null) ? androidId : "ID_INCONNU";
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_CODE && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startGPSMonitoring();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (geoManager != null) {
            geoManager.removeUpdates(locationListener);
        }
    }
}
