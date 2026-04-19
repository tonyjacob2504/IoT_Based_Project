<?php

header('Content-Type: application/json; charset=utf-8');

include 'connection.php'; // Include your DB connection


function jsonResponse($statusCode, $data) {
    http_response_code($statusCode);
    echo json_encode($data);
    exit;
}

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    jsonResponse(200, ['success' => false, 'message' => 'Method not allowed. Use POST.']);
}

$inputPhone = null;
$inputPassword = null;


if (!empty($_POST)) {
    $inputPhone = isset($_POST['phone']) ? trim($_POST['phone']) : null;
    $inputPassword = isset($_POST['password']) ? $_POST['password'] : null;

} else {
    $raw = file_get_contents('php://input');
    if ($raw) {
        $json = json_decode($raw, true);
        if (is_array($json)) {
            $inputPhone = isset($json['phone']) ? trim($json['phone']) : null;
            $inputPassword = isset($json['password']) ? $json['password'] : null;
        }
    }
}

if (empty($inputPhone) || empty($inputPassword)) {
    jsonResponse(200, ['success' => false, 'message' => 'phone and password are required.']);
}

if (!isset($con) || !($con instanceof mysqli)) {
    jsonResponse(200, ['success' => false, 'message' => 'Database connection not available.']);
}

// escape email to avoid injection (note: prepared statements are safer)

$sql = "SELECT * FROM care_taker WHERE phone_number = '$inputPhone' AND password = '$inputPassword' LIMIT 1";
$result = mysqli_query($con, $sql);
if(mysqli_num_rows($result) == 0){
    jsonResponse(200, ['success' => false, 'message' => 'Invalid phone number or password.']);
}else{

    $sql = "SELECT id FROM users WHERE care_taker = '$inputPhone' LIMIT 1";
    $result = mysqli_query($con, $sql);
    if ($result === false) {
        jsonResponse(200, ['success' => false, 'message' => 'Database query failed.']);
    }

    $row = mysqli_fetch_assoc($result);
    mysqli_free_result($result);

    if (!$row) {
        jsonResponse(200, ['success' => false, 'message' => 'Invalid email or password.']);
    }

    $id = $row['id'];

    // jsonResponse(401, ['success' => false, 'message' => 'Invalid email or password.']);

    jsonResponse(200, [
        'success' => true,
        'message' => 'Login successful.',
        'user' => [
            'id' => $id
        ]
    ]);
}

?>
