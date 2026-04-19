<?php

include('connection.php');

$uid      = $_POST['uid'];
$lat      = $_POST['latitude'];
$lng      = $_POST['longitude'];
$accuracy = $_POST['accuracy'] ?? null;
$speed    = $_POST['speed'] ?? null;

if (!$uid || !$lat || !$lng) {
    echo json_encode(["success" => false, "msg" => "Invalid data"]);
    exit;
}

$lastSql = "SELECT latitude, longitude, created_at 
            FROM user_location_history 
            WHERE uid = '$uid' 
            ORDER BY created_at DESC 
            LIMIT 1";

$lastResult = mysqli_query($con, $lastSql);
$lastRow = mysqli_fetch_assoc($lastResult);

$shouldInsert = false;

if (!$lastRow) {
    // First location
    $shouldInsert = true;
} else {

    // Time difference
    $lastTime = strtotime($lastRow['created_at']);
    $nowTime  = time();
    $timeDiff = $nowTime - $lastTime;

    // Distance calculation (Haversine)
    function distanceMeters($lat1, $lon1, $lat2, $lon2) {
        $earthRadius = 6371000;

        $dLat = deg2rad($lat2 - $lat1);
        $dLon = deg2rad($lon2 - $lon1);

        $a = sin($dLat/2) * sin($dLat/2) +
             cos(deg2rad($lat1)) * cos(deg2rad($lat2)) *
             sin($dLon/2) * sin($dLon/2);

        $c = 2 * atan2(sqrt($a), sqrt(1 - $a));
        return $earthRadius * $c;
    }

    $distance = distanceMeters(
        $lastRow['latitude'],
        $lastRow['longitude'],
        $lat,
        $lng
    );

    // Insert if moved ≥10 meters OR ≥10 seconds passed
    if ($distance >= 10 || $timeDiff >= 10) {
        $shouldInsert = true;
    }
}

if ($shouldInsert) {

    $insertSql = "INSERT INTO user_location_history 
                  (uid, latitude, longitude, accuracy, speed) 
                  VALUES 
                  ('$uid', '$lat', '$lng', '$accuracy', '$speed')";

    if (mysqli_query($con, $insertSql)) {
        echo json_encode([
            "success" => true,
            "inserted" => true
        ]);
    } else {
        echo json_encode([
            "success" => false,
            "error" => mysqli_error($con)
        ]);
    }

} else {
    echo json_encode([
        "success" => true,
        "inserted" => false
    ]);
}

exit;
?>
