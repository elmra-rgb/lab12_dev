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

    // Getters
    public function fetchId() { return $this->internalId; }
    public function fetchLatitude() { return $this->latitudeVal; }
    public function fetchLongitude() { return $this->longitudeVal; }
    public function fetchDate() { return $this->timestampDate; }
    public function fetchDeviceId() { return $this->deviceId; }

    // Setters
    public function assignId($internalId) { $this->internalId = $internalId; }
    public function assignLatitude($latitudeVal) { $this->latitudeVal = $latitudeVal; }
    public function assignLongitude($longitudeVal) { $this->longitudeVal = $longitudeVal; }
    public function assignDate($timestampDate) { $this->timestampDate = $timestampDate; }
    public function assignDeviceId($deviceId) { $this->deviceId = $deviceId; }
}
?>