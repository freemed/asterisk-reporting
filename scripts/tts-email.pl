#!/usr/bin/perl
#
#	$Id$
#
#	Consume email messages and push into ttscall.sh script
#

my $number = "";
my $message = "";

while(<>) {
	my $line = $_;
	if ( $line =~ m/^PhoneNumber: ([0-9\-]+)/ ) {
		$number = $1;
	}
	if ( $line =~ m/^Message: (.*)$/ ) {
		$message = $1;
	}
}

`echo sudo /usr/local/bin/ttscall.sh "$number" "$message" | logger -t $0`;
`sudo /usr/local/bin/ttscall.sh "$number" "$message"`;

