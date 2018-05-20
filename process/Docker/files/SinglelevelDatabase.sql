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

INSERT INTO `Level`(Name, Description) VALUES ('Default', 'Default');

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
	`Value`					FLOAT(8,5)	NULL,
	PRIMARY KEY (`GridBox_ID`, `Date_ID`),
	INDEX `TimeseriesData_Grid_IX` (`GridBox_ID`),
	INDEX `GridData_Date_IX` (`Date_ID`)
)ENGINE=MyISAM DEFAULT CHARSET=latin1;
#PARTITION BY KEY(Level_ID, Date_ID) PARTITIONS 120;

DELIMITER $$
DROP PROCEDURE IF EXISTS p_GetGridData;$$
CREATE PROCEDURE p_GetGridData(
	IN `in_level_ID`		TINYINT UNSIGNED,
	IN `in_date_ID`			DATE
	)
BEGIN	
	SELECT
		gd.GridBox_ID
		, Lat.Lat
		, Lon.Lon
		, gd.Value
	FROM GridData gd FORCE INDEX (GridData_Date_IX)
	JOIN GridBox gb ON gd.GridBox_ID = gb.GridBox_ID
	JOIN Lat ON Lat.Lat_ID = gb.Lat_ID
	JOIN Lon ON Lon.Lon_ID = gb.Lon_ID
	JOIN Date d ON d.Date_ID = gd.Date_ID
	WHERE d.Date = in_date_ID
	ORDER BY Lat.Lat ASC, Lon.Lon ASC;
END$$
DELIMITER ;

DELIMITER $$
DROP PROCEDURE IF EXISTS p_GetTimeseriesData;
CREATE PROCEDURE p_GetTimeseriesData(
	IN `in_level_ID`		TINYINT UNSIGNED,
	IN `in_GridBox_ID`		SMALLINT UNSIGNED
	)
BEGIN	
	SELECT 
		1 AS Level_ID
		, d.Date
		, gd.Value
	FROM GridData gd FORCE INDEX (TimeseriesData_Grid_IX)
	JOIN Date d ON d.Date_ID = gd.Date_ID
	WHERE gd.GridBox_ID = in_GridBox_ID
	ORDER BY d.Date;
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
	INSERT INTO `GridData` (`GridBox_ID`, `Date_ID`, `Value`) VALUES 
		(`in_gridBox_ID`, `in_date_ID`, `in_value`)
		ON DUPLICATE KEY UPDATE `Value`=VALUES(`Value`);
END$$
DELIMITER ;



#############################################################################
# Stored Procedures specific to Python code research - 
#    Discrete Spherical Transform 
#############################################################################
DELIMITER $$
DROP PROCEDURE IF EXISTS g_dateRange;$$
CREATE PROCEDURE g_dateRange(
	IN `in_startDate`			DATE,
	IN `in_endDate`			DATE
	)
BEGIN	
	SELECT 
			d.Date
			, YEAR(d.Date) AS Year
			, MONTH(d.Date) AS Month
		FROM Date d 
		WHERE d.Date >= in_startDate AND d.Date <= in_endDate
		ORDER BY d.Date ASC;
END$$
DELIMITER ;





