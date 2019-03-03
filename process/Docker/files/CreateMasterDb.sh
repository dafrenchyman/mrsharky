#!/bin/bash
SQLSERVER="localhost"
SQLUSERNAME="root"
SQLPASS="${MYSQLPASS}"
WORKFOLDER=/climateFiles
PWD=$(pwd)

echo "#####################################################################"
echo "Creating GriddedClimateData database"
echo "#####################################################################"
echo ""
echo "	Populating the database"
cp /opt/Datasets.csv /var/lib/mysql-files/Datasets.csv
chmod 777 /var/lib/mysql-files/Datasets.csv
mysql -u $SQLUSERNAME --password=$SQLPASS < "MasterDatabase.sql"
rm /var/lib/mysql-files/Datasets.csv
echo "	Populating the database - DONE"
echo ""


echo "Dump the main lookup database to disk"
mkdir -p $WORKFOLDER/Data
mysqldump -p${SQLPASS} --routines mrsharky_GriddedClimateData > $WORKFOLDER/Data/mrsharky_GriddedClimateData.sql
gzip $WORKFOLDER/Data/mrsharky_GriddedClimateData.sql
