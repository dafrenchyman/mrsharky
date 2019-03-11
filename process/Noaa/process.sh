#!/bin/bash


JARLOCATION="/opt/mrsharky-1.0-SNAPSHOT.jar"
CLASSLOCATION="com.mrsharky.dataprocessor.NoaaGlobalTempGriddedAscParser"

gunzip -k NOAAGlobalTemp.gridded.v4.0.1.201605.asc.gz

java -cp ${JARLOCATION} ${CLASSLOCATION} \
		-INPUT ./NOAAGlobalTemp.gridded.v4.0.1.201605.asc \
        -OUTPUT ./NOAAGlobalTemp.gridded.v4.0.1.201605.csv

