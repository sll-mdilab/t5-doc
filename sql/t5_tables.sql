TRUNCATE t5_device, t5_observation, t5_message;

DROP TABLE t5_device;
DROP TABLE t5_observation;
DROP TABLE t5_message;

CREATE TABLE t5_message (
    id              SERIAL,
    time            TIMESTAMPTZ,
    content         XML,
    PRIMARY KEY (id)
);

CREATE TABLE t5_observation (
    id              SERIAL,
    message_id      INTEGER REFERENCES t5_message (id),
    uid             VARCHAR (64),
    set_id          VARCHAR (64),
    start_time      TIMESTAMPTZ,
    end_time        TIMESTAMPTZ,
    value           VARCHAR (10000),
    value_type      VARCHAR (4),
    code            VARCHAR (64),
    code_system     VARCHAR (64),
    unit            VARCHAR (64),
    unit_system     VARCHAR (64),
    sample_rate     VARCHAR (64),
    data_range      VARCHAR (64),
    PRIMARY KEY (id)
);

CREATE TABLE t5_device (
    id              SERIAL,
    device_id       VARCHAR (64),
    level           VARCHAR (64),
    observation_id  INTEGER REFERENCES t5_observation (id),
    PRIMARY KEY (id)
);

GRANT ALL ON t5_device TO t5user;
GRANT ALL ON t5_device_id_seq TO t5user;
GRANT ALL ON t5_observation TO t5user;
GRANT ALL ON t5_observation_id_seq TO t5user;
GRANT ALL ON t5_message TO t5user;
GRANT ALL ON t5_message_id_seq TO t5user;