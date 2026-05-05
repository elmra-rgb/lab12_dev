<?php
header('Content-Type: application/json; charset=utf-8');

// Vérifier la méthode HTTP
if ($_SERVER["REQUEST_METHOD"] != "POST") {
    http_response_code(405);
    echo json_encode(["success" => false, "message" => "Méthode POST requise"]);
    exit;
}

// Inclusion des classes
include_once __DIR__ . '/localisation/service/PointGeoServices.php';
include_once __DIR__ . '/localisation/class/PointGeo.php';

// Récupération des données POST
$latitudeData = $_POST['latitude'] ?? null;
$longitudeData = $_POST['longitude'] ?? null;
$dateTimeData = $_POST['date'] ?? null;
$deviceIdData = $_POST['imei'] ?? null;

// IP du client
$clientIpAddress = $_SERVER['REMOTE_ADDR'];

// Validation
if ($latitudeData === null || $longitudeData === null || $dateTimeData === null || $deviceIdData === null) {
    http_response_code(400);
    echo json_encode([
        "success" => false, 
        "error" => "Paramètres manquants",
        "received" => [
            "latitude" => $latitudeData,
            "longitude" => $longitudeData,
            "date" => $dateTimeData,
            "imei" => $deviceIdData
        ],
        "client_ip" => $clientIpAddress
    ]);
    exit;
}

try {
    $geoService = new PointGeoServices();
    $newPoint = new PointGeo(null, $latitudeData, $longitudeData, $dateTimeData, $deviceIdData);
    $geoService->insertRecord($newPoint);
    
    echo json_encode([
        "success" => true,
        "client_ip" => $clientIpAddress,
        "message" => "Position enregistrée avec succès"
    ]);
} catch (Exception $error) {
    http_response_code(500);
    echo json_encode([
        "success" => false,
        "error" => $error->getMessage(),
        "client_ip" => $clientIpAddress
    ]);
}
?>