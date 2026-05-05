<?php
include_once __DIR__ . '/../dao/StorageInterface.php';
include_once __DIR__ . '/../class/PointGeo.php';
include_once __DIR__ . '/../connection/DatabaseConnector.php';

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

    // Méthodes non utilisées mais imposées par l'interface
    public function updateRecord($dataObject) {}
    public function deleteRecord($dataObject) {}
    public function findById($dataObject) {}
}
?>