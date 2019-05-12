#!/bin/bash
SQLSERVER="localhost"
SQLUSERNAME="root"
SQLPASS="${MYSQLPASS}"
PWD=$(pwd)
#WORKFOLDER=$PWD
DELETE_DB_AFTER_DUMP="TRUE"
WORKFOLDER=/climateFiles
DELETEALLDB="TRUE"
JARLOCATION="/opt/mrsharky-1.0-SNAPSHOT.jar"
CLASSLOCATION="com.mrsharky.dataprocessor.NetCdfParser"
#"/climateFiles"

# mysqldump -u mrsharky_climate -ppTZjTeGg0WObPBxvjxJ6 --routines --databases mrsharky_GriddedClimateData > mrsharky_GriddedClimateData.sql
# mysqldump -u mrsharky_climate -ppTZjTeGg0WObPBxvjxJ6 --databases mrsharky_GriddedClimateData > mrsharky_GriddedClimateData.sql
# mysqldump -u mrsharky_climate -ppTZjTeGg0WObPBxvjxJ6 --databases mrsharky_noaa20v2c_Mon_press_air_mon_mean > mrsharky_noaa20v2c_Mon_press_air_mon_mean.sql
# mysql -u mrsharky_climate -p${SQLPASS} < mrsharky_GriddedClimateData.sql



if [ "$DELETEALLDB" == "TRUE" ]; then
	echo "#####################################################################"
	echo "Performing a Pre-cleanup"
	echo "#####################################################################"
	echo "Deleting:"


	# Loop through all of the databases that match the name
	while IFS=$'\t' read -r DATABASE; do
        DATABASE="$(echo -e "${DATABASE}" | tr -d '[[:space:]]')"

		echo "	${DATABASE}"
		mysql -u ${SQLUSERNAME} --password=${SQLPASS} -s -N -e "DROP DATABASE IF EXISTS ${DATABASE}"
	done <<< "$(mysql -u $SQLUSERNAME -p$SQLPASS -s -N -e "\
			show databases LIKE 'mrsharky_noaa20v2c_%';		\
	")"
	
	# Loop through all of the databases that match the name
	while IFS=$'\t' read -r DATABASE; do
        DATABASE="$(echo -e "${DATABASE}" | tr -d '[[:space:]]')"

		echo "	${DATABASE}"
		mysql -u ${SQLUSERNAME} --password=${SQLPASS} -s -N -e "DROP DATABASE IF EXISTS ${DATABASE}"
	done <<< "$(mysql -u $SQLUSERNAME -p$SQLPASS -s -N -e "\
			show databases LIKE '%noaaGpcp%';		\
	")"

fi


# Loop through all of the data returned
while IFS=$'\t' read -r DATASETNAME DOWNLOADLOCATION INPUTFILE OUTPUTFILE DATABASESTORE VARIABLEOFINTEREST OUTPUTFILENAME DATASET_ID LEVELTYPE MONTHDIFF; do

	# Remove whitespace from DATABASESTORE variable
	DATABASESTORE="$(echo -e "${DATABASESTORE}" | tr -d '[[:space:]]')"
	# Download the dataset
	DIR2CREATE=$WORKFOLDER/Data/${INPUTFILE%/*}
	mkdir -p $DIR2CREATE
	echo "#####################################################################"
	echo "Processing dataset: ${DATASETNAME}"
	echo "#####################################################################"
	echo ""
	echo "---------------------------------------------------------------------"
	echo "Downloading: ${DOWNLOADLOCATION}"
	echo "---------------------------------------------------------------------"
	echo ""
	START=$(date +%s)

	wget -c ${DOWNLOADLOCATION} -O $WORKFOLDER/Data/${INPUTFILE}

	echo ""
	END=$(date +%s)
	DIFF=$(echo "$END - $START" | bc)
	echo "Download Completed (seconds): ${DIFF}"

	echo ""
	echo "---------------------------------------------------------------------"
	echo "Creating database: ${DATABASESTORE}"
	echo "---------------------------------------------------------------------"
	echo ""
	mysql -u ${SQLUSERNAME} --password=${SQLPASS} -s -N -e "DROP DATABASE IF EXISTS ${DATABASESTORE}"
	mysql -u ${SQLUSERNAME} --password=${SQLPASS} -s -N -e "CREATE DATABASE IF NOT EXISTS ${DATABASESTORE}"

	if [ "$LEVELTYPE" == "Single" ]; then
		echo "Single level dataset"
		mysql -u ${SQLUSERNAME} --password=${SQLPASS} ${DATABASESTORE} < "SinglelevelDatabase.sql"
	elif [ "$LEVELTYPE" == "Multi" ]; then
		echo "Multi level dataset"
		mysql -u ${SQLUSERNAME} --password=${SQLPASS} ${DATABASESTORE} < "MultilevelDatabase.sql"
	else
		echo "Unknown system type"
		exit 1
	fi

	echo ""
	echo "---------------------------------------------------------------------"
	echo "Extracting netCDF data to CSV File: Data/${OUTPUTFILE}"
	echo "---------------------------------------------------------------------"
	echo ""
	START=$(date +%s)

	java -cp ${JARLOCATION} ${CLASSLOCATION} \
		-INPUT $WORKFOLDER/Data/${INPUTFILE} -OUTPUT /var/lib/mysql-files/${OUTPUTFILENAME}\
		-DATABASEURL "jdbc:mysql://${SQLSERVER}/${DATABASESTORE}"\
		-DATABASEUSERNAME "${SQLUSERNAME}" -DATABASEPASSWORD "${SQLPASS}"\
		-VARIABLEOFINTEREST "${VARIABLEOFINTEREST}" -TIMEVARIABLE "time"
	errorType=$?
	if [ "$errorType" != "0" ]; then
		echo "ERROR - Unable to extract netCDF file"
		mysql -u ${SQLUSERNAME} --password=${SQLPASS}  -s -N -e	"	\
			DROP DATABASE ${DATABASESTORE}"
		continue
	fi
	echo ""
	END=$(date +%s)
	DIFF=$(echo "$END - $START" | bc)

	echo "Extraction Completed (seconds): ${DIFF}"

	echo ""
	echo "---------------------------------------------------------------------"
	echo "Load CSV Data into DB: /var/lib/mysql-files/${OUTPUTFILENAME}"
	echo "---------------------------------------------------------------------"
	echo ""
	START=$(date +%s)

	if [ "$LEVELTYPE" == "Single" ]; then
		echo "Single level dataset"
		mysql -u ${SQLUSERNAME} --password=${SQLPASS} ${DATABASESTORE} -s -N -e	"	\
			LOAD DATA INFILE '/var/lib/mysql-files/${OUTPUTFILENAME}'				\
				INTO TABLE s_GridData												\
				FIELDS TERMINATED BY ',' OPTIONALLY ENCLOSED BY '\"' 				\
				LINES TERMINATED BY '\\n' (											\
			Level_ID,GridBox_ID,Date_ID,Value);										\
			INSERT INTO GridData (GridBox_ID,Date_ID,Value) SELECT GridBox_ID,Date_ID,Value FROM s_GridData;							\
			TRUNCATE TABLE s_GridData;												\
		"
	elif [ "$LEVELTYPE" == "Multi" ]; then
		echo "Multi level dataset"
		mysql -u ${SQLUSERNAME} --password=${SQLPASS} ${DATABASESTORE} -s -N -e	"	\
			LOAD DATA INFILE '/var/lib/mysql-files/${OUTPUTFILENAME}'				\
				INTO TABLE s_GridData												\
				FIELDS TERMINATED BY ',' OPTIONALLY ENCLOSED BY '\"' 				\
				LINES TERMINATED BY '\\n' (											\
			Level_ID,GridBox_ID,Date_ID,Value);"
		
		
		# Loop through all the different levelIDs to insert them
		while IFS=$'\t' read -r LEVEL_ID; do

			printf -v LEVEL_ID_PAD "%03d" $LEVEL_ID
		
			echo "	Processing ${LEVEL_ID_PAD}"
			mysql -u ${SQLUSERNAME} --password=${SQLPASS} ${DATABASESTORE} -s -N -e "\
				ALTER TABLE GridData ADD Value_${LEVEL_ID_PAD} FLOAT(8,5)	NULL; \
				ALTER TABLE TimeData ADD Value_${LEVEL_ID_PAD} FLOAT(8,5)	NULL; \
				INSERT INTO GridData (GridBox_ID, Date_ID, Value_${LEVEL_ID_PAD}) \
						(SELECT s.GridBox_ID, s.Date_ID, s.Value \
						FROM s_GridData s WHERE Level_ID = ${LEVEL_ID} )\
					ON DUPLICATE KEY UPDATE Value_${LEVEL_ID_PAD} = s.Value; \
				DELETE FROM s_GridData WHERE Level_ID = ${LEVEL_ID}; \
				INSERT INTO TimeData (GridBox_ID, Date_ID, Value_${LEVEL_ID_PAD}) \
						SELECT s.GridBox_ID, s.Date_ID, s.Value_${LEVEL_ID_PAD} \
						FROM GridData s ORDER BY s.GridBox_ID, s.Date_ID \
					ON DUPLICATE KEY UPDATE Value_${LEVEL_ID_PAD} = s.Value_${LEVEL_ID_PAD};" 

		done <<< "$(mysql -u $SQLUSERNAME -p$SQLPASS ${DATABASESTORE} -s -N -e "\
				SELECT DISTINCT Level_ID FROM s_GridData;	")"
		
		mysql -u ${SQLUSERNAME} --password=${SQLPASS} ${DATABASESTORE} -s -N -e "TRUNCATE TABLE s_GridData;"
	else
		echo "Unknown system type"
		exit 1
	fi

	echo ""
	END=$(date +%s)
	DIFF=$(echo "$END - $START" | bc)
	echo "Loading Completed (seconds): ${DIFF}"
	echo ""
	rm /var/lib/mysql-files/${OUTPUTFILENAME}

	echo "---------------------------------------------------------------------"
	echo "Indexing Data: ${DATABASESTORE}"
	echo "---------------------------------------------------------------------"
	echo ""
	START=$(date +%s)

	if [ "$LEVELTYPE" == "Single" ]; then
		echo "Single level dataset"
		GRIDDATA_PART=${MONTHDIFF}/10
		mysql -u ${SQLUSERNAME} --password=${SQLPASS} ${DATABASESTORE} -s -N -e "	\
			SHOW ERRORS; SHOW WARNINGS; COMMIT;										\
			OPTIMIZE TABLE GridData;"
	elif [ "$LEVELTYPE" == "Multi" ]; then
		echo "Multi level dataset"
		GRIDDATA_PART=${MONTHDIFF}
		TIMESERIES_PART=$(mysql -u ${SQLUSERNAME} --password=${SQLPASS} ${DATABASESTORE} -s -N -e "SELECT ROUND(COUNT(*)/10) FROM GridBox;")
		mysql -u ${SQLUSERNAME} --password=${SQLPASS} ${DATABASESTORE} -s -N -e "							\
			SHOW ERRORS; SHOW WARNINGS; COMMIT;																\
			OPTIMIZE TABLE GridData;
			OPTIMIZE TABLE TimeData;"
			errorType=$?
			if [ "$errorType" != "0" ]; then
				echo "ERROR occured during index step"
				break
			fi
	else
		echo "Unknown system type"
		exit 1
	fi

	echo ""
	END=$(date +%s)
	DIFF=$(echo "$END - $START" | bc)
	echo "Indexing Completed (seconds): ${DIFF}"
	echo ""

	echo "---------------------------------------------------------------------"
	echo "Cleanup"
	echo "---------------------------------------------------------------------"
	echo ""
	
	#rm Data/${INPUTFILE}

	TABLEROWS=0
	if [ "$LEVELTYPE" == "Single" ]; then
		TABLEROWS=$(mysql -u ${SQLUSERNAME} --password=${SQLPASS} ${DATABASESTORE} -s -N -e "SELECT COUNT(*) FROM GridData;")
	elif [ "$LEVELTYPE" == "Multi" ]; then
		TABLEROWS=$(mysql -u ${SQLUSERNAME} --password=${SQLPASS} ${DATABASESTORE} -s -N -e "SELECT COUNT(*) FROM GridData;")
	fi

	if [ $TABLEROWS -gt 0 ]; then
		echo "Insert into database (${DATABASESTORE}) was successful"
		mysql -u ${SQLUSERNAME} --password=${SQLPASS} mrsharky_GriddedClimateData -s -N -e "	\
			UPDATE Dataset SET Loaded = 1 WHERE Dataset_ID = ${DATASET_ID};"

		# Get the default level name (LevelID = 1)
		if [ "$LEVELTYPE" == "Multi" ]; then
			DEFAULT_LEVEL=$(mysql -u ${SQLUSERNAME} --password=${SQLPASS} ${DATABASESTORE} -s -N -e " \
				SELECT name FROM Level WHERE Level.Level_ID = 1;")
			mysql -u ${SQLUSERNAME} --password=${SQLPASS} mrsharky_GriddedClimateData -s -N -e "	\
				UPDATE Dataset SET DefaultLevel = '${DEFAULT_LEVEL}' WHERE Dataset_ID = ${DATASET_ID};"


		fi

	fi	

    echo "---------------------------------------------------------------------"
	echo "Export Tables"
	echo "---------------------------------------------------------------------"
	echo ""

    mysqldump -p${SQLPASS} --routines ${DATABASESTORE} > $WORKFOLDER/Data/${DATABASESTORE}.sql
    gzip -f $WORKFOLDER/Data/${DATABASESTORE}.sql

    if [ "$DELETE_DB_AFTER_DUMP" == "TRUE" ]; then
        echo "---------------------------------------------------------------------"
	    echo "Deleting database ${DATABASESTORE}"
	    echo "---------------------------------------------------------------------"
	    echo ""
        mysql -u ${SQLUSERNAME} --password=${SQLPASS} -s -N -e "DROP DATABASE IF EXISTS ${DATABASESTORE}"
    fi

done <<< "$(mysql -u $SQLUSERNAME -p$SQLPASS -s -N -e "\
SELECT																			\
		Name																	\
		, OriginalLocation														\
		, InputFile																\
		, OutputFile															\
		, DatabaseStore															\
		, VariableOfInterest													\
		, OutputFileName														\
		, Dataset_ID															\
		, CASE 																	\
			WHEN Name LIKE'%Single Level%' THEN 'Single'						\
			WHEN Name LIKE'%Pressure Level%' THEN 'Multi'						\
			WHEN Name LIKE'%Subsurface%' THEN 'Multi'						\
			ELSE 'Single' END AS LevelType										\
		, TIMESTAMPDIFF(MONTH, StartDate, EndDate)+1 AS MonthDiff				\
	FROM mrsharky_GriddedClimateData.Dataset 									\
	WHERE Loaded = 0 #AND Name LIKE 'NOAA 20th Century ReAnalysis (V2c)%' 	\
	ORDER BY Dataset_ID;
")"
#  Name LIKE '%Pressure Level%' 
#WHERE DatabaseStore = 'Ncp20CRA2c_Mon_mono_sprd_sbsno_mon_mean';			\


echo "Finished Loading all the requested datasets"
mysqldump -p${SQLPASS} --routines mrsharky_GriddedClimateData > $WORKFOLDER/Data/mrsharky_GriddedClimateData.sql
gzip -f $WORKFOLDER/Data/mrsharky_GriddedClimateDatasql

#echo ${DOWNLOADLOCATION}
#echo ${INPUTFILE}


#DOWNLOADLOCATION=`echo $SQLVALUES | awk '{printf $1}'`
#DOWNLOADLOCATION=`echo $SQLVALUES | awk '{printf $2}'`
#echo $DOWNLOADLOCATION
#echo $INPUTFILE




