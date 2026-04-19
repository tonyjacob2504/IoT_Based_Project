<?php

include('connection.php');

$uid = $_REQUEST['uid']; 
// $uid = "1";

$sql = "SELECT * FROM tbl_user_location WHERE uid = '$uid'";
if($result = mysqli_query($con, $sql))
	$row = mysqli_fetch_assoc($result);

$data["data"] = $row;
// echo json_encode($data);

$sql = "SELECT * FROM users WHERE id = '$uid'";
if($result = mysqli_query($con, $sql))
	$row = mysqli_fetch_assoc($result);

$data["battery"] = $row['battery_per'];

echo json_encode($data);

?>