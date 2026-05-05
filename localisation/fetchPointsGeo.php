<?php
header('Content-Type: application/json; charset=utf-8');

include_once __DIR__ . '/localisation/service/PointGeoServices.php';

try {
    $geoService = new PointGeoServices();
    $records = $geoService->retrieveAllRecords();
    
    echo json_encode([
        "success" => true,
        "points" => $records
    ]);
} catch (Exception $error) {
    http_response_code(500);
    echo json_encode([
        "success" => false,
        "error" => $error->getMessage()
    ]);
}
?>