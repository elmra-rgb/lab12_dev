package com.example.lab12_dev;

import android.os.Bundle;
import android.widget.Toast;

import androidx.fragment.app.FragmentActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class MapDisplayActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap globalMapRef;
    private RequestQueue networkQueue;
    private static final String SERVER_FETCH_URL = "http://10.0.2.2:8888/localisation/fetchPointsGeo.php";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map_display);

        networkQueue = Volley.newRequestQueue(this);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map_fragment);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMapInstance) {
        globalMapRef = googleMapInstance;
        retrieveAndDisplayPositions();
    }

    private void retrieveAndDisplayPositions() {
        JsonObjectRequest jsonRequest = new JsonObjectRequest(
                Request.Method.POST,
                SERVER_FETCH_URL,
                null,
                response -> {
                    try {
                        boolean operationSuccess = response.getBoolean("success");
                        if (operationSuccess) {
                            JSONArray pointsArray = response.getJSONArray("points");

                            if (pointsArray.length() == 0) {
                                Toast.makeText(MapDisplayActivity.this,
                                        "Aucune position enregistrée", Toast.LENGTH_LONG).show();
                                return;
                            }

                            for (int index = 0; index < pointsArray.length(); index++) {
                                JSONObject singlePoint = pointsArray.getJSONObject(index);

                                double latValue = singlePoint.getDouble("latitude_value");
                                double lonValue = singlePoint.getDouble("longitude_value");
                                String deviceInfo = singlePoint.getString("device_identifier");
                                String dateInfo = singlePoint.getString("record_date");

                                LatLng pointPosition = new LatLng(latValue, lonValue);

                                globalMapRef.addMarker(new MarkerOptions()
                                        .position(pointPosition)
                                        .title("Appareil: " + deviceInfo)
                                        .snippet("Date: " + dateInfo));
                            }

                            // Centrer la carte sur le premier point
                            if (pointsArray.length() > 0) {
                                JSONObject firstPoint = pointsArray.getJSONObject(0);
                                double firstLat = firstPoint.getDouble("latitude_value");
                                double firstLon = firstPoint.getDouble("longitude_value");
                                LatLng centerPos = new LatLng(firstLat, firstLon);
                                globalMapRef.moveCamera(
                                        com.google.android.gms.maps.CameraUpdateFactory
                                                .newLatLngZoom(centerPos, 12f)
                                );
                            }

                            Toast.makeText(MapDisplayActivity.this,
                                    pointsArray.length() + " position(s) affichée(s)",
                                    Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(MapDisplayActivity.this,
                                    "Erreur de chargement des données",
                                    Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException parsingError) {
                        parsingError.printStackTrace();
                        Toast.makeText(MapDisplayActivity.this,
                                "Erreur de format JSON",
                                Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    Toast.makeText(MapDisplayActivity.this,
                            "Erreur réseau: " + error.getMessage(),
                            Toast.LENGTH_LONG).show();
                }
        );

        networkQueue.add(jsonRequest);
    }
}