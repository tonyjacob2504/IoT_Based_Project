<?php

include('connection.php');

$uid = $_REQUEST['uid'];   // works for GET or POST

if (!$uid) {
    echo json_encode([
        "success" => false,
        "message" => "uid missing"
    ]);
    exit;
}

$sql = "SELECT latitude, longitude 
        FROM user_location_history 
        WHERE user_id = '$uid' 
          AND created_at >= NOW() - INTERVAL 30 MINUTE
        ORDER BY created_at ASC";

$result = mysqli_query($con, $sql);

$points = array();

if ($result) {
    while ($row = mysqli_fetch_assoc($result)) {
        $points[] = array(
            "lat" => (double)$row['latitude'],
            "lng" => (double)$row['longitude']
        );
    }

    echo json_encode([
        "success" => true,
        "points" => $points
    ]);
} else {
    echo json_encode([
        "success" => false,
        "error" => mysqli_error($con)
    ]);
}