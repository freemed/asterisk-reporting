#--------------------------------------------------------------------------
#
#	$Id: reports.sql 1508 2009-04-23 16:09:54Z jbuchbinder $
#
#	PBX Reports
#
#--------------------------------------------------------------------------

USE asteriskcdrdb;

#--------------------------------------------------------------------------

DROP PROCEDURE IF EXISTS rptInboundSalesCalls;

DELIMITER //

CREATE PROCEDURE rptInboundSalesCalls (
		  IN pDateBegin DATE
		, IN pDateEnd DATE
	)
BEGIN
	SELECT
		  p.name AS 'Name'
		, p.extension AS 'Extension'
		, ci.origination AS 'Inbound Number'
		, ROUND(ci.duration / 60, 1) AS 'Duration (minutes)'
		, ci.stamp AS 'Date/Time'
	FROM
		salescalls_inbound ci
		LEFT OUTER JOIN salespeople p ON p.extension = ci.extension
	WHERE
		    CAST(ci.stamp AS DATE) >= pDateBegin
		AND CAST(ci.stamp AS DATE) <= pDateEnd
	ORDER BY
		  p.name
		, ci.stamp
	;
END//

DELIMITER ;

#--------------------------------------------------------------------------

DROP PROCEDURE IF EXISTS rptOutboundWeeklySalesCallTotals;

DELIMITER //

CREATE PROCEDURE rptOutboundWeeklySalesCallTotals (
		  IN pDate DATE
	)
BEGIN
	SELECT
		  p.name AS 'Name'
		, p.extension AS 'Extension'
		, CAST(co.stamp AS DATE) AS 'Date'
		, COUNT(co.destination) AS '# of Calls'
		, ROUND( ( SUM(co.duration) / COUNT(co.destination) ) / 60, 1) AS 'Avg Length (minutes)'
		, ROUND( MAX(co.duration) / 60, 1 ) AS 'Longest (min)'
	FROM
		salescalls_outbound co
		LEFT OUTER JOIN salespeople p ON p.extension = co.extension
	WHERE
		    CAST(co.stamp AS DATE) >= DATE_SUB(pDate, INTERVAL 6 DAY)
		AND CAST(co.stamp AS DATE) <= pDate
		AND p.display = 1
	GROUP BY
		  p.extension
		, CAST(co.stamp AS DATE)
	ORDER BY
		  p.name
		, CAST(co.stamp AS DATE)
	;
END//

DELIMITER ;

#--------------------------------------------------------------------------

DROP PROCEDURE IF EXISTS rptDailySalesCallsPerExtension;

DELIMITER //

CREATE PROCEDURE rptDailySalesCallsPerExtension (
		  IN pExtension INT UNSIGNED
		, IN pDate DATE
	)
BEGIN
	SELECT
		COUNT(*) AS 'Number of Calls'
	FROM 
		salescalls_outbound co
		LEFT OUTER JOIN salespeople p ON p.extension = co.extension
	WHERE
		    p.extension = pExtension
		AND CAST(co.stamp AS DATE) = pDate;

	SELECT
		  p.name AS 'Name'
		, p.extension AS 'Extension'
		, CAST(co.stamp AS DATE) AS 'Call Date'
		, co.destination AS 'Destination'
		, ROUND( co.duration / 60, 2 ) AS 'Duration'
	FROM
		salescalls_outbound co
		LEFT OUTER JOIN salespeople p ON p.extension = co.extension
	WHERE
		    p.extension = pExtension
		AND CAST(co.stamp AS DATE) = pDate;
END//

DELIMITER ;

#--------------------------------------------------------------------------

DROP PROCEDURE IF EXISTS rptDailySalesCallsInAndOutPerExtension;

DELIMITER //

CREATE PROCEDURE rptDailySalesCallsInAndOutPerExtension (
		  IN pExtension INT UNSIGNED
		, IN pDateStart DATE
		, IN pDateEnd DATE
	)
BEGIN
	DROP TEMPORARY TABLE IF EXISTS tmp_CallInOut;

	CREATE TEMPORARY TABLE tmp_CallInOut (
		  `Name`			VARCHAR(100)
		, `Direction`			ENUM ( 'Inbound', 'Outbound' ) NOT NULL
		, `Extension`			INT UNSIGNED
		, `Date/Time`			TIMESTAMP
		, `Other Party`			VARCHAR(30)
		, `Duration (minutes)`		DECIMAL(10, 2)
	);

	INSERT INTO tmp_CallInOut
	SELECT
		  p.name AS 'Name'
		, 'Outbound' AS 'Direction'
		, p.extension AS 'Extension'
		, co.stamp AS 'Date/Time'
		, co.destination AS 'Other Party'
		, ROUND( co.duration / 60, 2 ) AS 'Duration (minutes)'
	FROM
		salescalls_outbound co
		LEFT OUTER JOIN salespeople p ON p.extension = co.extension
	WHERE
		    p.extension = pExtension
		AND ( 
			    CAST(co.stamp AS DATE) >= pDateStart
			AND CAST(co.stamp AS DATE) <= pDateEnd
		);

	INSERT INTO tmp_CallInOut
	SELECT
		  p.name AS 'Name'
		, 'Inbound' AS 'Direction'
		, p.extension AS 'Extension'
		, ci.stamp AS 'Date/Time'
		, ci.origination AS 'Other Party'
		, ROUND(ci.duration / 60, 1) AS 'Duration (minutes)'
	FROM
		salescalls_inbound ci
		LEFT OUTER JOIN salespeople p ON p.extension = ci.extension
	WHERE
		    p.extension = pExtension
		AND ( 
			    CAST(ci.stamp AS DATE) >= pDateStart
			AND CAST(ci.stamp AS DATE) <= pDateEnd
		)
	;

	SELECT
		 Direction
		, COUNT(*) AS 'Number of Calls'
	FROM 
		tmp_CallInOut
	GROUP BY Direction WITH ROLLUP;

	SELECT
		*
	FROM
		tmp_CallInOut
	ORDER BY
		`Date/Time`
	;

	DROP TEMPORARY TABLE IF EXISTS tmp_CallInOut;
END//

DELIMITER ;

#--------------------------------------------------------------------------

DROP PROCEDURE IF EXISTS rptFullCdrRecordForExtension;

DELIMITER //

CREATE PROCEDURE rptFullCdrRecordForExtension (
		  IN pExtension INT UNSIGNED
	)
BEGIN
	SELECT
		*
	FROM cdr
	WHERE
		( src = pExtension OR dst = pExtension )
		AND src NOT IN ( '*97', '*98' )
		AND dst NOT IN ( '*97', '*98' )
	ORDER BY calldate;
END//

DELIMITER ;

#--------------------------------------------------------------------------

DROP PROCEDURE IF EXISTS rptWeeklySalesCallTotals;

DELIMITER //

CREATE PROCEDURE rptWeeklySalesCallTotals (
		  IN ext INT UNSIGNED
		, IN pDate DATE
	)
BEGIN
	CREATE TEMPORARY TABLE tmpWeeklyAgg (
		  extension	INT UNSIGNED
		, direction	ENUM( 'IN', 'OUT' ) NOT NULL
		, destination	VARCHAR (20) NOT NULL
		, duration	INT UNSIGNED
		, stamp		TIMESTAMP
	);

	INSERT INTO tmpWeeklyAgg
		SELECT extension, 'IN', origination, duration, stamp FROM salescalls_inbound
		WHERE extension = ext AND DATE(stamp) >= DATE_SUB(NOW(), INTERVAL 5 DAY);
	INSERT INTO tmpWeeklyAgg
		SELECT extension, 'OUT', destination, duration, stamp FROM salescalls_outbound
		WHERE extension = ext AND DATE(stamp) >= DATE_SUB(NOW(), INTERVAL 5 DAY);

	# Show summary of calls
	SELECT
		  p.name AS 'Source'
		, DATE(stamp) AS 'Call Date'
		, COUNT(DATE(stamp)) AS 'Number of Calls'
	FROM
		tmpWeeklyAgg t
		LEFT OUTER JOIN salespeople p ON t.extension = p.extension
	GROUP BY 
		DATE(stamp)
	ORDER BY DATE(stamp)
	;

	# Show list of calls
	SELECT
		  p.name AS 'Source'
		, DATE(stamp) AS 'Call Date'
		, direction AS 'In/Out'
		, destination AS 'Destination'
		, ROUND(duration/60,2) AS 'Duration'
	FROM
		tmpWeeklyAgg t
		LEFT OUTER JOIN salespeople p ON t.extension = p.extension
	ORDER BY stamp
	;

	DROP TEMPORARY TABLE tmpWeeklyAgg;
END//

DELIMITER ;

