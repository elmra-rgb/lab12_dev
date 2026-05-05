<?php
interface StorageInterface {
    public function insertRecord($dataObject);
    public function retrieveAllRecords();
    public function updateRecord($dataObject);
    public function deleteRecord($dataObject);
    public function findById($dataObject);
}
?>