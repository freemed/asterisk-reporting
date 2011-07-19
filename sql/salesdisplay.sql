#--------------------------------------------------------------------------
#
#	$Id: salesdisplay.sql 1515 2009-04-24 16:16:16Z jbuchbinder $
#
#--------------------------------------------------------------------------

USE asteriskcdrdb;

#--------------------------------------------------------------------------

DROP PROCEDURE IF EXISTS getSalesExtensions;
DROP PROCEDURE IF EXISTS spGetSalesExtensions;

DELIMITER //

CREATE PROCEDURE spGetSalesExtensions ( )
BEGIN
	SELECT
		  extension
		, name
		, short_name
	FROM 
		salespeople
	WHERE
		display = 1
	ORDER BY extension ;
END//

DELIMITER ;

#--------------------------------------------------------------------------

DROP PROCEDURE IF EXISTS spGetSalesStats;

DELIMITER //

CREATE PROCEDURE spGetSalesStats ( )
BEGIN
	DECLARE done BOOL DEFAULT FALSE;
	DECLARE ex INT UNSIGNED;

	DECLARE tsHourAgo DATETIME;
	DECLARE tsDayAgo DATETIME;
	DECLARE tsWeekAgo DATETIME;

	DECLARE tCalls INT UNSIGNED DEFAULT 0;
	DECLARE tLongCalls INT UNSIGNED DEFAULT 0;
	DECLARE tAvgLength DECIMAL( 5, 2) DEFAULT 0;

	DECLARE cur CURSOR FOR
		SELECT extension FROM salespeople WHERE display = 1;

	DECLARE CONTINUE HANDLER FOR SQLSTATE '02000' SET done = TRUE;
	DECLARE CONTINUE HANDLER FOR SQLSTATE '23000' BEGIN END;

	# Build timestamps
	SELECT DATE_SUB( NOW(), INTERVAL 1 HOUR ) INTO tsHourAgo;
	SELECT CAST( CAST( NOW() AS DATE ) AS DATETIME ) INTO tsDayAgo;
	SELECT DATE_SUB( CAST( NOW() AS DATE ), INTERVAL 7 DAY ) INTO tsWeekAgo;

	CREATE TEMPORARY TABLE tmpSalesStats (
		  extension		INT UNSIGNED NOT NULL

		, calls_hour		INT UNSIGNED DEFAULT 0
		, longcalls_hour	INT UNSIGNED DEFAULT 0
		, avglength_hour	DECIMAL( 5, 2 ) UNSIGNED DEFAULT 0.0

		, calls_day		INT UNSIGNED DEFAULT 0
		, longcalls_day		INT UNSIGNED DEFAULT 0
		, avglength_day		DECIMAL( 5, 2 ) UNSIGNED DEFAULT 0.0

		, calls_week		INT UNSIGNED DEFAULT 0
		, longcalls_week	INT UNSIGNED DEFAULT 0
		, avglength_week	DECIMAL( 5, 2 ) UNSIGNED DEFAULT 0.0
	);

	INSERT INTO tmpSalesStats ( extension )
		SELECT extension FROM salespeople WHERE display = 1;

	OPEN cur;
	WHILE NOT done DO
		FETCH cur INTO ex;
		
		SELECT COUNT(uniqueid) INTO tCalls
			FROM salescalls_outbound
			WHERE extension = ex AND stamp >= tsHourAgo ;
		SELECT ( SUM(duration) / COUNT(uniqueid) ) / 60 INTO tAvgLength
			FROM salescalls_outbound
			WHERE extension = ex AND stamp >= tsHourAgo ;
		SELECT COUNT(uniqueid) INTO tLongCalls
			FROM salescalls_outbound
			WHERE extension = ex AND stamp >= tsHourAgo AND duration >= 300 ;

		UPDATE tmpSalesStats SET
			  calls_hour = tCalls
			, longcalls_hour = tLongCalls
			, avglength_hour = tAvgLength
			WHERE extension = ex;

		SELECT COUNT(uniqueid) INTO tCalls
			FROM salescalls_outbound
			WHERE extension = ex AND stamp >= tsDayAgo ;
		SELECT ( SUM(duration) / COUNT(uniqueid) ) / 60 INTO tAvgLength
			FROM salescalls_outbound
			WHERE extension = ex AND stamp >= tsDayAgo ;
		SELECT COUNT(uniqueid) INTO tLongCalls
			FROM salescalls_outbound
			WHERE extension = ex AND stamp >= tsDayAgo AND duration >= 300 ;

		UPDATE tmpSalesStats SET
			  calls_day = tCalls
			, longcalls_day = tLongCalls
			, avglength_day = tAvgLength
			WHERE extension = ex;

		SELECT COUNT(uniqueid) INTO tCalls
			FROM salescalls_outbound
			WHERE extension = ex AND stamp >= tsWeekAgo ;
		SELECT ( SUM(duration) / COUNT(uniqueid) ) / 60 INTO tAvgLength
			FROM salescalls_outbound
			WHERE extension = ex AND stamp >= tsWeekAgo ;
		SELECT COUNT(uniqueid) INTO tLongCalls
			FROM salescalls_outbound
			WHERE extension = ex AND stamp >= tsWeekAgo AND duration >= 300 ;

		UPDATE tmpSalesStats SET
			  calls_week = ( tCalls / 5 )
			, longcalls_week = ( tLongCalls / 5 )
			, avglength_week = tAvgLength
			WHERE extension = ex;
	END WHILE;
	CLOSE cur;

	SELECT * FROM tmpSalesStats ORDER BY extension;

	DROP TEMPORARY TABLE tmpSalesStats;
END//

DELIMITER ;

#--------------------------------------------------------------------------

