#!/bin/bash
#
#	$Id$
#

REPORT=$1; shift
RECIP=$1; shift
PARAMS=$*

DATESTAMP=$(date +%Y%m%d)

$(dirname "$0")/report-wrapper.sh $REPORT csv $PARAMS > /tmp/$REPORT-$DATESTAMP.csv 
$(dirname "$0")/report-wrapper.sh $REPORT xls $PARAMS > /tmp/$REPORT-$DATESTAMP.xls
$(dirname "$0")/report-wrapper.sh $REPORT html $PARAMS > /tmp/$REPORT-$DATESTAMP.html

echo "Please find reports for $DATESTAMP attached." | mutt -s "Report $REPORT for $DATESTAMP" -a /tmp/$REPORT-$DATESTAMP.csv -a /tmp/$REPORT-$DATESTAMP.xls -a /tmp/$REPORT-$DATESTAMP.html $RECIP

rm -f /tmp/$REPORT-$DATESTAMP.{html,csv,xls}


