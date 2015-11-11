DROP TABLE t5_message;
CREATE TABLE t5_message (
    id              SERIAL,
    time            TIMESTAMP,
    content         XML,
    PRIMARY KEY (id)
);

DROP TABLE t5_observation;
CREATE TABLE t5_observation (
    id              SERIAL,
    message_id      INTEGER REFERENCES t5_message (id),
    uid             VARCHAR (64),
    setid           VARCHAR (64),
    start_time      TIMESTAMP,
    end_time        TIMESTAMP,
    value           VARCHAR (10000),
    code            VARCHAR (64),
    code_system     VARCHAR (64),
    unit            VARCHAR (64),
    unit_system     VARCHAR (64),
    sample_Rate     VARCHAR (64),
    date_range      VARCHAR (64),
    PRIMARY KEY (id)
);

DROP TABLE t5_device;
CREATE TABLE t5_device (
    id              SERIAL,
    device_id       VARCHAR (64),
    observation_id  INTEGER REFERENCES t5_observation (id),
    PRIMARY KEY (id)
);