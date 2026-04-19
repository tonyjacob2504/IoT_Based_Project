<?php

include('connection.php');

$uid = $_POST['uid'];
$battery = $_POST['battery'];

$sql = "UPDATE users SET battery_per = '$battery' WHERE id = '$uid'";
if(mysqli_query($con, $sql))
{
	echo "success";
}
else{
	echo "failed";
}

?>