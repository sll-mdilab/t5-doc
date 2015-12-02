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

CREATE INDEX t5_observation_start_time_idx ON t5_observation (start_time);
CREATE INDEX t5_observation_code_idx ON t5_observation (code);
CREATE INDEX t5_device_device_id_idx ON t5_device (device_id);

SET plv8.start_proc = 'plv8_init';
SELECT fhir_create_storage('{"resourceType": "DeviceUseStatement"}');

GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO t5user;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO t5user;