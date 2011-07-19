#!/usr/bin/perl
#
#	$Id: salesforce-graph.pl 1532 2009-05-01 19:31:42Z jbuchbinder $
#

use FindBin qw($Bin);
use WWW::Mechanize;
use Config::Properties;
use File::Temp qw(tempfile);

my $m = WWW::Mechanize->new();

my $configpath = "$Bin";
if ( ! -r "$configpath/salesdisplay.properties" ) {
	$configpath = "$Bin/..";
}

open PROPS, "< $configpath/salesdisplay.properties" or die "unable to open configuration file";

my $config = new Config::Properties();
$config->load( *PROPS );

# Login to Salesforce
$m->get( $config->getProperty( 'salesforce.url.login' ) );
$m->submit_form(
	form_number => 1,
	fields => {
		  un => $config->getProperty( 'salesforce.username' )
		, pw => $config->getProperty( 'salesforce.password' )
	}
);

# Get Dashboard
$m->get( $config->getProperty( 'salesforce.url.dashboard' ) );
if ( $m->content =~ /img src="(\/servlet\/servlet.ChartServer[^"]+)"/ ) {
	$m->get ( 'http://na1.salesforce.com' . URLDecode( $1 ) );
	pushToTempFile( $m->content );
}
close PROPS;

sub pushToTempFile {
	my $content = $_;
###	my ($fh, $filename) = tempfile(DIR => "/tmp");
	my $filename = "/tmp/test";
	open TEMP, ">$filename" or die "Could not write to temporary file!\n";
	print TEMP $content;
	close TEMP;
	print $filename . "\n";
}

sub URLDecode {
	my $theURL = $_[0];
	$theURL =~ tr/+/ /;
	$theURL =~ s/&amp;/&/g;
	$theURL =~ s/%([a-fA-F0-9]{2,2})/chr(hex($1))/eg;
	$theURL =~ s/<!–(.|\n)*–>//g;
	return $theURL;
}
