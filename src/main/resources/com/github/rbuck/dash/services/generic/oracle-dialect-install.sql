
DROP DATABASE IF EXISTS demo;
CREATE DATABASE IF NOT EXISTS demo;
USE demo;

DROP PROCEDURE IF EXISTS generate_data;
DROP PROCEDURE IF EXISTS generate_100m;
DROP TABLE IF EXISTS test CASCADE;

-- create tables

CREATE TABLE test (
  id         BIGINT        DEFAULT NULL,
  created_at TIMESTAMP     DEFAULT CURRENT_TIMESTAMP, -- must be set to current time
  a          VARCHAR(32)   DEFAULT '',
  b          VARCHAR(64)   DEFAULT '',
  c          VARCHAR(128)  DEFAULT '',
  d          VARCHAR(256)  DEFAULT '',
  e          VARCHAR(512)  DEFAULT '',
  f          VARCHAR(1024) DEFAULT ''
);

DELIMITER $$
CREATE PROCEDURE generate_data()
  BEGIN
    DECLARE i INT DEFAULT 0;
    WHILE i < 1000 DO
      INSERT INTO `test` (`id`) VALUES (RAND());
      SET i = i + 1;
    END WHILE;
  END$$
DELIMITER ;

DELIMITER $$
CREATE PROCEDURE generate_100m()
  BEGIN
    DECLARE i INT DEFAULT 0;
    WHILE i < 100000 DO
      CALL generate_data();
      SET i = i + 1;
    END WHILE;
  END$$
DELIMITER ;

#CALL generate_100m();