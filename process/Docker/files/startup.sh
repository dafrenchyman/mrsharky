#!/bin/bash

#!/bin/sh

# Set mysql account's UID.
usermod -u $MYSQL_UID -g $MYSQL_GID --non-unique mysql > /dev/null 2>&1

OLD_UID="$(id -u mysql)" # Users UID
OLD_GID="$(id -g mysql)" # Users GID

# change the UID and GID of mysql
usermod -u $MYSQL_UID mysql
groupmod -g $MYSQL_GID mysql

# change the UID and GID of all the files
find / -group $OLD_GID -exec chgrp -h mysql {} \;
find / -user $OLD_UID -exec chown -h mysql {} \;

# Change ownership to mysql account on all working folders.
chown -R $MYSQL_UID:$MYSQL_GID /var/lib/mysql
chown -R $MYSQL_UID:$MYSQL_GID /var/lib/mysql-files
chown -R $MYSQL_UID:$MYSQL_GID /var/lib/mysql-keyring
chown -R $MYSQL_UID:$MYSQL_GID /climateFiles

export MYSQLPASS=${MYSQLPASS}

# Change permissions on Dropbox folder
#chmod 755 /dbox/Dropbox

if [ ! -f /var/lib/mysql/ibdata1 ]; then
    echo "Setting up first run"
    mysql_install_db 

	mysqld --initialize-insecure

	/usr/bin/mysqld_safe &
	sleep 10s

    mysql -e "SET PASSWORD FOR 'root'@'localhost' = PASSWORD('${MYSQLPASS}');"
	#mysql -s -N -e "ALTER USER 'root'@'localhost' IDENTIFIED BY '${MYSQLPASS}';"
	mysql --password=${MYSQLPASS} -e  "GRANT ALL PRIVILEGES ON *.* TO 'root'@'%' IDENTIFIED BY '${MYSQLPASS}' WITH GRANT OPTION; FLUSH PRIVILEGES;"
    mysql --password=${MYSQLPASS} -e  "GRANT ALL PRIVILEGES ON *.* TO 'root'@'localhost' IDENTIFIED BY '${MYSQLPASS}' WITH GRANT OPTION; FLUSH PRIVILEGES;"
	killall mysqld_safe
	sleep 10s

fi

# Start Apache
#source /etc/apache2/envvars
#/usr/sbin/apache2 -DFOREGROUND &

# Start MySql
mysqld_safe &
sleep 10s

# Start the process
cd /opt
/opt/CreateMasterDb.sh
/opt/LoadDatasets.sh


