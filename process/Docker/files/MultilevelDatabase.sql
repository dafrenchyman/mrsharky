#############################################################################
# project: 	GriddedClimateData
# filename:	DatasetDatabase.sql
# Notes:	MYSQL script
# created: 	2016-06-17	- jpierret
#############################################################################

SHOW ERRORS;
SHOW WARNINGS;
COMMIT;

#SET GLOBAL innodb_file_per_table=1;
#SET GLOBAL innodb_file_format=Barracuda;

#############################################################################
# Table: Level
#############################################################################
DROP TABLE IF EXISTS `Level`;
CREATE TABLE `Level` (
	`Level_ID`           TINYINT UNSIGNED NOT NULL AUTO_INCREMENT, 
	`Name`           		VARCHAR(500) NOT NULL,
	`Description`			VARCHAR(2000) NULL,
	PRIMARY KEY (`Level_ID`), 
	UNIQUE KEY `Name_U` (`Name`)
) ENGINE=MyISAM DEFAULT CHARSET=latin1;

#############################################################################
# Stored Procedure: p_GetLevel
#############################################################################
DELIMITER $$
DROP PROCEDURE IF EXISTS p_GetLevel;$$
CREATE PROCEDURE p_GetLevel()
BEGIN
	SELECT 
			L.Level_ID
			, L.Name
		FROM `Level` L
		ORDER BY L.Level_ID ASC;
END$$
DELIMITER ;

DELIMITER $$
DROP PROCEDURE IF EXISTS p_InsertLevel;$$
CREATE PROCEDURE p_InsertLevel(
	IN `in_Name`				VARCHAR(500),	
	IN `in_Description`		VARCHAR(2000)
	)
BEGIN
	INSERT IGNORE INTO `Level` (`Name`, `Description`) VALUES (`in_Name`, `in_Description`)
	ON DUPLICATE KEY UPDATE `Description` = VALUES(`Description`);
	SELECT `Level_ID` FROM Level WHERE Name = in_Name;
END$$
DELIMITER ;

#############################################################################
# Table: Lat
#############################################################################
DROP TABLE IF EXISTS `Lat`;
CREATE TABLE `Lat` ( 
	`Lat_ID`					SMALLINT	UNSIGNED NOT NULL AUTO_INCREMENT, 
	`Lat`						DOUBLE	NOT NULL, 
	PRIMARY KEY (`Lat_ID`), 
	UNIQUE KEY `Lat_U` (`Lat`) 
) ENGINE=MyISAM DEFAULT CHARSET=latin1;

#############################################################################
# Stored Procedure: Lat
#############################################################################
DELIMITER $$
DROP PROCEDURE IF EXISTS p_InsertLat;$$
CREATE PROCEDURE p_InsertLat(
	IN `in_Lat`				DOUBLE
	)
BEGIN
	INSERT IGNORE INTO `Lat` (`Lat`) VALUES (`in_Lat`);
	SELECT `Lat_ID` FROM Lat WHERE Lat = in_Lat;
END$$
DELIMITER ;

#############################################################################
# Table: Lon
#############################################################################
DROP TABLE IF EXISTS `Lon`;
CREATE TABLE `Lon` ( 
	`Lon_ID` 				SMALLINT	UNSIGNED NOT NULL AUTO_INCREMENT, 
	`Lon`						DOUBLE	NOT NULL, 
	PRIMARY KEY (`Lon_ID`), 
	UNIQUE KEY `Lon_U` (`Lon`) 
) ENGINE=MyISAM DEFAULT CHARSET=latin1;

#############################################################################
# Stored Procedure: Lon
#############################################################################
DELIMITER $$
DROP PROCEDURE IF EXISTS p_InsertLon;$$
CREATE PROCEDURE p_InsertLon(
	IN `in_Lon`				DOUBLE
	)
BEGIN
	INSERT IGNORE INTO `Lon` (`Lon`) VALUES (`in_Lon`);
	SELECT `Lon_ID` FROM Lon WHERE Lon = in_Lon;
END$$
DELIMITER ;

#############################################################################
# Table: Gridbox
#############################################################################
DROP TABLE IF EXISTS `GridBox`;
CREATE TABLE `GridBox` ( 
	`GridBox_ID` 			SMALLINT	UNSIGNED NOT NULL AUTO_INCREMENT, 
	`Lat_ID`					SMALLINT	UNSIGNED NOT NULL, 
	`Lon_ID`					SMALLINT	UNSIGNED NOT NULL,  
	PRIMARY KEY (`GridBox_ID`), 
	UNIQUE KEY `Lat_lon_U` (`Lat_ID`, `Lon_ID`)
) ENGINE=MyISAM DEFAULT CHARSET=latin1;

#############################################################################
# Table: Date
#############################################################################
DROP TABLE IF EXISTS `Date`;
CREATE TABLE `Date` ( 
	`Date_ID` 				SMALLINT	UNSIGNED NOT NULL AUTO_INCREMENT, 
	`Date` 					DATE		NOT NULL, 
	PRIMARY KEY (`Date_ID`),
	UNIQUE KEY `Date_U` (`Date`)
) ENGINE=MyISAM DEFAULT CHARSET=latin1;

#############################################################################
# Stored Procedure: p_GetDate
#############################################################################
DELIMITER $$
DROP PROCEDURE IF EXISTS p_GetDate;$$
CREATE PROCEDURE p_GetDate()
BEGIN
	SELECT 
			D.Date_ID
			, D.Date
		FROM `Date` D
		ORDER BY D.Date ASC;
END$$
DELIMITER ;

#############################################################################
# Stored Procedure: p_InsertLatLon
#############################################################################
DELIMITER $$
DROP PROCEDURE IF EXISTS p_InsertLatLon;$$
CREATE PROCEDURE p_InsertLatLon(
	IN `in_Lat`				DOUBLE,
	IN `in_Lon`				DOUBLE
	)
BEGIN
	DECLARE Lat_ID_out SMALLINT UNSIGNED;
	DECLARE Lon_ID_out SMALLINT UNSIGNED;
	
	# Lat
	INSERT IGNORE INTO `Lat` (`Lat`) VALUES (`in_Lat`);
	SET `Lat_ID_out` = (SELECT `Lat_ID` FROM `Lat` WHERE `Lat` = `in_Lat`);
	
	# Lon
	INSERT IGNORE INTO `Lon` (`Lon`) VALUES (`in_Lon`);
	SET `Lon_ID_out` = (SELECT `Lon_ID` FROM `Lon` WHERE `Lon` = `in_Lon`);

	# Gridbox
	INSERT IGNORE INTO `GridBox` (Lat_ID, Lon_ID) VALUES (Lat_ID_out, Lon_ID_out);
	#SELECT 
	#		GridBox_ID
	#		, Lat_ID
	#		, Lon_ID
	#	FROM GridBox WHERE Lat_ID = Lat_ID_out AND lon_ID = Lon_ID_out;
END$$
DELIMITER ;

#############################################################################
# Stored Procedure: p_InsertLatLon
#############################################################################
DELIMITER $$
DROP PROCEDURE IF EXISTS p_GetGridBox;$$
CREATE PROCEDURE p_GetGridBox()
BEGIN
	SELECT 
			G.GridBox_ID
			, Lat.Lat_ID
			, Lon.Lon_ID
			, Lat.Lat
			, Lon.Lon
		FROM GridBox G
		JOIN Lat ON Lat.Lat_ID = G.Lat_ID
		JOIN Lon ON Lon.Lon_ID = G.Lon_ID
		ORDER BY Lat ASC, Lon ASC;
END$$
DELIMITER ;

# CALL p_GetGridBox();

#############################################################################
# Table: GridData
#############################################################################
DROP TABLE IF EXISTS `s_GridData`;
CREATE TABLE `s_GridData` ( 
	`Level_ID` 				TINYINT		UNSIGNED NOT NULL, 
	`GridBox_ID`			SMALLINT 	UNSIGNED NOT NULL, 
	`Date_ID` 				SMALLINT 	UNSIGNED NOT NULL, 
	`Value`					FLOAT(8,5)	NULL
)ENGINE=MyISAM DEFAULT CHARSET=latin1;# ROW_FORMAT=COMPRESSED KEY_BLOCK_SIZE=8; 

DROP TABLE IF EXISTS `GridData`;
CREATE TABLE `GridData` ( 
	`GridBox_ID`			SMALLINT 	UNSIGNED NOT NULL, 
	`Date_ID` 				SMALLINT 	UNSIGNED NOT NULL, 
	PRIMARY KEY (`GridBox_ID`, `Date_ID`),
	INDEX `GridData_Date_IX` (`Date_ID`)
)ENGINE=MyISAM DEFAULT CHARSET=latin1;
#PARTITION BY KEY(Date_ID) PARTITIONS 12;

DELIMITER $$
DROP PROCEDURE IF EXISTS p_GetGridData;$$
CREATE PROCEDURE p_GetGridData(
	IN `in_level_ID`		TINYINT UNSIGNED,
	IN `in_date_ID`			DATE
	)
BEGIN	

	IF (in_level_ID = 1) THEN
		SELECT
				gd.GridBox_ID, Lat.Lat, Lon.Lon, gd.Value_001 AS Value
			FROM GridData gd FORCE INDEX (GridData_Date_IX)
			JOIN GridBox gb ON gd.GridBox_ID = gb.GridBox_ID
			JOIN Lat ON Lat.Lat_ID = gb.Lat_ID
			JOIN Lon ON Lon.Lon_ID = gb.Lon_ID
			JOIN Date d ON d.Date_ID = gd.Date_ID
			WHERE d.Date = in_date_ID
			ORDER BY Lat.Lat ASC, Lon.Lon ASC;
	ELSEIF (in_level_ID = 2) THEN
		SELECT
				gd.GridBox_ID, Lat.Lat, Lon.Lon, gd.Value_002 AS Value
			FROM GridData gd FORCE INDEX (GridData_Date_IX)
			JOIN GridBox gb ON gd.GridBox_ID = gb.GridBox_ID
			JOIN Lat ON Lat.Lat_ID = gb.Lat_ID
			JOIN Lon ON Lon.Lon_ID = gb.Lon_ID
			JOIN Date d ON d.Date_ID = gd.Date_ID
			WHERE d.Date = in_date_ID
			ORDER BY Lat.Lat ASC, Lon.Lon ASC;
	ELSEIF (in_level_ID = 3) THEN
		SELECT
				gd.GridBox_ID, Lat.Lat, Lon.Lon, gd.Value_003 AS Value
			FROM GridData gd FORCE INDEX (GridData_Date_IX)
			JOIN GridBox gb ON gd.GridBox_ID = gb.GridBox_ID
			JOIN Lat ON Lat.Lat_ID = gb.Lat_ID
			JOIN Lon ON Lon.Lon_ID = gb.Lon_ID
			JOIN Date d ON d.Date_ID = gd.Date_ID
			WHERE d.Date = in_date_ID
			ORDER BY Lat.Lat ASC, Lon.Lon ASC;
	ELSEIF (in_level_ID = 4) THEN
		SELECT
				gd.GridBox_ID, Lat.Lat, Lon.Lon, gd.Value_004 AS Value
			FROM GridData gd FORCE INDEX (GridData_Date_IX)
			JOIN GridBox gb ON gd.GridBox_ID = gb.GridBox_ID
			JOIN Lat ON Lat.Lat_ID = gb.Lat_ID
			JOIN Lon ON Lon.Lon_ID = gb.Lon_ID
			JOIN Date d ON d.Date_ID = gd.Date_ID
			WHERE d.Date = in_date_ID
			ORDER BY Lat.Lat ASC, Lon.Lon ASC;
	ELSEIF (in_level_ID = 5) THEN
		SELECT
				gd.GridBox_ID, Lat.Lat, Lon.Lon, gd.Value_005 AS Value
			FROM GridData gd FORCE INDEX (GridData_Date_IX)
			JOIN GridBox gb ON gd.GridBox_ID = gb.GridBox_ID
			JOIN Lat ON Lat.Lat_ID = gb.Lat_ID
			JOIN Lon ON Lon.Lon_ID = gb.Lon_ID
			JOIN Date d ON d.Date_ID = gd.Date_ID
			WHERE d.Date = in_date_ID
			ORDER BY Lat.Lat ASC, Lon.Lon ASC;
	ELSEIF (in_level_ID = 6) THEN
		SELECT
				gd.GridBox_ID, Lat.Lat, Lon.Lon, gd.Value_006 AS Value
			FROM GridData gd FORCE INDEX (GridData_Date_IX)
			JOIN GridBox gb ON gd.GridBox_ID = gb.GridBox_ID
			JOIN Lat ON Lat.Lat_ID = gb.Lat_ID
			JOIN Lon ON Lon.Lon_ID = gb.Lon_ID
			JOIN Date d ON d.Date_ID = gd.Date_ID
			WHERE d.Date = in_date_ID
			ORDER BY Lat.Lat ASC, Lon.Lon ASC;
	ELSEIF (in_level_ID = 7) THEN
		SELECT
				gd.GridBox_ID, Lat.Lat, Lon.Lon, gd.Value_007 AS Value
			FROM GridData gd FORCE INDEX (GridData_Date_IX)
			JOIN GridBox gb ON gd.GridBox_ID = gb.GridBox_ID
			JOIN Lat ON Lat.Lat_ID = gb.Lat_ID
			JOIN Lon ON Lon.Lon_ID = gb.Lon_ID
			JOIN Date d ON d.Date_ID = gd.Date_ID
			WHERE d.Date = in_date_ID
			ORDER BY Lat.Lat ASC, Lon.Lon ASC;
	ELSEIF (in_level_ID = 8) THEN
		SELECT
				gd.GridBox_ID, Lat.Lat, Lon.Lon, gd.Value_008 AS Value
			FROM GridData gd FORCE INDEX (GridData_Date_IX)
			JOIN GridBox gb ON gd.GridBox_ID = gb.GridBox_ID
			JOIN Lat ON Lat.Lat_ID = gb.Lat_ID
			JOIN Lon ON Lon.Lon_ID = gb.Lon_ID
			JOIN Date d ON d.Date_ID = gd.Date_ID
			WHERE d.Date = in_date_ID
			ORDER BY Lat.Lat ASC, Lon.Lon ASC;
	ELSEIF (in_level_ID = 9) THEN
		SELECT
				gd.GridBox_ID, Lat.Lat, Lon.Lon, gd.Value_009 AS Value
			FROM GridData gd FORCE INDEX (GridData_Date_IX)
			JOIN GridBox gb ON gd.GridBox_ID = gb.GridBox_ID
			JOIN Lat ON Lat.Lat_ID = gb.Lat_ID
			JOIN Lon ON Lon.Lon_ID = gb.Lon_ID
			JOIN Date d ON d.Date_ID = gd.Date_ID
			WHERE d.Date = in_date_ID
			ORDER BY Lat.Lat ASC, Lon.Lon ASC;
	ELSEIF (in_level_ID = 10) THEN
		SELECT
				gd.GridBox_ID, Lat.Lat, Lon.Lon, gd.Value_010 AS Value
			FROM GridData gd FORCE INDEX (GridData_Date_IX)
			JOIN GridBox gb ON gd.GridBox_ID = gb.GridBox_ID
			JOIN Lat ON Lat.Lat_ID = gb.Lat_ID
			JOIN Lon ON Lon.Lon_ID = gb.Lon_ID
			JOIN Date d ON d.Date_ID = gd.Date_ID
			WHERE d.Date = in_date_ID
			ORDER BY Lat.Lat ASC, Lon.Lon ASC;
	ELSEIF (in_level_ID = 11) THEN
		SELECT
				gd.GridBox_ID, Lat.Lat, Lon.Lon, gd.Value_011 AS Value
			FROM GridData gd FORCE INDEX (GridData_Date_IX)
			JOIN GridBox gb ON gd.GridBox_ID = gb.GridBox_ID
			JOIN Lat ON Lat.Lat_ID = gb.Lat_ID
			JOIN Lon ON Lon.Lon_ID = gb.Lon_ID
			JOIN Date d ON d.Date_ID = gd.Date_ID
			WHERE d.Date = in_date_ID
			ORDER BY Lat.Lat ASC, Lon.Lon ASC;
	ELSEIF (in_level_ID = 12) THEN
		SELECT
				gd.GridBox_ID, Lat.Lat, Lon.Lon, gd.Value_012 AS Value
			FROM GridData gd FORCE INDEX (GridData_Date_IX)
			JOIN GridBox gb ON gd.GridBox_ID = gb.GridBox_ID
			JOIN Lat ON Lat.Lat_ID = gb.Lat_ID
			JOIN Lon ON Lon.Lon_ID = gb.Lon_ID
			JOIN Date d ON d.Date_ID = gd.Date_ID
			WHERE d.Date = in_date_ID
			ORDER BY Lat.Lat ASC, Lon.Lon ASC;
	ELSEIF (in_level_ID = 13) THEN
		SELECT
				gd.GridBox_ID, Lat.Lat, Lon.Lon, gd.Value_013 AS Value
			FROM GridData gd FORCE INDEX (GridData_Date_IX)
			JOIN GridBox gb ON gd.GridBox_ID = gb.GridBox_ID
			JOIN Lat ON Lat.Lat_ID = gb.Lat_ID
			JOIN Lon ON Lon.Lon_ID = gb.Lon_ID
			JOIN Date d ON d.Date_ID = gd.Date_ID
			WHERE d.Date = in_date_ID
			ORDER BY Lat.Lat ASC, Lon.Lon ASC;
	ELSEIF (in_level_ID = 14) THEN
		SELECT
				gd.GridBox_ID, Lat.Lat, Lon.Lon, gd.Value_014 AS Value
			FROM GridData gd FORCE INDEX (GridData_Date_IX)
			JOIN GridBox gb ON gd.GridBox_ID = gb.GridBox_ID
			JOIN Lat ON Lat.Lat_ID = gb.Lat_ID
			JOIN Lon ON Lon.Lon_ID = gb.Lon_ID
			JOIN Date d ON d.Date_ID = gd.Date_ID
			WHERE d.Date = in_date_ID
			ORDER BY Lat.Lat ASC, Lon.Lon ASC;
	ELSEIF (in_level_ID = 15) THEN
		SELECT
				gd.GridBox_ID, Lat.Lat, Lon.Lon, gd.Value_015 AS Value
			FROM GridData gd FORCE INDEX (GridData_Date_IX)
			JOIN GridBox gb ON gd.GridBox_ID = gb.GridBox_ID
			JOIN Lat ON Lat.Lat_ID = gb.Lat_ID
			JOIN Lon ON Lon.Lon_ID = gb.Lon_ID
			JOIN Date d ON d.Date_ID = gd.Date_ID
			WHERE d.Date = in_date_ID
			ORDER BY Lat.Lat ASC, Lon.Lon ASC;
	ELSEIF (in_level_ID = 16) THEN
		SELECT
				gd.GridBox_ID, Lat.Lat, Lon.Lon, gd.Value_016 AS Value
			FROM GridData gd FORCE INDEX (GridData_Date_IX)
			JOIN GridBox gb ON gd.GridBox_ID = gb.GridBox_ID
			JOIN Lat ON Lat.Lat_ID = gb.Lat_ID
			JOIN Lon ON Lon.Lon_ID = gb.Lon_ID
			JOIN Date d ON d.Date_ID = gd.Date_ID
			WHERE d.Date = in_date_ID
			ORDER BY Lat.Lat ASC, Lon.Lon ASC;
	ELSEIF (in_level_ID = 17) THEN
		SELECT
				gd.GridBox_ID, Lat.Lat, Lon.Lon, gd.Value_017 AS Value
			FROM GridData gd FORCE INDEX (GridData_Date_IX)
			JOIN GridBox gb ON gd.GridBox_ID = gb.GridBox_ID
			JOIN Lat ON Lat.Lat_ID = gb.Lat_ID
			JOIN Lon ON Lon.Lon_ID = gb.Lon_ID
			JOIN Date d ON d.Date_ID = gd.Date_ID
			WHERE d.Date = in_date_ID
			ORDER BY Lat.Lat ASC, Lon.Lon ASC;
	ELSEIF (in_level_ID = 18) THEN
		SELECT
				gd.GridBox_ID, Lat.Lat, Lon.Lon, gd.Value_018 AS Value
			FROM GridData gd FORCE INDEX (GridData_Date_IX)
			JOIN GridBox gb ON gd.GridBox_ID = gb.GridBox_ID
			JOIN Lat ON Lat.Lat_ID = gb.Lat_ID
			JOIN Lon ON Lon.Lon_ID = gb.Lon_ID
			JOIN Date d ON d.Date_ID = gd.Date_ID
			WHERE d.Date = in_date_ID
			ORDER BY Lat.Lat ASC, Lon.Lon ASC;
	ELSEIF (in_level_ID = 19) THEN
		SELECT
				gd.GridBox_ID, Lat.Lat, Lon.Lon, gd.Value_019 AS Value
			FROM GridData gd FORCE INDEX (GridData_Date_IX)
			JOIN GridBox gb ON gd.GridBox_ID = gb.GridBox_ID
			JOIN Lat ON Lat.Lat_ID = gb.Lat_ID
			JOIN Lon ON Lon.Lon_ID = gb.Lon_ID
			JOIN Date d ON d.Date_ID = gd.Date_ID
			WHERE d.Date = in_date_ID
			ORDER BY Lat.Lat ASC, Lon.Lon ASC;
	ELSEIF (in_level_ID = 20) THEN
		SELECT
				gd.GridBox_ID, Lat.Lat, Lon.Lon, gd.Value_020 AS Value
			FROM GridData gd FORCE INDEX (GridData_Date_IX)
			JOIN GridBox gb ON gd.GridBox_ID = gb.GridBox_ID
			JOIN Lat ON Lat.Lat_ID = gb.Lat_ID
			JOIN Lon ON Lon.Lon_ID = gb.Lon_ID
			JOIN Date d ON d.Date_ID = gd.Date_ID
			WHERE d.Date = in_date_ID
			ORDER BY Lat.Lat ASC, Lon.Lon ASC;
	ELSEIF (in_level_ID = 21) THEN
		SELECT
				gd.GridBox_ID, Lat.Lat, Lon.Lon, gd.Value_021 AS Value
			FROM GridData gd FORCE INDEX (GridData_Date_IX)
			JOIN GridBox gb ON gd.GridBox_ID = gb.GridBox_ID
			JOIN Lat ON Lat.Lat_ID = gb.Lat_ID
			JOIN Lon ON Lon.Lon_ID = gb.Lon_ID
			JOIN Date d ON d.Date_ID = gd.Date_ID
			WHERE d.Date = in_date_ID
			ORDER BY Lat.Lat ASC, Lon.Lon ASC;
	ELSEIF (in_level_ID = 22) THEN
		SELECT
				gd.GridBox_ID, Lat.Lat, Lon.Lon, gd.Value_022 AS Value
			FROM GridData gd FORCE INDEX (GridData_Date_IX)
			JOIN GridBox gb ON gd.GridBox_ID = gb.GridBox_ID
			JOIN Lat ON Lat.Lat_ID = gb.Lat_ID
			JOIN Lon ON Lon.Lon_ID = gb.Lon_ID
			JOIN Date d ON d.Date_ID = gd.Date_ID
			WHERE d.Date = in_date_ID
			ORDER BY Lat.Lat ASC, Lon.Lon ASC;
	ELSEIF (in_level_ID = 23) THEN
		SELECT
				gd.GridBox_ID, Lat.Lat, Lon.Lon, gd.Value_023 AS Value
			FROM GridData gd FORCE INDEX (GridData_Date_IX)
			JOIN GridBox gb ON gd.GridBox_ID = gb.GridBox_ID
			JOIN Lat ON Lat.Lat_ID = gb.Lat_ID
			JOIN Lon ON Lon.Lon_ID = gb.Lon_ID
			JOIN Date d ON d.Date_ID = gd.Date_ID
			WHERE d.Date = in_date_ID
			ORDER BY Lat.Lat ASC, Lon.Lon ASC;
	ELSEIF (in_level_ID = 24) THEN
		SELECT
				gd.GridBox_ID, Lat.Lat, Lon.Lon, gd.Value_024 AS Value
			FROM GridData gd FORCE INDEX (GridData_Date_IX)
			JOIN GridBox gb ON gd.GridBox_ID = gb.GridBox_ID
			JOIN Lat ON Lat.Lat_ID = gb.Lat_ID
			JOIN Lon ON Lon.Lon_ID = gb.Lon_ID
			JOIN Date d ON d.Date_ID = gd.Date_ID
			WHERE d.Date = in_date_ID
			ORDER BY Lat.Lat ASC, Lon.Lon ASC;
	END IF;

END$$
DELIMITER ;

DROP TABLE IF EXISTS `TimeData`;
CREATE TABLE `TimeData` (
	`GridBox_ID` SMALLINT(5) UNSIGNED NOT NULL,
	`Date_ID` SMALLINT(5) UNSIGNED NOT NULL,
	PRIMARY KEY (`GridBox_ID`, `Date_ID`),
	INDEX `TimeseriesData_Grid_IX` (`GridBox_ID`)
)ENGINE=MyISAM DEFAULT CHARSET=latin1;

DELIMITER $$
DROP PROCEDURE IF EXISTS p_GetTimeseriesData;$$
CREATE PROCEDURE p_GetTimeseriesData(
	IN `in_level_ID`		TINYINT UNSIGNED,
	IN `in_GridBox_ID`		SMALLINT UNSIGNED
	)
BEGIN	

	IF (in_level_ID = 1) THEN
		SELECT  d.Date, gd.Value_001 AS Value
			FROM TimeData gd FORCE INDEX (TimeseriesData_Grid_IX) 
			JOIN Date d ON d.Date_ID = gd.Date_ID WHERE gd.GridBox_ID = in_GridBox_ID ORDER BY d.Date;
	ELSEIF (in_level_ID = 2) THEN
		SELECT  d.Date, gd.Value_002 AS Value
			FROM TimeData gd FORCE INDEX (TimeseriesData_Grid_IX) 
			JOIN Date d ON d.Date_ID = gd.Date_ID WHERE gd.GridBox_ID = in_GridBox_ID ORDER BY d.Date;
	ELSEIF (in_level_ID = 3) THEN
		SELECT d.Date, gd.Value_003 AS Value
			FROM TimeData gd FORCE INDEX (TimeseriesData_Grid_IX) 
			JOIN Date d ON d.Date_ID = gd.Date_ID WHERE gd.GridBox_ID = in_GridBox_ID ORDER BY d.Date;
	ELSEIF (in_level_ID = 4) THEN
		SELECT d.Date, gd.Value_004 AS Value
			FROM TimeData gd FORCE INDEX (TimeseriesData_Grid_IX) 
			JOIN Date d ON d.Date_ID = gd.Date_ID WHERE gd.GridBox_ID = in_GridBox_ID ORDER BY d.Date;
	ELSEIF (in_level_ID = 5) THEN
		SELECT d.Date, gd.Value_005 AS Value
			FROM TimeData gd FORCE INDEX (TimeseriesData_Grid_IX) 
			JOIN Date d ON d.Date_ID = gd.Date_ID WHERE gd.GridBox_ID = in_GridBox_ID ORDER BY d.Date;
	ELSEIF (in_level_ID = 6) THEN
		SELECT d.Date, gd.Value_006 AS Value
			FROM TimeData gd FORCE INDEX (TimeseriesData_Grid_IX) 
			JOIN Date d ON d.Date_ID = gd.Date_ID WHERE gd.GridBox_ID = in_GridBox_ID ORDER BY d.Date;
	ELSEIF (in_level_ID = 7) THEN
		SELECT d.Date, gd.Value_007 AS Value
			FROM TimeData gd FORCE INDEX (TimeseriesData_Grid_IX) 
			JOIN Date d ON d.Date_ID = gd.Date_ID WHERE gd.GridBox_ID = in_GridBox_ID ORDER BY d.Date;
	ELSEIF (in_level_ID = 8) THEN
		SELECT d.Date, gd.Value_008 AS Value
			FROM TimeData gd FORCE INDEX (TimeseriesData_Grid_IX) 
			JOIN Date d ON d.Date_ID = gd.Date_ID WHERE gd.GridBox_ID = in_GridBox_ID ORDER BY d.Date;
	ELSEIF (in_level_ID = 9) THEN
		SELECT d.Date, gd.Value_009 AS Value
			FROM TimeData gd FORCE INDEX (TimeseriesData_Grid_IX) 
			JOIN Date d ON d.Date_ID = gd.Date_ID WHERE gd.GridBox_ID = in_GridBox_ID ORDER BY d.Date;
	ELSEIF (in_level_ID = 10) THEN
		SELECT d.Date, gd.Value_010 AS Value
			FROM TimeData gd FORCE INDEX (TimeseriesData_Grid_IX) 
			JOIN Date d ON d.Date_ID = gd.Date_ID WHERE gd.GridBox_ID = in_GridBox_ID ORDER BY d.Date;
	ELSEIF (in_level_ID = 11) THEN
		SELECT d.Date, gd.Value_011 AS Value
			FROM TimeData gd FORCE INDEX (TimeseriesData_Grid_IX) 
			JOIN Date d ON d.Date_ID = gd.Date_ID WHERE gd.GridBox_ID = in_GridBox_ID ORDER BY d.Date;
	ELSEIF (in_level_ID = 12) THEN
		SELECT d.Date, gd.Value_012 AS Value
			FROM TimeData gd FORCE INDEX (TimeseriesData_Grid_IX) 
			JOIN Date d ON d.Date_ID = gd.Date_ID WHERE gd.GridBox_ID = in_GridBox_ID ORDER BY d.Date;
	ELSEIF (in_level_ID = 13) THEN
		SELECT d.Date, gd.Value_013 AS Value
			FROM TimeData gd FORCE INDEX (TimeseriesData_Grid_IX) 
			JOIN Date d ON d.Date_ID = gd.Date_ID WHERE gd.GridBox_ID = in_GridBox_ID ORDER BY d.Date;
	ELSEIF (in_level_ID = 14) THEN
		SELECT d.Date, gd.Value_014 AS Value
			FROM TimeData gd FORCE INDEX (TimeseriesData_Grid_IX) 
			JOIN Date d ON d.Date_ID = gd.Date_ID WHERE gd.GridBox_ID = in_GridBox_ID ORDER BY d.Date;
	ELSEIF (in_level_ID = 15) THEN
		SELECT d.Date, gd.Value_015 AS Value
			FROM TimeData gd FORCE INDEX (TimeseriesData_Grid_IX) 
			JOIN Date d ON d.Date_ID = gd.Date_ID WHERE gd.GridBox_ID = in_GridBox_ID ORDER BY d.Date;
	ELSEIF (in_level_ID = 16) THEN
		SELECT d.Date, gd.Value_016 AS Value
			FROM TimeData gd FORCE INDEX (TimeseriesData_Grid_IX) 
			JOIN Date d ON d.Date_ID = gd.Date_ID WHERE gd.GridBox_ID = in_GridBox_ID ORDER BY d.Date;
	ELSEIF (in_level_ID = 17) THEN
		SELECT d.Date, gd.Value_017 AS Value
			FROM TimeData gd FORCE INDEX (TimeseriesData_Grid_IX) 
			JOIN Date d ON d.Date_ID = gd.Date_ID WHERE gd.GridBox_ID = in_GridBox_ID ORDER BY d.Date;
	ELSEIF (in_level_ID = 18) THEN
		SELECT d.Date, gd.Value_018 AS Value
			FROM TimeData gd FORCE INDEX (TimeseriesData_Grid_IX) 
			JOIN Date d ON d.Date_ID = gd.Date_ID WHERE gd.GridBox_ID = in_GridBox_ID ORDER BY d.Date;
	ELSEIF (in_level_ID = 19) THEN
		SELECT d.Date, gd.Value_019 AS Value
			FROM TimeData gd FORCE INDEX (TimeseriesData_Grid_IX) 
			JOIN Date d ON d.Date_ID = gd.Date_ID WHERE gd.GridBox_ID = in_GridBox_ID ORDER BY d.Date;
	ELSEIF (in_level_ID = 20) THEN
		SELECT d.Date, gd.Value_020 AS Value
			FROM TimeData gd FORCE INDEX (TimeseriesData_Grid_IX) 
			JOIN Date d ON d.Date_ID = gd.Date_ID WHERE gd.GridBox_ID = in_GridBox_ID ORDER BY d.Date;
	ELSEIF (in_level_ID = 21) THEN
		SELECT d.Date, gd.Value_021 AS Value
			FROM TimeData gd FORCE INDEX (TimeseriesData_Grid_IX) 
			JOIN Date d ON d.Date_ID = gd.Date_ID WHERE gd.GridBox_ID = in_GridBox_ID ORDER BY d.Date;
	ELSEIF (in_level_ID = 22) THEN
		SELECT d.Date, gd.Value_022 AS Value
			FROM TimeData gd FORCE INDEX (TimeseriesData_Grid_IX) 
			JOIN Date d ON d.Date_ID = gd.Date_ID WHERE gd.GridBox_ID = in_GridBox_ID ORDER BY d.Date;
	ELSEIF (in_level_ID = 23) THEN
		SELECT d.Date, gd.Value_023  AS Value
			FROM TimeData gd FORCE INDEX (TimeseriesData_Grid_IX) 
			JOIN Date d ON d.Date_ID = gd.Date_ID WHERE gd.GridBox_ID = in_GridBox_ID ORDER BY d.Date;
	ELSEIF (in_level_ID = 24) THEN
		SELECT d.Date, gd.Value_024 AS Value
			FROM TimeData gd FORCE INDEX (TimeseriesData_Grid_IX) 
			JOIN Date d ON d.Date_ID = gd.Date_ID WHERE gd.GridBox_ID = in_GridBox_ID ORDER BY d.Date;
	END IF;

END$$
DELIMITER ;


/*
SELECT 
	(SELECT COUNT(*) FROM Level) * (SELECT COUNT(*) FROM GridBox) *
	(SELECT COUNT(*) FROM `Date`) AS total
*/
#############################################################################
# Stored Procedure
# Input dataset info, get out the IDs 
#############################################################################
DELIMITER $$
DROP PROCEDURE IF EXISTS p_InsertGridData;$$
CREATE PROCEDURE p_InsertGridData(
	IN `in_level_ID`			TINYINT UNSIGNED,
	IN `in_gridBox_ID`		SMALLINT UNSIGNED,
	IN `in_date_ID`			SMALLINT UNSIGNED,
	IN `in_value`				FLOAT(8,5)
	)
BEGIN
	DECLARE columnCount INT;
	DECLARE currentDatabase VARCHAR(1000);
	DECLARE columnName VARCHAR(1000);
	DECLARE queryString3 VARCHAR(1000);
	
	SELECT DATABASE() INTO currentDatabase;

	SELECT CONCAT('Value_', LPAD(in_level_ID, 3, '0')) INTO columnName;
	SET @queryString1 = (SELECT CONCAT('SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
           WHERE TABLE_NAME=\'GridData\' AND column_name=\'',columnName ,'\' 
           AND TABLE_SCHEMA=\'', currentDatabase ,'\' INTO @ColumnExists'));
	PREPARE sqlQuery1 FROM @queryString1;
	EXECUTE sqlQuery1;
	SET columnCount = @ColumnExists;
	DEALLOCATE PREPARE sqlQuery1; 
	
	IF columnCount = 0 THEN BEGIN
		SET @queryString2 = (SELECT CONCAT('ALTER TABLE GridData ADD ', columnName, ' FLOAT(8.5) NULL'));
		PREPARE sqlQuery2 FROM @queryString2;
		EXECUTE sqlQuery2;
		DEALLOCATE PREPARE sqlQuery2; 
	END; END IF;
	
	SET @queryString3 = CONCAT('INSERT INTO GridData (
		GridBox_ID, Date_ID, ',columnName ,') VALUES
		(',in_gridBox_ID,',', in_date_ID,',', in_value,')
		ON DUPLICATE KEY UPDATE ',columnName, '=VALUES(',columnName,')') ;
	PREPARE sqlQuery3 FROM @queryString3;
	EXECUTE sqlQuery3;
	DEALLOCATE PREPARE sqlQuery3; 

END$$
DELIMITER ;

#CALL p_InsertGridData(2,99,1,2.0)

#CALL p_GetGridData(1,1)