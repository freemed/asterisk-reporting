#!/bin/bash
#
#	$Id: wrapper.sh 1566 2009-05-18 19:57:45Z jbuchbinder $
#

#
# Tomcat wrapper script for SalesDisplay application
#

WHEREIAM=$( cd "$(dirname "$0")" ; pwd )
SEARCH_PATH=(
	/usr/lib/jvm/java-6-sun
	/usr/java/latest
)
JAVA_HOME=""
for S in ${SEARCH_PATH[@]}; do
	if [ "$JAVA_HOME" == "" -a -x "$S/bin/javac" ]; then
		echo "Found JAVA_HOME in $S"
		export JAVA_HOME="$S"
	fi
done
export JAVA_OPTS=" -Dhome=${WHEREIAM} -Dproperties=${WHEREIAM}/salesdisplay.properties "

${WHEREIAM}/bin/catalina.sh $*

