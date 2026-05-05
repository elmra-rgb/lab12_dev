<?php
class DatabaseConnector {
    private $dbConnection;

    public function __construct() {
        // Configuration MAMP (port 8888 non nécessaire pour PHP)
        $databaseHost = 'localhost';
        $databaseName = 'localisation';
        $databaseUser = 'root';
        $databasePassword = 'root';  // MAMP par défaut : root sans mot de passe ou 'root'

        try {
            $dsnString = "mysql:host=$databaseHost;port=8889;dbname=$databaseName;charset=utf8mb4";
            $this->dbConnection = new PDO($dsnString, $databaseUser, $databasePassword, [
                PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION,
                PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC,
                PDO::ATTR_EMULATE_PREPARES => false
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