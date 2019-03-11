

SHOW ERRORS;
SHOW WARNINGS;
COMMIT;

DROP DATABASE IF EXISTS noaa;
CREATE DATABASE IF NOT EXISTS noaa;
USE noaa;

#############################################################################
# Table: Level
#############################################################################
DROP TABLE IF EXISTS `Level`;
CREATE TABLE `Level` (
	`Level_ID`              TINYINT UNSIGNED NOT NULL AUTO_INCREMENT, 
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
	`Lat_ID`				SMALLINT	UNSIGNED NOT NULL AUTO_INCREMENT, 
	`Lat`					DOUBLE	NOT NULL, 
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
	`Lon`					DOUBLE	NOT NULL, 
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
	`GridBox_ID`            SMALLINT	UNSIGNED NOT NULL AUTO_INCREMENT, 
	`Lat_ID`				SMALLINT	UNSIGNED NOT NULL, 
	`Lon_ID`				SMALLINT	UNSIGNED NOT NULL,  
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

#############################################################################
# Table: staggingData
#############################################################################
DROP TABLE IF EXISTS `s_inputData`;
CREATE TABLE `s_inputData` ( 
	`Lat` 					DOUBLE NOT NULL, 
	`Lon`					DOUBLE NOT NULL, 
	`Date` 					DATE NOT NULL, 
	`Value`					FLOAT(8,5)	NULL
) ENGINE=MyISAM DEFAULT CHARSET=latin1;

#############################################################################
# Load Data into Stagging Table
#############################################################################
LOAD DATA INFILE '/var/lib/mysql-files/NOAAGlobal5DegTemp188001-201601.csv' 
	INTO TABLE s_inputData 
	FIELDS TERMINATED BY ',' OPTIONALLY ENCLOSED BY '"' 
LINES TERMINATED BY '\n' IGNORE 1 LINES (Lat, Lon, Date, Value);

#############################################################################
# Process data from stagging to final table
#############################################################################
INSERT INTO Lat (Lat) SELECT DISTINCT Lat FROM s_inputData ORDER BY Lat ASC;
INSERT INTO Lon (Lon) SELECT DISTINCT Lon FROM s_inputData ORDER BY Lon ASC;

INSERT INTO GridBox (Lat_ID, Lon_ID) 
SELECT 
		Lat.Lat_ID
		, Lon.Lon_ID
	FROM (SELECT DISTINCT Lat, Lon FROM s_inputData) AS LatLons
	JOIN Lat ON Lat.Lat = LatLons.Lat
	JOIN Lon ON Lon.Lon = LatLons.lon;
OPTIMIZE TABLE GridBox;

INSERT INTO `Date` (`Date`) SELECT DISTINCT `Date` FROM s_inputData;
OPTIMIZE TABLE `Date`;

INSERT INTO GridData (GridBox_ID, Date_ID, Value)
SELECT
		G.GridBox_ID
		, D.Date_ID
		, CASE 
				WHEN S.Value < -900 THEN NULL
				ELSE S.Value END AS Value
	FROM s_inputData S
	JOIN Lat ON Lat.Lat = S.Lat
	JOIN Lon ON Lon.Lon = S.lon
	JOIN GridBox G ON G.Lat_ID = Lat.Lat_ID AND G.Lon_ID = Lon.Lon_ID
	JOIN `Date` D ON D.Date = S.Date;
OPTIMIZE TABLE GridData;

SELECT COUNT(*) FROM s_inputData;
SELECT COUNT(*) FROM GridData;


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


#############################################################################
# Stored Procedure
# Input dataset info, get out the IDs 
#############################################################################
DELIMITER $$
DROP PROCEDURE IF EXISTS p_InsertGridData;$$
CREATE PROCEDURE p_InsertGridData(
	IN `in_level_ID`		TINYINT UNSIGNED,
	IN `in_gridBox_ID`		SMALLINT UNSIGNED,
	IN `in_date_ID`			SMALLINT UNSIGNED,
	IN `in_value`			FLOAT(8,5)
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
	IN `in_startDate`		DATE,
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









