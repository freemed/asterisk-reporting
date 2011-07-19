#--------------------------------------------------------------------------
#
#	$Id$
#
#--------------------------------------------------------------------------

USE asteriskcdrdb;

#--------------------------------------------------------------------------

DROP TABLE IF EXISTS salespeople;

CREATE TABLE salespeople (
	  extension		INT UNSIGNED NOT NULL
	, name			VARCHAR (255) NOT NULL
	, short_name		VARCHAR (30) NOT NULL
	, email			VARCHAR (50) NOT NULL
	, display		TINYINT NOT NULL DEFAULT 1

	, INDEX ( extension )
);

INSERT INTO salespeople VALUES
	  ( 200, 'Sales Guy 1', 'Adam', 'adam@example.com', 1 )
	, ( 201, 'Sales Guy 2', 'Bob', 'bob@example.com', 1 )
	, ( 202, 'Sales Guy 3', 'Charlie', 'charlie@example.com', 1 )
;

#--------------------------------------------------------------------------

DROP TABLE IF EXISTS internal_numbers;

CREATE TABLE internal_numbers (
	  phone			VARCHAR (16) NOT NULL
	, description		VARCHAR (255)
);

INSERT INTO internal_numbers VALUES
	  ( '8888146754', 'Webex' )
	, ( '18888146754', 'Webex' )
;

#--------------------------------------------------------------------------

DROP TABLE IF EXISTS salescalls_outbound;
DROP TABLE IF EXISTS salescalls_inbound;

CREATE TABLE salescalls_outbound (
	  extension		INT UNSIGNED NOT NULL
	, destination		VARCHAR (20) NOT NULL
	, duration		INT(11) UNSIGNED NOT NULL
	, stamp			DATETIME NOT NULL
	, uniqueid		VARCHAR (32) NOT NULL UNIQUE

	, INDEX ( extension, stamp )
	, INDEX ( uniqueid )
);

CREATE TABLE salescalls_inbound (
	  extension		INT UNSIGNED NOT NULL
	, origination		VARCHAR (20) NOT NULL
	, duration		INT(11) UNSIGNED NOT NULL
	, stamp			DATETIME NOT NULL
	, uniqueid		VARCHAR (32) NOT NULL UNIQUE

	, INDEX ( extension, stamp )
	, INDEX ( uniqueid )
);

#--------------------------------------------------------------------------

DROP PROCEDURE IF EXISTS cdrEtlSalesCalls;

DELIMITER //

CREATE PROCEDURE cdrEtlSalesCalls ( )
BEGIN
	DECLARE done BOOL DEFAULT FALSE;
	DECLARE tCalldate DATETIME;
	DECLARE tSrc VARCHAR ( 30 );
	DECLARE tDst VARCHAR ( 30 );
	DECLARE tDuration INT UNSIGNED;
	DECLARE tUniqueId VARCHAR ( 32 );

	DECLARE curOut CURSOR FOR
		SELECT calldate, CASE LENGTH(src) WHEN 3 THEN src ELSE LEFT( REPLACE(REPLACE(channel, 'SIP/', ''), 'IAX/', ''), 3) END AS src, dst, duration, uniqueid FROM cdr WHERE LENGTH( dst ) >= 10 AND uniqueid NOT IN ( SELECT uniqueid FROM salescalls_outbound ) HAVING src IN ( SELECT extension FROM salespeople ) ORDER BY calldate;
	DECLARE curIn CURSOR FOR
		SELECT calldate, src, dst, duration, uniqueid FROM cdr WHERE dst IN ( SELECT extension FROM salespeople ) AND LENGTH( src ) >= 10 AND dcontext != 'from-internal' AND uniqueid NOT IN ( SELECT uniqueid FROM salescalls_inbound ) ORDER BY calldate;

	DECLARE CONTINUE HANDLER FOR SQLSTATE '02000' SET done = TRUE;
	DECLARE CONTINUE HANDLER FOR SQLSTATE '23000' BEGIN END;

	OPEN curOut;
	WHILE NOT done DO
		FETCH curOut INTO tCalldate, tSrc, tDst, tDuration, tUniqueId;

		# Handle leading ones.
		IF LENGTH( tDst ) = 11 AND LEFT( tDst, 1 ) = '1' THEN
			SELECT RIGHT(tDst, 10) INTO tDst;
		END IF;
		IF NOT ISNULL( tSrc ) THEN
			INSERT INTO salescalls_outbound
				( extension, destination, duration, stamp, uniqueid )
				VALUES
				( tSrc, tDst, tDuration, tCallDate, tUniqueId )
			;
		END IF;
	END WHILE;
	CLOSE curOut;

	SET done = FALSE;
	SET tCallDate = NULL;
	SET tSrc = NULL;
	SET tDst = NULL;
	SET tDuration = NULL;
	SET tUniqueId = NULL;

	OPEN curIn;
	WHILE NOT done DO
		FETCH curIn INTO tCalldate, tSrc, tDst, tDuration, tUniqueId;

		# Handle leading ones.
		IF LENGTH( tSrc ) = 11 AND LEFT( tSrc, 1 ) = '1' THEN
			SELECT RIGHT(tSrc, 10) INTO tSrc;
		END IF;
		IF NOT ISNULL( tDst ) THEN
			INSERT INTO salescalls_inbound
				( extension, origination, duration, stamp, uniqueid )
				VALUES
				( tDst, tSrc, tDuration, tCallDate, tUniqueId )
			;
		END IF;
	END WHILE;
	CLOSE curIn;

	# Remove any internal numbers
	DELETE FROM salescalls_inbound WHERE origination IN ( SELECT phone FROM internal_numbers );

END//

DELIMITER ;

#--------------------------------------------------------------------------

# Rebuild index on first run
CALL cdrEtlSalesCalls();

#--------------------------------------------------------------------------

