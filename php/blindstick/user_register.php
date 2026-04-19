<?php

include 'connection.php'; // Include your DB connection

$name = $_POST['name'] ?? '';
$email = $_POST['email'] ?? '';
$phone = $_POST['phone'] ?? '';
$emgPhone = $_POST['emgPhone'] ?? '';
$password = $_POST['password'] ?? '';


$sql = "INSERT INTO users (name, email, phone, care_taker, password) VALUES 
        ('$name', '$email', '$phone', '$emgPhone', '$password')";

if(mysqli_query($con, $sql))
{
    echo json_encode([
        "status" => "success",
        "message" => "Registered successfully"
    ]);
} else {
    echo json_encode([
        "status" => "error",
        "message" => "Failed to register: " . mysqli_error($con)
    ]);
}

?>