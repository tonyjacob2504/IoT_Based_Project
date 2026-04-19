<?php

include('connection.php');

$uid = $_POST['uid'];

$sql = "SELECT * FROM tbl_sos WHERE uid = '$uid'";
if($result = mysqli_query($con, $sql))
{
	$row = mysqli_fetch_assoc($result);
	echo json_encode($row);
}
else{
	echo "failed";
}

?>