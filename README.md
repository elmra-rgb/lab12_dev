# LAB 12 – GPS + MySQL + PHP + Google Maps : Localisation et stockage en temps réel 📍🗺️

## ⚠️ Particularité macOS Apple Silicon M2

**Problème rencontré :** Sur macOS avec processeur Apple Silicon M2 (ARM-64 native), le template **"Google Maps Activity"** n'existe pas par défaut dans Android Studio.

**Solution alternative mise en place :**
- Création d'un projet **"Empty Views Activity"** au lieu de Google Maps Activity
- Configuration manuelle des dépendances Google Play Services
- Ajout manuel du fragment `SupportMapFragment` dans le layout
- Adaptation pour MAMP (port 8888) au lieu de XAMPP
- Utilisation de l'adresse `10.0.2.2` pour l'émulateur Android (accès au localhost du Mac)
- Protection de la vie privée : utilisation de l'émulateur avec coordonnées fictives

---

## Aperçu de l'application

Une application Android qui récupère la position GPS, l'envoie à un serveur PHP/MySQL via Volley, stocke les données en base, et affiche toutes les positions sur une Google Map.

| Demande permission GPS | Affichage de la localisation | Stockage dans phpMyAdmin |
|------------------------|------------------------------|--------------------------|
| <img src="screens/pic1.png" width="200"> | <img src="screens/pic2.png" width="200"> | <img src="screens/pic3.png" width="200"> |

## Fonctionnalités

- **GPS Tracking** : récupération de la position (latitude, longitude) via GPS_PROVIDER
- **Envoi au serveur** : transmission des données via Volley (POST)
- **Stockage MySQL** : sauvegarde des positions dans phpMyAdmin
- **Google Maps** : affichage de tous les markers des positions enregistrées
- **Permission runtime** : demande d'accès à la localisation au lancement

## Architecture du projet

```
📁 MacOS (MAMP) : /Applications/MAMP/htdocs/serveur_web/
├── 📁 classe/PointGeo.php              # Modèle Position
├── 📁 connexion/DatabaseConnector.php  # Connexion MySQL (PDO)
├── 📁 dao/StorageInterface.php         # Interface CRUD
├── 📁 service/PointGeoServices.php     # Service (INSERT/SELECT)
├── 📄 insertPointGeo.php               # API insertion (POST)
├── 📄 fetchPointsGeo.php               # API récupération (POST)
└── 📄 test.php                         # Test de connexion

📁 Android Studio : lab12_dev/
└── 📁 app/src/main/
    ├── 📁 java/com/example/lab12_dev/
    │   ├── 📄 LocalisationActivity.java    # GPS + Volley + UI
    │   └── 📄 MapDisplayActivity.java      # Google Maps + markers
    ├── 📁 res/
    │   ├── 📁 layout/
    │   │   ├── 📄 activity_localisation.xml
    │   │   └── 📄 activity_map_display.xml
    │   └── 📁 values/
    │       ├── 📄 strings.xml
    │       └── 📄 google_maps_api.xml
    └── 📄 AndroidManifest.xml
```

## Code source complet

### PARTIE 1 : BASE DE DONNÉES MySQL (phpMyAdmin)

**SQL à exécuter :**

```sql
CREATE DATABASE `localisation`;
USE `localisation`;

CREATE TABLE `point_geo` (
    `record_id` int(11) NOT NULL PRIMARY KEY AUTO_INCREMENT,
    `latitude_value` double NOT NULL,
    `longitude_value` double NOT NULL,
    `record_date` datetime NOT NULL,
    `device_identifier` varchar(50) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

---

### PARTIE 2 : BACKEND PHP (MAMP)

#### 2.1 `classe/PointGeo.php`

```php
<?php
class PointGeo {
    private $internalId;
    private $latitudeVal;
    private $longitudeVal;
    private $timestampDate;
    private $deviceId;

    public function __construct($internalId, $latitudeVal, $longitudeVal, $timestampDate, $deviceId) {
        $this->internalId = $internalId;
        $this->latitudeVal = $latitudeVal;
        $this->longitudeVal = $longitudeVal;
        $this->timestampDate = $timestampDate;
        $this->deviceId = $deviceId;
    }

    public function fetchId() { return $this->internalId; }
    public function fetchLatitude() { return $this->latitudeVal; }
    public function fetchLongitude() { return $this->longitudeVal; }
    public function fetchDate() { return $this->timestampDate; }
    public function fetchDeviceId() { return $this->deviceId; }
}
?>
```

#### 2.2 `connexion/DatabaseConnector.php`

```php
<?php
class DatabaseConnector {
    private $dbConnection;

    public function __construct() {
        $databaseHost = 'localhost';
        $databaseName = 'localisation';
        $databaseUser = 'root';
        $databasePassword = 'root';  // MAMP utilise 'root' sans mot de passe ou 'root'

        try {
            $dsnString = "mysql:host=$databaseHost;dbname=$databaseName;charset=utf8mb4";
            $this->dbConnection = new PDO($dsnString, $databaseUser, $databasePassword, [
                PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION,
                PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC
            ]);
        } catch (Exception $error) {
            die('Connexion échouée : ' . $error->getMessage());
        }
    }

    public function getConnection() {
        return $this->dbConnection;
    }
}
?>
```

#### 2.3 `dao/StorageInterface.php`

```php
<?php
interface StorageInterface {
    public function insertRecord($dataObject);
    public function retrieveAllRecords();
    public function updateRecord($dataObject);
    public function deleteRecord($dataObject);
    public function findById($dataObject);
}
?>
```

#### 2.4 `service/PointGeoServices.php`

```php
<?php
include_once __DIR__ . '/../dao/StorageInterface.php';
include_once __DIR__ . '/../classe/PointGeo.php';
include_once __DIR__ . '/../connexion/DatabaseConnector.php';

class PointGeoServices implements StorageInterface {
    private $dbConnector;

    public function __construct() {
        $this->dbConnector = new DatabaseConnector();
    }

    public function insertRecord($geoPoint) {
        $sqlQuery = "INSERT INTO point_geo(latitude_value, longitude_value, record_date, device_identifier) 
                     VALUES (?, ?, ?, ?)";
        
        $preparedStmt = $this->dbConnector->getConnection()->prepare($sqlQuery);
        
        $preparedStmt->execute([
            $geoPoint->fetchLatitude(),
            $geoPoint->fetchLongitude(),
            $geoPoint->fetchDate(),
            $geoPoint->fetchDeviceId()
        ]);
        
        return true;
    }

    public function retrieveAllRecords() {
        $sqlQuery = "SELECT * FROM point_geo ORDER BY record_date DESC";
        $preparedStmt = $this->dbConnector->getConnection()->prepare($sqlQuery);
        $preparedStmt->execute();
        return $preparedStmt->fetchAll(PDO::FETCH_ASSOC);
    }

    // Méthodes non utilisées
    public function updateRecord($dataObject) {}
    public function deleteRecord($dataObject) {}
    public function findById($dataObject) {}
}
?>
```

#### 2.5 `insertPointGeo.php` (API insertion)

```php
<?php
header('Content-Type: application/json; charset=utf-8');

if ($_SERVER["REQUEST_METHOD"] != "POST") {
    http_response_code(405);
    echo json_encode(["success" => false, "message" => "Méthode POST requise"]);
    exit;
}

include_once __DIR__ . '/service/PointGeoServices.php';
include_once __DIR__ . '/classe/PointGeo.php';

$latitudeData = $_POST['latitude'] ?? null;
$longitudeData = $_POST['longitude'] ?? null;
$dateTimeData = $_POST['date'] ?? null;
$deviceIdData = $_POST['imei'] ?? null;

if ($latitudeData === null || $longitudeData === null || $dateTimeData === null || $deviceIdData === null) {
    http_response_code(400);
    echo json_encode(["success" => false, "error" => "Paramètres manquants"]);
    exit;
}

try {
    $geoService = new PointGeoServices();
    $newPoint = new PointGeo(null, $latitudeData, $longitudeData, $dateTimeData, $deviceIdData);
    $geoService->insertRecord($newPoint);
    
    echo json_encode(["success" => true, "message" => "Position enregistrée avec succès"]);
} catch (Exception $error) {
    http_response_code(500);
    echo json_encode(["success" => false, "error" => $error->getMessage()]);
}
?>
```

#### 2.6 `fetchPointsGeo.php` (API récupération)

```php
<?php
header('Content-Type: application/json; charset=utf-8');

if ($_SERVER["REQUEST_METHOD"] != "POST") {
    http_response_code(405);
    echo json_encode(["success" => false, "message" => "Méthode POST requise"]);
    exit;
}

include_once __DIR__ . '/service/PointGeoServices.php';

function getAllGeoPoints() {
    $geoService = new PointGeoServices();
    $allPoints = $geoService->retrieveAllRecords();
    
    echo json_encode([
        "success" => true,
        "points" => $allPoints,
        "total_count" => count($allPoints)
    ]);
}

getAllGeoPoints();
?>
```

---

### PARTIE 3 : APPLICATION ANDROID

#### 3.1 `app/build.gradle`

```gradle
plugins {
    id 'com.android.application'
}

android {
    namespace 'com.example.lab12_dev'
    compileSdk 34

    defaultConfig {
        applicationId "com.example.lab12_dev"
        minSdk 23
        targetSdk 34
        versionCode 1
        versionName "1.0"
    }

    compileOptions {
        sourceCompatibility JavaVersion.VERSION_1_8
        targetCompatibility JavaVersion.VERSION_1_8
    }
}

dependencies {
    implementation 'androidx.appcompat:appcompat:1.6.1'
    implementation 'com.google.android.material:material:1.11.0'
    implementation 'com.android.volley:volley:1.2.1'
    implementation 'com.google.android.gms:play-services-maps:18.2.0'
    implementation 'com.google.android.gms:play-services-location:21.0.1'
    implementation 'androidx.constraintlayout:constraintlayout:2.1.4'
}
```

#### 3.2 `AndroidManifest.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
    <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
    <uses-permission android:name="android.permission.READ_PHONE_STATE" />

    <application
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:theme="@style/Theme.AppCompat.Light.DarkActionBar"
        android:usesCleartextTraffic="true">

        <meta-data
            android:name="com.google.android.geo.API_KEY"
            android:value="@string/google_maps_key" />

        <activity
            android:name=".LocalisationActivity"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

        <activity
            android:name=".MapDisplayActivity"
            android:exported="false" />

    </application>

</manifest>
```

#### 3.3 `res/values/strings.xml`

```xml
<resources>
    <string name="app_name">lab12_dev</string>
    
    <string name="gps_provider_enabled">Fournisseur %s activé</string>
    <string name="gps_provider_disabled">Fournisseur %s désactivé</string>
    <string name="new_position_detected">Lat: %.4f, Lng: %.4f</string>
    
    <string name="latitude_label">Latitude : </string>
    <string name="longitude_label">Longitude : </string>
    <string name="show_map_button">Afficher la carte</string>
    
    <string name="permission_denied">Permission de localisation refusée</string>
    <string name="permission_granted">Permission accordée</string>
</resources>
```

#### 3.4 `res/values/google_maps_api.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="google_maps_key" translatable="false">AIzaSyD0BZHm5lxxKcZxWF_7sBzQBm7Z5GzB8NU</string>
</resources>
```

#### 3.5 `res/layout/activity_localisation.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    android:padding="20dp"
    android:gravity="center">

    <TextView
        android:id="@+id/display_latitude"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="Latitude : --"
        android:textSize="20sp"
        android:textStyle="bold"
        android:padding="10dp" />

    <TextView
        android:id="@+id/display_longitude"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="Longitude : --"
        android:textSize="20sp"
        android:textStyle="bold"
        android:padding="10dp" />

    <Button
        android:id="@+id/action_view_map"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="@string/show_map_button"
        android:textSize="16sp"
        android:padding="15dp"
        android:layout_marginTop="30dp" />

</LinearLayout>
```

#### 3.6 `res/layout/activity_map_display.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<FrameLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    tools:context=".MapDisplayActivity">

    <fragment
        android:id="@+id/map_fragment"
        android:name="com.google.android.gms.maps.SupportMapFragment"
        android:layout_width="match_parent"
        android:layout_height="match_parent" />

</FrameLayout>
```

#### 3.7 `LocalisationActivity.java`

```java
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
    private Button mapButton;
    private RequestQueue networkQueue;
    private LocationManager geoManager;

    private final String SERVER_URL = "http://10.0.2.2:8888/serveur_web/insertPointGeo.php";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_localisation);

        latitudeView = findViewById(R.id.display_latitude);
        longitudeView = findViewById(R.id.display_longitude);
        mapButton = findViewById(R.id.action_view_map);

        networkQueue = Volley.newRequestQueue(this);
        geoManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        mapButton.setOnClickListener(v -> {
            startActivity(new Intent(LocalisationActivity.this, MapDisplayActivity.class));
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
        geoManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                UPDATE_TIME_MS,
                UPDATE_DISTANCE_M,
                new GeoLocationListener()
        );
        Toast.makeText(this, "Recherche de position en cours...", Toast.LENGTH_LONG).show();
    }

    private void sendToServer(double lat, double lon) {
        StringRequest request = new StringRequest(
                Request.Method.POST,
                SERVER_URL,
                response -> { },
                error -> Toast.makeText(this, "Erreur envoi", Toast.LENGTH_SHORT).show()
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                params.put("latitude", String.valueOf(lat));
                params.put("longitude", String.valueOf(lon));
                params.put("date", sdf.format(new Date()));
                params.put("imei", Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID));
                return params;
            }
        };
        networkQueue.add(request);
    }

    private class GeoLocationListener implements LocationListener {
        @Override
        public void onLocationChanged(@NonNull Location location) {
            double lat = location.getLatitude();
            double lon = location.getLongitude();

            latitudeView.setText("Latitude : " + lat);
            longitudeView.setText("Longitude : " + lon);

            String msg = String.format(getString(R.string.new_position_detected), lat, lon);
            Toast.makeText(LocalisationActivity.this, msg, Toast.LENGTH_LONG).show();

            sendToServer(lat, lon);
        }

        @Override
        public void onProviderDisabled(@NonNull String provider) {
            String msg = String.format(getString(R.string.gps_provider_disabled), provider);
            Toast.makeText(LocalisationActivity.this, msg, Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onProviderEnabled(@NonNull String provider) {
            String msg = String.format(getString(R.string.gps_provider_enabled), provider);
            Toast.makeText(LocalisationActivity.this, msg, Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {}
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_CODE && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startGPSMonitoring();
            Toast.makeText(this, R.string.permission_granted, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, R.string.permission_denied, Toast.LENGTH_LONG).show();
        }
    }
}
```

#### 3.8 `MapDisplayActivity.java`

```java
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

    private GoogleMap worldMap;
    private RequestQueue networkQueue;
    private final String FETCH_URL = "http://10.0.2.2:8888/serveur_web/fetchPointsGeo.php";

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
    public void onMapReady(GoogleMap googleMap) {
        worldMap = googleMap;
        loadAndDisplayMarkers();
    }

    private void loadAndDisplayMarkers() {
        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                FETCH_URL,
                null,
                response -> {
                    try {
                        JSONArray points = response.getJSONArray("points");
                        for (int i = 0; i < points.length(); i++) {
                            JSONObject p = points.getJSONObject(i);
                            double lat = p.getDouble("latitude_value");
                            double lon = p.getDouble("longitude_value");
                            worldMap.addMarker(new MarkerOptions()
                                    .position(new LatLng(lat, lon))
                                    .title("Position " + (i + 1)));
                        }
                        Toast.makeText(this, points.length() + " position(s) chargée(s)", Toast.LENGTH_SHORT).show();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                },
                error -> Toast.makeText(this, "Erreur chargement", Toast.LENGTH_SHORT).show()
        );
        networkQueue.add(request);
    }
}
```

---

## Configuration pour MAMP sur macOS

### Démarrer MAMP
```bash
# Lancer MAMP et démarrer Apache + MySQL (port 8888)
open /Applications/MAMP/MAMP.app
```

### Créer les dossiers du backend
```bash
mkdir -p /Applications/MAMP/htdocs/serveur_web/classe
mkdir -p /Applications/MAMP/htdocs/serveur_web/connexion
mkdir -p /Applications/MAMP/htdocs/serveur_web/dao
mkdir -p /Applications/MAMP/htdocs/serveur_web/service
```

### Copier tous les fichiers PHP dans `/Applications/MAMP/htdocs/serveur_web/`

### Vérifier que tout fonctionne
```bash
curl -X POST http://localhost:8888/serveur_web/test.php
curl -X POST http://10.0.2.2:8888/serveur_web/fetchPointsGeo.php
```

---

## Comment exécuter l'application

1. **Démarrer MAMP** (Apache + MySQL sur port 8888)
2. **Créer la base de données** via phpMyAdmin (`http://localhost:8888/phpMyAdmin5/`)
3. **Copier les fichiers PHP** dans `/Applications/MAMP/htdocs/serveur_web/`
4. **Créer le projet Android** "Empty Views Activity" (car Google Maps Activity inexistant sur macOS M2)
5. **Remplacer tous les fichiers** par les codes ci-dessus
6. **Lancer l'émulateur** (ou connecter un vrai téléphone)
7. **Exécuter l'application**

---

## Fonctionnement

| Action | Résultat |
|--------|----------|
| Lancement de l'application | Demande la permission de localisation (pic1) |
| Permission accordée | Toast "Recherche de position en cours..." |
| Position GPS détectée | Toast avec latitude/longitude (pic2) |
| Après 60m ou 150m | Envoi automatique au serveur |
| Vérification dans phpMyAdmin | Données stockées dans `point_geo` (pic3) |
| Clic sur "Afficher la carte" | Google Map avec tous les markers |

---

## Points techniques abordés

### Backend (PHP/MySQL)
- **PDO** : connexion sécurisée à MySQL
- **Requêtes préparées** : protection contre les injections SQL
- **API REST** : création d'endpoints POST pour insert/fetch
- **JSON** : échange de données structurées

### Android
- **LocationManager** : service GPS pour récupérer la position
- **LocationListener** : écoute des changements de position
- **Volley** : librairie pour requêtes HTTP POST
- **Google Maps SDK** : affichage de la carte et des markers
- **Permissions runtime** : demande explicite (Android 6+)
- **SupportMapFragment** : fragment pour intégrer Google Maps

---

## Dépannage

| Problème | Solution |
|----------|----------|
| Carte blanche | Vérifier la clé API dans `google_maps_api.xml` |
| Aucune position | Activer le GPS dans l'émulateur (Location → Send) |
| Erreur Volley | Vérifier que MAMP est démarré sur le port 8888 |
| phpMyAdmin vide | Vérifier la table `point_geo` et les insertions |
| Connexion refusée | Vérifier l'adresse `10.0.2.2:8888` |

---

**Auteur** : ELHEZZAM RANIA  
**Plateforme** : macOS Apple Silicon M2 (ARM-64 native)  
**Environnement** : MAMP (port 8888) + Android Studio + Émulateur  
**Particularité** : Template Google Maps Activity inexistant sur macOS M2 → création manuelle complète  
**Clé API utilisée** : AIzaSyD0BZHm5lxxKcZxWF_7sBzQBm7Z5GzB8NU
