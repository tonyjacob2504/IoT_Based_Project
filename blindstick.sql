-- phpMyAdmin SQL Dump
-- version 5.2.1
-- https://www.phpmyadmin.net/
--
-- Host: localhost
-- Generation Time: Mar 03, 2026 at 08:15 PM
-- Server version: 10.4.28-MariaDB
-- PHP Version: 8.2.4

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- Database: `blindstick`
--

-- --------------------------------------------------------

--
-- Table structure for table `care_taker`
--

CREATE TABLE `care_taker` (
  `id` int(11) NOT NULL,
  `phone_number` varchar(20) NOT NULL,
  `password` varchar(20) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `care_taker`
--

INSERT INTO `care_taker` (`id`, `phone_number`, `password`) VALUES
(1, '7012656981', '12345');

-- --------------------------------------------------------

--
-- Table structure for table `tbl_sos`
--

CREATE TABLE `tbl_sos` (
  `id` int(11) NOT NULL,
  `uid` int(11) NOT NULL,
  `sos_status` int(11) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `tbl_sos`
--

INSERT INTO `tbl_sos` (`id`, `uid`, `sos_status`) VALUES
(1, 1, 0);

-- --------------------------------------------------------

--
-- Table structure for table `tbl_user_location`
--

CREATE TABLE `tbl_user_location` (
  `id` int(11) NOT NULL,
  `uid` int(11) NOT NULL,
  `lat` varchar(50) NOT NULL,
  `lon` varchar(50) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `tbl_user_location`
--

INSERT INTO `tbl_user_location` (`id`, `uid`, `lat`, `lon`) VALUES
(1, 1, '10.0058715', '76.3050618');

-- --------------------------------------------------------

--
-- Table structure for table `users`
--

CREATE TABLE `users` (
  `id` int(10) UNSIGNED NOT NULL,
  `name` varchar(120) NOT NULL,
  `email` varchar(160) DEFAULT NULL,
  `phone` varchar(20) DEFAULT NULL,
  `password` varchar(255) NOT NULL,
  `care_taker` varchar(15) NOT NULL,
  `battery_per` varchar(5) DEFAULT NULL,
  `is_active` tinyint(1) NOT NULL DEFAULT 1,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `users`
--

INSERT INTO `users` (`id`, `name`, `email`, `phone`, `password`, `care_taker`, `battery_per`, `is_active`, `created_at`) VALUES
(1, 'abi', 'abin@gmail.com', '7012656981', 'qwerty12345', '7012656981', '100', 1, '2025-10-28 14:54:24'),
(5, 'a', 'abinn@gmail.com', '8157988437', 'qwerty12345', '', '', 1, '2025-10-28 20:49:49'),
(8, 'test', 'abin12@gmail.com', '7012656984', 'qwerty12345', '7012656981', NULL, 1, '2026-01-01 17:56:30'),
(9, 'test', 'test@gmail.com', '7012656980', 'test12345', '7012656981', NULL, 1, '2026-01-01 17:56:52');

-- --------------------------------------------------------

--
-- Table structure for table `user_location_history`
--

CREATE TABLE `user_location_history` (
  `id` int(11) NOT NULL,
  `user_id` int(11) NOT NULL,
  `latitude` varchar(50) NOT NULL,
  `longitude` varchar(50) NOT NULL,
  `accuracy` varchar(50) DEFAULT NULL,
  `speed` varchar(50) DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `user_location_history`
--

INSERT INTO `user_location_history` (`id`, `user_id`, `latitude`, `longitude`, `accuracy`, `speed`, `created_at`) VALUES
(1, 1, '9.9541009', '76.2427335', '', '', '2026-01-01 19:54:53'),
(2, 1, '9.9651200', '76.2929800', '6.5', '1.2', '2026-01-01 20:23:50'),
(3, 1, '9.9651800', '76.2930500', '6.2', '1.3', '2026-01-01 20:24:50'),
(4, 1, '9.9652300', '76.2931200', '6.1', '1.4', '2026-01-01 20:25:50'),
(5, 1, '9.9652700', '76.2931800', '5.9', '1.2', '2026-01-01 20:26:50'),
(6, 1, '9.9653000', '76.2932200', '5.8', '1.1', '2026-01-01 20:27:50'),
(7, 1, '9.9653150', '76.2932400', '5.6', '1.0', '2026-01-01 20:28:50'),
(8, 1, '9.9653200', '76.2932500', '5.5', '0.9', '2026-01-01 20:29:50'),
(9, 1, '9.9653230', '76.2932550', '5.4', '0.8', '2026-01-01 20:30:50'),
(10, 1, '9.9653250', '76.2932580', '5.3', '0.7', '2026-01-01 20:31:50'),
(11, 1, '9.9556079', '76.2922853', '5.2', '0.6', '2026-01-01 20:32:50'),
(12, 1, '9.9541353', '76.2427106', '', '', '2026-01-01 20:32:50'),
(13, 1, '9.9540982', '76.2427689', '', '', '2026-01-02 15:14:04'),
(14, 1, '10.006823', '76.3044216', '', '', '2026-01-09 12:35:01'),
(15, 1, '10.0070262', '76.3046878', '', '', '2026-01-09 12:36:09'),
(16, 1, '10.0068638', '76.3043963', '', '', '2026-01-09 12:45:33'),
(17, 1, '10.0065945', '76.3042688', '', '', '2026-01-09 13:05:08'),
(18, 1, '10.0069321', '76.3044718', '', '', '2026-01-09 13:06:20'),
(19, 1, '10.0068254', '76.3044838', '', '', '2026-01-09 13:08:47'),
(20, 1, '10.007014', '76.3044802', '', '', '2026-01-09 13:10:50'),
(21, 1, '10.0068564', '76.3044022', '', '', '2026-01-09 13:12:10'),
(22, 1, '10.0067373', '76.3045389', '', '', '2026-01-09 13:20:43'),
(23, 1, '10.0068579', '76.3044209', '', '', '2026-01-09 13:23:03'),
(24, 1, '10.0070322', '76.3045287', '', '', '2026-01-09 13:26:37'),
(25, 1, '10.0069176', '76.3044683', '', '', '2026-01-09 13:26:42'),
(26, 1, '9.953955', '76.2428337', '', '', '2026-02-17 12:43:12'),
(27, 1, '9.9541957', '76.2428451', '', '', '2026-02-17 12:51:21'),
(28, 1, '9.9540379', '76.242798', '', '', '2026-02-17 13:05:52'),
(29, 1, '9.9573883', '76.2424939', '', '', '2026-02-17 13:15:06'),
(30, 1, '9.9540207', '76.242641', '', '', '2026-02-17 13:17:57'),
(31, 1, '9.9540501', '76.2427611', '', '', '2026-02-17 13:21:47'),
(32, 1, '9.9543666', '76.242936', '', '', '2026-02-17 16:14:53'),
(33, 1, '9.9544635', '76.2427472', '', '', '2026-02-17 16:20:14'),
(34, 1, '9.9540235', '76.2428076', '', '', '2026-02-17 16:21:40'),
(35, 1, '9.9539634', '76.2429095', '', '', '2026-02-17 16:46:43'),
(36, 1, '10.0058923', '76.3050287', '', '', '2026-02-19 15:48:17'),
(37, 1, '10.0059496', '76.3048774', '', '', '2026-02-19 16:19:08'),
(38, 1, '10.0058926', '76.3050285', '', '', '2026-02-19 16:19:38');

--
-- Indexes for dumped tables
--

--
-- Indexes for table `care_taker`
--
ALTER TABLE `care_taker`
  ADD PRIMARY KEY (`id`);

--
-- Indexes for table `tbl_sos`
--
ALTER TABLE `tbl_sos`
  ADD PRIMARY KEY (`id`);

--
-- Indexes for table `tbl_user_location`
--
ALTER TABLE `tbl_user_location`
  ADD PRIMARY KEY (`id`);

--
-- Indexes for table `users`
--
ALTER TABLE `users`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `email` (`email`),
  ADD UNIQUE KEY `phone` (`phone`);

--
-- Indexes for table `user_location_history`
--
ALTER TABLE `user_location_history`
  ADD PRIMARY KEY (`id`),
  ADD KEY `idx_user_time` (`user_id`,`created_at`),
  ADD KEY `idx_time` (`created_at`);

--
-- AUTO_INCREMENT for dumped tables
--

--
-- AUTO_INCREMENT for table `care_taker`
--
ALTER TABLE `care_taker`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=2;

--
-- AUTO_INCREMENT for table `tbl_sos`
--
ALTER TABLE `tbl_sos`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=2;

--
-- AUTO_INCREMENT for table `tbl_user_location`
--
ALTER TABLE `tbl_user_location`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=2;

--
-- AUTO_INCREMENT for table `users`
--
ALTER TABLE `users`
  MODIFY `id` int(10) UNSIGNED NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=10;

--
-- AUTO_INCREMENT for table `user_location_history`
--
ALTER TABLE `user_location_history`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=39;
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
