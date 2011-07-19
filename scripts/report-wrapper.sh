#!/bin/bash
#
#	$Id: report-wrapper.sh 1615 2009-06-09 16:45:52Z jbuchbinder $
#

if [ "$MYSQL" == "" ]; then
	MYSQL="mysql -uroot asteriskcdrdb "
fi

if [ $# -lt 2 ]; then
	echo "syntax: $(basename "$0") reportname (csv|xls|html) [parameters ...]"
	exit
fi

REPORT=$1; shift
FORMAT=$1; shift

NUMPARAMS=$#

function process_parameter() {
	P=$1

	# Special handling for 'NOW()'
	if [ "$P" == "NOW()" -o "$P" == "NOW" ]; then
		echo "NOW()"
		return 0;
	fi

	# Otherwise quote and exit
	echo "'$P'"
	return 0
}

case $NUMPARAMS in
	0)
	QUERY="CALL $REPORT ( );"
	;;

	1)
	QUERY="CALL $REPORT ( $(process_parameter $1) );"
	;;

	*)
	QUERY="CALL $REPORT ( $(process_parameter $1)"
	shift
	while [ "$1" != "" ]; do
		QUERY="${QUERY}, $(process_parameter $1)"
		shift
	done
	QUERY="$QUERY );"
	;;
esac

case $( echo $FORMAT | tr A-Z a-z ) in

	csv)
	$MYSQL -Be "$QUERY" | perl -pi -e 's/\t/,/g;'
	;;

	xls)
	$MYSQL -Be "$QUERY" | perl -pi -e 's/\t/,/g;' | $(dirname "$0")/csv2xls.pl /dev/stdin /dev/stdout
	;;

	html)
	$MYSQL --html -e "$QUERY" 
	;;

	*)
	echo "Format $FORMAT not supported!"
	exit
	;;

esac

