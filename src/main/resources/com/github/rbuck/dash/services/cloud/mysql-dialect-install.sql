-- comment
  -- indented comment
USE cloud;

DROP TABLE IF EXISTS object CASCADE;
DROP TABLE IF EXISTS container CASCADE;
DROP TABLE IF EXISTS container_stat CASCADE;
DROP TABLE IF EXISTS account CASCADE;
DROP TABLE IF EXISTS account_stat CASCADE;

-- create tables

CREATE TABLE account (
  id                     BIGINT    NOT NULL        AUTO_INCREMENT PRIMARY KEY,
  urn                    VARCHAR(32), -- unique

  name                   VARCHAR(32),
  metadata               VARCHAR(64)               DEFAULT '',
  created_at             TIMESTAMP                 DEFAULT CURRENT_TIMESTAMP, -- must be set to current time
  modified_at            TIMESTAMP NULL            DEFAULT NULL, -- must be set to current time
  deleted_at             TIMESTAMP NULL            DEFAULT NULL,

  description            VARCHAR(1024)             DEFAULT '',

  permissible_containers INTEGER                   DEFAULT 100
)
  ENGINE = InnoDB;

CREATE UNIQUE INDEX idx_account_urn ON account (urn);
CREATE INDEX idx_account_name ON account (name);

CREATE TABLE account_stat (
  account_id      BIGINT REFERENCES account (id), -- foreign
  container_count INTEGER DEFAULT 0,
  object_count    INTEGER DEFAULT 0,
  bytes_used      INTEGER DEFAULT 0
)
  ENGINE = InnoDB;

CREATE UNIQUE INDEX idx_account_stat_account_id ON account_stat (account_id);

CREATE TABLE container (
  id          BIGINT    NOT NULL       AUTO_INCREMENT PRIMARY KEY,
  account_id  BIGINT REFERENCES account (id), -- foreign
  rand_id     DOUBLE,

  name        VARCHAR(64), -- globally unique (contention)
  metadata    VARCHAR(64)              DEFAULT '',
  created_at  TIMESTAMP                DEFAULT CURRENT_TIMESTAMP, -- must be set to current time
  modified_at TIMESTAMP NULL           DEFAULT NULL, -- must be set to current time
  deleted_at  TIMESTAMP NULL           DEFAULT NULL
)
  ENGINE = InnoDB;

CREATE UNIQUE INDEX idx_container_name ON container (name);
CREATE INDEX idx_container_account_id ON container (account_id);
CREATE INDEX ix_container_random_id ON container (rand_id);

CREATE TABLE container_stat (
  container_id BIGINT REFERENCES container (id), -- foreign
  object_count BIGINT DEFAULT 0,
  bytes_used   BIGINT DEFAULT 0
)
  ENGINE = InnoDB;

CREATE UNIQUE INDEX idx_container_stat_container_id ON container_stat (container_id);

CREATE TABLE object (
  id           BIGINT    NOT NULL          AUTO_INCREMENT PRIMARY KEY,
  container_id BIGINT REFERENCES container (id), -- foreign
  rand_id      DOUBLE,

  name         VARCHAR(64), -- unique per container
  metadata     VARCHAR(64)                 DEFAULT '',
  created_at   TIMESTAMP                   DEFAULT CURRENT_TIMESTAMP, -- must be set to current time
  modified_at  TIMESTAMP NULL              DEFAULT NULL, -- must be set to current time
  deleted_at   TIMESTAMP NULL              DEFAULT NULL,

  size         BIGINT,

  content_type TEXT,
  etag         TEXT
)
  ENGINE = InnoDB;

CREATE INDEX ix_object_container_name ON object (container_id, name);
CREATE INDEX ix_object_random_id ON object (rand_id);

DELIMITER |
CREATE TRIGGER trg_before_insert_container
BEFORE INSERT ON container
FOR EACH ROW
  SET NEW.rand_id = rand();
|
DELIMITER ;

DELIMITER |
CREATE TRIGGER trg_before_insert_object
BEFORE INSERT ON object
FOR EACH ROW
  SET NEW.rand_id = rand();
|
DELIMITER ;

DELIMITER |
CREATE TRIGGER trg_object_update
BEFORE UPDATE ON object
FOR EACH ROW
  SIGNAL SQLSTATE '45000'
  SET MESSAGE_TEXT = 'Cannot update object row';
|
DELIMITER ;

DELIMITER |
CREATE TRIGGER trg_account_insert
AFTER INSERT ON account
FOR EACH ROW
  INSERT INTO account_stat (account_id) VALUES (NEW.id);
|
DELIMITER ;

-- good to here...

DELIMITER |
CREATE TRIGGER trg_container_insert
AFTER INSERT ON container
FOR EACH ROW
  BEGIN
    INSERT INTO container_stat (container_id) VALUES (NEW.id);
    UPDATE account_stat
    SET container_count = container_count + 1
    WHERE account_id = NEW.account_id;
  END;
|
DELIMITER ;

DELIMITER |
CREATE TRIGGER trg_object_insert
AFTER INSERT ON object
FOR EACH ROW
  BEGIN
    UPDATE container_stat
    SET
      object_count = object_count + 1,
      bytes_used   = bytes_used + NEW.size
    WHERE container_id = NEW.container_id;
  END;
|
DELIMITER ;

DELIMITER |
CREATE TRIGGER container_delete
BEFORE DELETE ON container
FOR EACH ROW
  BEGIN
    DELETE FROM container_stat
    WHERE container_id = OLD.id;
    UPDATE account_stat
    SET
      container_count = container_count - 1
    WHERE account_id = OLD.account_id;
  END;
|
DELIMITER ;

DELIMITER |
CREATE TRIGGER trg_object_delete
BEFORE DELETE ON object
FOR EACH ROW
  BEGIN
    UPDATE container_stat
    SET
      object_count = object_count - 1,
      bytes_used   = bytes_used - OLD.size
    WHERE container_id = OLD.container_id;
  END;
|
DELIMITER ;
