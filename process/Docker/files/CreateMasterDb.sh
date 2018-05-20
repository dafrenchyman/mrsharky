#!/bin/bash
SQLSERVER="localhost"
SQLUSERNAME="root"
SQLPASS="${MYSQLPASS}"
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
