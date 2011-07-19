#!/bin/bash
#
#	ttscall.sh
#

NUMBER=$1
shift
PHRASE=$*

FILENAME=$( date +%s )

cat <<EOF > /var/spool/asterisk/${FILENAME}
Channel: Local/${NUMBER}@from-internal
Callerid: 5088482484
MaxRetries: 1
RetryTime: 300
WaitTime: 50
Priority: 1
Application: AGI
Data: festival-script.pl|$PHRASE
EOF
chown asterisk:asterisk  /var/spool/asterisk/${FILENAME}
mv /var/spool/asterisk/${FILENAME} /var/spool/asterisk/outgoing

