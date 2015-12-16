--TRUNCATE t5_device, t5_observation, t5_message;

--DROP TABLE t5_device;
--DROP TABLE t5_observation;
--DROP TABLE t5_message;

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
SELECT fhir_create_storage('{"resourceType": "Encounter"}');
SELECT fhir_create_storage('{"resourceType": "Device"}');
SELECT fhir_create_storage('{"resourceType": "Patient"}');
SELECT fhir_create_storage('{"resourceType": "Practitioner"}');
SELECT fhir_create_storage('{"resourceType": "BrokeringReceiver"}');
SELECT fhir_create_storage('{"resourceType": "EpisodeOfCare"}');
SELECT fhir_create_storage('{"resourceType": "ReferralRequest"}');
SELECT fhir_create_storage('{"resourceType": "Organization"}');
SELECT fhir_create_storage('{"resourceType": "ClinicalImpression"}');
SELECT fhir_create_storage('{"resourceType": "Medication"}');
SELECT fhir_create_storage('{"resourceType": "MedicationOrder"}');
SELECT fhir_create_storage('{"resourceType": "Procedure"}');
SELECT fhir_create_storage('{"resourceType": "DiagnosticReport"}');
SELECT fhir_create_storage('{"resourceType": "Condition"}');

SELECT fhir_create_resource(
$$
{
"resource": {
 "resourceType": "SearchParameter",
 "id": "ReferralRequest--encounter",
 "url": "http://sll-mdilab.com/fhir/SearchParameter/ReferralRequest--encounter",
 "name": "-encounter",
 "publisher": "Karolinska University Hospital",
 "date": "2015-12-09T11:07:00+00:00",
 "code": "-encounter",
 "base": "ReferralRequest",
 "type": "reference",
 "description": "The encounter referenced by this request",
 "xpath": "f:ReferralRequest/f:encounter",
 "xpathUsage": "normal"
}
}
$$
);

SELECT fhir_create_resource(
$$
{
"resource": {
 "resourceType": "SearchParameter",
 "id": "ReferralRequest--supporting-information",
 "url": "http://sll-mdilab.com/fhir/SearchParameter/ReferralRequest--supporting-information",
 "name": "-supporting-information",
 "publisher": "Karolinska University Hospital",
 "date": "2015-12-09T11:07:00+00:00",
 "code": "-supporting-information",
 "base": "ReferralRequest",
 "type": "reference",
 "description": "The supporting information referenced by this request",
 "xpath": "f:ReferralRequest/f:supportingInformation",
 "xpathUsage": "normal"
}
}
$$
);

SELECT fhir_create_resource(
$$
{
"resource": {
 "resourceType": "SearchParameter",
 "id": "Encounter--service-provider",
 "url": "http://sll-mdilab.com/fhir/SearchParameter/Encounter--service-provider",
 "name": "-service-provider",
 "publisher": "Karolinska University Hospital",
 "date": "2015-12-09T11:07:00+00:00",
 "code": "-service-provider",
 "base": "Encounter",
 "type": "reference",
 "description": "The service provider organization for the encounter",
 "xpath": "f:Encounter/f:serviceProvider",
 "xpathUsage": "normal"
}
}
$$
);

SELECT fhir_create_resource(
$$
{
"resource": {
 "resourceType": "SearchParameter",
 "id": "DeviceUseStatement-start",
 "url": "http://sll-mdilab.com/fhir/SearchParameter/DeviceUseStatement-start",
 "name": "start",
 "publisher": "Karolinska University Hospital",
 "date": "2015-11-13T11:07:00+00:00",
 "code": "start",
 "base": "DeviceUseStatement",
 "type": "date",
 "description": "Search by whenUsed.start",
 "xpath": "f:DeviceUseStatement/f:whenUsed/f:start",
 "xpathUsage": "normal"
}
}
$$
);

SELECT fhir_create_resource(
$$
{
"resource": {
 "resourceType": "SearchParameter",
 "id": "DeviceUseStatement-end",
 "url": "http://sll-mdilab.com/fhir/SearchParameter/DeviceUseStatement-end",
 "name": "end",
 "publisher": "Karolinska University Hospital",
 "date": "2015-11-13T11:07:00+00:00",
 "code": "end",
 "base": "DeviceUseStatement",
 "type": "date",
 "description": "Search by whenUsed.end",
 "xpath": "f:DeviceUseStatement/f:whenUsed/f:end",
 "xpathUsage": "normal"
}
}
$$
);

SELECT fhir_create_resource(
$$
{
"resource": {
 "resourceType": "SearchParameter",
 "id": "DeviceUseStatement--period",
 "url": "http://sll-mdilab.com/fhir/SearchParameter/DeviceUseStatement--period",
 "name": "-period",
 "publisher": "Karolinska University Hospital",
 "date": "2015-11-13T11:07:00+00:00",
 "code": "-period",
 "base": "DeviceUseStatement",
 "type": "date",
 "description": "Search by whenUsed",
 "xpath": "f:DeviceUseStatement/f:whenUsed",
 "xpathUsage": "normal"
}
}
$$
);

SELECT fhir_index_parameter('{"resourceType": "EpisodeOfCare", "name": "team-member"}');
SELECT fhir_index_parameter('{"resourceType": "EpisodeOfCare", "name": "status"}');

SELECT fhir_index_parameter('{"resourceType": "ReferralRequest", "name": "requester"}');
SELECT fhir_index_parameter('{"resourceType": "ReferralRequest", "name": "recipient"}');
SELECT fhir_index_parameter('{"resourceType": "ReferralRequest", "name": "status"}');
SELECT fhir_index_parameter('{"resourceType": "ReferralRequest", "name": "patient"}');

SELECT fhir_index_parameter('{"resourceType": "Encounter", "name": "status"}');
SELECT fhir_index_parameter('{"resourceType": "Encounter", "name": "episodeofcare"}');
SELECT fhir_index_parameter('{"resourceType": "Encounter", "name": "patient"}');
SELECT fhir_index_parameter('{"resourceType": "Encounter", "name": "part-of"}');
SELECT fhir_index_parameter('{"resourceType": "Encounter", "name": "type"}');

SELECT fhir_index_parameter('{"resourceType": "DeviceUseStatement", "name": "patient"}');
SELECT fhir_index_parameter('{"resourceType": "DeviceUseStatement", "name": "subject"}');
SELECT fhir_index_parameter('{"resourceType": "DeviceUseStatement", "name": "start"}');
SELECT fhir_index_parameter('{"resourceType": "DeviceUseStatement", "name": "end"}');

SELECT fhir_index_parameter('{"resourceType": "Patient", "name": "identifier"}');
SELECT fhir_index_parameter('{"resourceType": "Patient", "name": "name"}');

GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO t5user;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO t5user;
