# T5 HL7v2 Endpoint

## Introduction

This is a server application which accepts IHE PCD-01 HL7v2 ORU01 messages (see references below) through the Minimal Lower Layer Protocol. These can for example be sent by a medical device or gateway. The application then validates and persists these messages in a database along with the ACK response messages sent out by the server itself.

## Data input
The application will listen on port 8870 for accepting HL7v2 messages through MLLP. 

If a message fails structural validation according to IHE PCD01 profile, the server will respond with an error-ACK message containing an ERR segment with a description of the error for each error found.

## Data output/persistence
The ingested HL7v2 ORU-01 message will be saved in 3 different formats for ease of analysis.

* The following formats are used to store messages as XML fields in an SQL database.
  * A custom XML format (T5 XML) which has a structure that follows the IHE PCD01-specified containment tree.
  * HL7v2 standard XML format.
* The following format is used for storage in an RDF triple store.
  * HL7v2 XML converted to RDF triples.

Observation data is also extracted from messages and stored driectly in SQL tables with separate fields for value, timestamp and a handfull of other data.

The ACK-messages sent in response by the server is also persisted as HL7v2 XML and optionally as RDF triples.

## Databases

### SQL Database
The application requires access to a PostgerSQL>=v9.4 database in which to store ingested messages as well as outgoing ACK-messages.

### RDF triple store
Optionally, the application can also store messages as RDF triples to aid further analysis and validation. This requires access to an OpenLink Virtuoso triple store. It has been tested with Virtuoso Open-Source Edition 7.2.1.

## Build
The application is written in Java version 8 and uses Gradle for automatic building and dependency management.

Before building the project, make sure to download the `virtjena_2.jar` and `virtjdbc4.jar` for Jena 2.10 and later from [OpenLink's Vituoso Open-Source Edition download page](http://virtuoso.openlinksw.com/dataspace/doc/dav/wiki/Main/VOSDownload#Jena%20Provider) and put them in the `lib/` folder.

Assuming that Java EE 8 development kit is installed and exist on the PATH environment variable, the project can be built with the following command from the project root folder:

    ./gradlew build

This outputs a .war-file into the `build/lib` directory.

## Database setup
A script for initializing all required database schema can be found at `sql/initialize.sql`.
Example:
```
psql [database or connection uri] < sql/initialize.sql
```

## Deployment
The build process produces a servlet contained in a .war-file which can be deployed on any compatible Java servlet container. It has been tested with Apache Tomcat 8.0.

The application uses the following environment variables:

* `JDBC_CONNECTION_STRING` - A string containing connection information such as hostname/IP, username, password and other connection settings accepted by the Postgres JDBC driver. All application data will be stored in this database.
* `T5_RDF_HOST` - (Optional) IP/hostname of the triple store.
* `T5_RDF_PORT` - (Optional) Port number of the triple store.
* `T5_RDF_GRAPH` - (Optional) Graph name prefix to use for storing triplified HL7 messages.
* `T5_RDF_USER` - (Optional) Triple store username.
* `T5_RDF_PASSWORD` - (Optional) Triple store password.
* `T5_DATA_INJECTION_ENABLED`- Enable/Disable device identification injection. Valid values are `true` and `false`.
* `T5_TIME_ADJUSTMENT_ENABLED` - (Optional) Enable/disable assumption of a time zone other than that of the machine on which the application is running when dealing with messages that does not specify a time zone. Valid values are `true` and `false`.
* `T5_DEFAULT_TIME_ZONE` - (Optional) The time zone to assume for incoming message that does not specify a time zone.


## References
* HL7v2 http://www.hl7.org/implement/standards/product_brief.cfm?product_id=185
* IHE PCD http://www.ihe.net/Patient_Care_Devices/
* MLLP http://www.hl7.org/implement/standards/product_brief.cfm?product_id=55
