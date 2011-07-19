#!/bin/bash
#
#	$Id: salescdr.sh 1977 2010-03-22 19:09:12Z jbuchbinder $
#

DESTINATIONEMAIL=$1

TEMPDIR=/tmp/$$
mkdir -p "$TEMPDIR"

MYSQL="mysql -uroot asteriskcdrdb -Be "

$MYSQL "SELECT extension, name, email FROM salespeople WHERE display = 1" -N | sed -e 's/\t/|/g;' | while read E; do
	EXT=$( echo "$E" | cut -d'|' -f1 )
	NAME=$( echo "$E" | cut -d'|' -f2 )
	EMAIL=$( echo "$E" | cut -d'|' -f3 )

	echo $E | logger -t salescdr-weekly

	$MYSQL "CALL rptWeeklySalesCallTotals('$EXT', NOW());" | sed 's/\t/","/g;s/^/"/;s/$/"/;' > $TEMPDIR/$EXT.csv
	cat $TEMPDIR/$EXT.csv | mutt -s "Weekly Call Stats - $NAME" -a $TEMPDIR/$EXT.csv $DESTINATIONEMAIL, $EMAIL
done

rm -rf "$TEMPDIR"

