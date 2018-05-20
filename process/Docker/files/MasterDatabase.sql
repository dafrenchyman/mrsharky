#############################################################################
# project: 	GriddedClimateData
# filename:	MasterDatabase.sql
# Notes:	MYSQL script
# created: 	2016-06-17	- jpierret
#############################################################################
SHOW ERRORS;
SHOW WARNINGS;
COMMIT;

# CREATE USER 'mrsharky_climate'@'localhost' IDENTIFIED BY 'pTZjTeGg0WObPBxvjxJ6';
# GRANT ALL PRIVILEGES ON *.* TO 'mrsharky_climate'@'localhost' WITH GRANT OPTION;

DROP DATABASE IF EXISTS mrsharky_GriddedClimateData;
CREATE DATABASE IF NOT EXISTS mrsharky_GriddedClimateData;
USE mrsharky_GriddedClimateData;

#############################################################################
# Table: staggingData
#############################################################################
DROP TABLE IF EXISTS `s_inputData`;
CREATE TABLE `s_inputData` ( 
	`SourceName` 			VARCHAR(1000) NULL,
	`DatasetName` 			VARCHAR(1000) NULL,
	`Description` 			VARCHAR(1000) NULL,
	`LayerName` 			VARCHAR(1000) NULL,
	`LayerParameter`		VARCHAR(1000) NULL,
	`Statistic`				VARCHAR(1000) NULL,
	`Levels`					VARCHAR(1000) NULL,
	`InputFile`				VARCHAR(1000) NULL,
	`OutputFile`			VARCHAR(1000) NULL,
	`OutputFileName`		VARCHAR(1000) NULL,
	`VariableOfInterest`	VARCHAR(1000) NULL,
	`OrigLocation`			VARCHAR(1000) NULL,
	`MetadataLocation`	VARCHAR(1000) NULL,
	`StartDate`				VARCHAR(1000) NULL,
	`EndDate`				VARCHAR(1000) NULL,
	`DatabaseStore`		VARCHAR(1000) NULL,
	`Units`					VARCHAR(1000) NULL,
	Blank					VARCHAR(1000) NULL
) ENGINE=MyISAM DEFAULT CHARSET=latin1;

#############################################################################
# Load Data into Stagging Table
#############################################################################

LOAD DATA INFILE '/var/lib/mysql-files/Datasets.csv'
#LOAD DATA INFILE '/media/dropbox/PhD/Reboot/Projects/GriddedDatabase/Datasets.csv' 
	INTO TABLE s_inputData 
	FIELDS TERMINATED BY ',' OPTIONALLY ENCLOSED BY '"' 
LINES TERMINATED BY '\n' IGNORE 1 LINES (
	`SourceName`,`DatasetName`,`Description`,`LayerName`,`LayerParameter`,`Statistic`,`Levels`,
	`InputFile`,`OutputFile`,`OutputFileName`,`VariableOfInterest`,`OrigLocation`,
	`MetadataLocation`,`StartDate`,`EndDate`,`DatabaseStore`, `Units`, Blank);

# SELECT * FROM s_inputData;

#############################################################################
# Table: Source
#############################################################################
DROP TABLE IF EXISTS `Source`;
CREATE TABLE `Source` (
	`Source_ID`				SMALLINT UNSIGNED NOT NULL AUTO_INCREMENT,
	`Name`					VARCHAR(100) NOT NULL,	
	PRIMARY KEY (`Source_ID`), 
	UNIQUE KEY `Name_U` (`Name`)
) ENGINE=MyISAM DEFAULT CHARSET=latin1;

INSERT IGNORE INTO `Source` (`Name`) SELECT DISTINCT SourceName FROM s_inputData;

#############################################################################
# Table: Layer
#############################################################################
DROP TABLE IF EXISTS `Layer`;
CREATE TABLE `Layer` (
	`Layer_ID`				SMALLINT UNSIGNED NOT NULL AUTO_INCREMENT, 
	`Name`					VARCHAR(100) NOT NULL,
	`Description`			VARCHAR(1000) NULL,
	`Parameter`      		VARCHAR(100) NOT NULL,
	PRIMARY KEY (`Layer_ID`), 
	UNIQUE KEY `Name_U` (`Name`)
) ENGINE=MyISAM DEFAULT CHARSET=latin1;

INSERT IGNORE INTO `Layer` (`Name`, `Parameter`) SELECT DISTINCT LayerName,  LayerParameter FROM s_inputData;

#############################################################################
# Table: Statistic
#############################################################################
DROP TABLE IF EXISTS `Statistic`;
CREATE TABLE `Statistic` (
	`Statistic_ID`			SMALLINT UNSIGNED NOT NULL AUTO_INCREMENT, 
	`Name`           		VARCHAR(100) NOT NULL,
	`Description`			VARCHAR(1000) NULL,
	PRIMARY KEY (`Statistic_ID`), 
	UNIQUE KEY `Name_U` (`Name`)
) ENGINE=MyISAM DEFAULT CHARSET=latin1;

INSERT IGNORE INTO `Statistic` (`Name`) SELECT DISTINCT Statistic FROM s_inputData;

#############################################################################
# Table: Level
#############################################################################
DROP TABLE IF EXISTS `Level`;
CREATE TABLE `Level` (
	`Level_ID`           SMALLINT UNSIGNED NOT NULL AUTO_INCREMENT, 
	`Name`           		VARCHAR(500) NOT NULL,
	`Description`			VARCHAR(2000) NULL,
	PRIMARY KEY (`Level_ID`), 
	UNIQUE KEY `Name_U` (`Name`)
) ENGINE=MyISAM DEFAULT CHARSET=latin1;

INSERT IGNORE INTO `Level` (`Name`) SELECT DISTINCT Levels FROM s_inputData;

#INSERT INTO `Level` (Name, Description) VALUES ('','');
# SELECT * FROM Level

#############################################################################
# Table: Dataset
#############################################################################
DROP TABLE IF EXISTS `Dataset`;
CREATE TABLE `Dataset` (
	`Dataset_ID`			SMALLINT UNSIGNED NOT NULL AUTO_INCREMENT,
	`Source_ID`     		SMALLINT UNSIGNED NOT NULL,
	`Layer_ID`				SMALLINT UNSIGNED NOT NULL,
	`Statistic_ID`			SMALLINT UNSIGNED NOT NULL,
	`Level_ID`				SMALLINT UNSIGNED NOT NULL,
	`Name`					VARCHAR(200) NOT NULL,
	`Description` 			VARCHAR(1000) NULL,
	`InputFile`				VARCHAR(500) NOT NULL,
	`OutputFile`			VARCHAR(500) NOT NULL,
	`OutputFileName`		VARCHAR(500) NOT NULL,
	`VariableOfInterest`	VARCHAR(20) NOT NULL,
	`OriginalLocation`	VARCHAR(500) NOT NULL,
	`MetadataLocation`	VARCHAR(500) NULL,
	`StartDate` 			DATE NOT NULL,
	`EndDate` 				DATE NOT NULL,
	`DatabaseStore`		VARCHAR(100) NOT NULL,
	`Units`				VARCHAR(100) NULL,
	`DefaultLevel`		VARCHAR(500) NULL,
	`Loaded`					TINYINT(1) NOT NULL,
	PRIMARY KEY (`Dataset_ID`), 
	UNIQUE KEY `Name_U` (`Name`),
	UNIQUE KEY `OriginalLocation_U` (`OriginalLocation`),
	UNIQUE KEY `InputFile_U` (`InputFile`),
	UNIQUE KEY `OutputFile_U` (`OutputFile`),
	UNIQUE KEY `IDs_U` (Source_ID, Layer_ID, Statistic_ID, Level_ID)
) ENGINE=MyISAM DEFAULT CHARSET=latin1;

INSERT INTO Dataset (
	`Source_ID`,
	`Layer_ID`,
	`Statistic_ID`,
	`Level_ID`,
	`Name`,
	`Description`,
	`InputFile`,
	`OutputFile`,
	`OutputFileName`,
	`VariableOfInterest`,
	`OriginalLocation`,
	`MetadataLocation`,
	`StartDate`,
	`EndDate`,
	`DatabaseStore`,
	`Units`,
	`DefaultLevel`,
	`Loaded`)
SELECT
		s.Source_ID
		, l.Layer_ID
		, stat.Statistic_ID
		, lev.Level_ID
		, i.DatasetName AS `Name`
		, i.Description
		, i.InputFile
		, i.OutputFile
		, i.OutputFileName
		, i.VariableOfInterest
		, i.OrigLocation AS `OriginalLocation`
		, i.MetadataLocation
		, STR_TO_DATE(i.StartDate, '%Y-%m-%d') AS StartDate
		, STR_TO_DATE(i.EndDate, '%Y-%m-%d') AS EndDate
		, i.DatabaseStore
		, i.Units
		, '' AS DefaultLevel
		, 0 AS Loaded
	FROM s_inputData i
	JOIN Source s ON s.Name = i.SourceName
	JOIN Layer l ON l.Name = i.LayerName
	JOIN Statistic stat ON stat.Name = i.Statistic
	JOIN `Level` lev ON lev.Name = i.Levels;
	
#SELECT * FROM Dataset;

#############################################################################
# Stored Procedure g_Dataset
# Gets all of the available datasets that have been loaded
#############################################################################
DELIMITER $$
DROP PROCEDURE IF EXISTS g_Dataset;$$
CREATE PROCEDURE g_Dataset()
BEGIN
SELECT
		Dataset_ID
		, Name
		, DatabaseStore 
		, OriginalLocation
		, StartDate
		, EndDate
		, Units
		, DefaultLevel
	FROM Dataset
	WHERE Loaded = 1
	ORDER BY Name ASC;
END$$
DELIMITER ;
