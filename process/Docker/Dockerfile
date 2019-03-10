FROM ubuntu:18.04
MAINTAINER Julien Pierret <dafrenchyman@gmail.com>

ENV MYSQL_USER=mysql \
    MYSQL_DATA_DIR=/var/lib/mysql \
    MYSQL_RUN_DIR=/run/mysqld \
    MYSQL_LOG_DIR=/var/log/mysql \
	DEBIAN_FRONTEND=noninteractive \
	MYSQL_UID=1000 \
	MYSQL_GID=1000


# Create mysql group and user 
RUN groupadd -g "${MYSQL_GID}" -r mysql
RUN useradd -g "${MYSQL_GID}" -u "${MYSQL_UID}" -M -r mysql

RUN apt-get update

###########################################################
# Nano (makes debugging easier in container)
###########################################################
RUN apt-get install nano -y

###########################################################
# Maria DB Setup
###########################################################
RUN apt-get install mariadb-server mariadb-client -y

# Extra stuff
RUN rm -rf /var/lib/apt/lists/*
#RUN sed -i 's/^\(bind-address\s.*\)/# \1/' /etc/mysql/my.cnf
RUN sed -i -e"s/^bind-address\s*=\s*127.0.0.1/#bind-address = 0.0.0.0/" /etc/mysql/mariadb.conf.d/50-server.cnf

# Add the startup script that adds in remote access to the server
ADD files/startup.sh /opt/startup.sh


###########################################################
# Install add-apt-repository
###########################################################
RUN apt-get -qqy update
RUN apt-get -qqy install software-properties-common

###########################################################
# Java
###########################################################
RUN add-apt-repository -y ppa:webupd8team/java
RUN apt-get -qqy update
RUN echo oracle-java8-installer shared/accepted-oracle-license-v1-1 select true | /usr/bin/debconf-set-selections
RUN apt-get -qqy install oracle-java8-installer
RUN apt-get -qqy install oracle-java8-set-default

###########################################################
# Maven and git
###########################################################
RUN apt-get -y install maven git libblas-dev liblapack-dev libgfortran3 gfortran
RUN mkdir -p /git
RUN cd /git && git clone https://github.com/dafrenchyman/mrsharky.git
RUN cd /git/mrsharky && mvn clean install
RUN cp /git/mrsharky/target/mrsharky-1.0-SNAPSHOT.jar /opt/mrsharky-1.0-SNAPSHOT.jar

##########################################################
# Climate files
##########################################################

# bc command
RUN apt-get install bc -y

RUN mkdir /climateFiles
ADD files/startup.sh /opt/startup.sh

ADD files/Datasets.csv              /opt/Datasets.csv
ADD files/LoadDatasets.sh           /opt/LoadDatasets.sh
ADD files/CreateMasterDb.sh         /opt/CreateMasterDb.sh
ADD files/MasterDatabase.sql        /opt/MasterDatabase.sql
ADD files/MultilevelDatabase.sql    /opt/MultilevelDatabase.sql
ADD files/SinglelevelDatabase.sql   /opt/SinglelevelDatabase.sql

RUN chmod +x /opt/CreateMasterDb.sh
RUN chmod +x /opt/LoadDatasets.sh
RUN chmod +x /opt/startup.sh


RUN mkdir /var/lib/mysql-files

# Add directory we're we can copy files


EXPOSE 3306
EXPOSE 80
EXPOSE 22
VOLUME ["${MYSQL_DATA_DIR}", "/var/lib/mysql-files", "/climateFiles"]




CMD ["/opt/startup.sh"]
