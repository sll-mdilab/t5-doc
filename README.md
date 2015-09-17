# T5 HL7v2 Endpoint

## Introduction

This is a server application which accepts IHE PCD-01 HL7v2 ORU01 messages (see references below) through the Minimal Lower Layer Protocol. These can for example be sent by a medical device or gateway. The application then validates and persists these messages in a database along with the ACK response messages sent out by the server itself.

## Data input
The application will listen on port 8870 for accepting HL7v2 messages through MLLP. 

If a message fails structural validation according to IHE PCD01 profile, the server will respond with an error-ACK message containing an ERR segment with a description of the error for each error found.

## Data output/persistence
The ingested HL7v2 ORU-01 message will be saved in 3 different formats for ease of analysis.

* The following formats are used to store messages in an XML document database.
  * A custom XML format (T5 XML) which has a structure that follows the IHE PCD01-specified containment tree.
  * HL7v2 XML format.
* The following format is used for storage in an RDF triple store.
  * HL7v2 XML converted to RDF triples.

The ACK-messages sent in response by the server is also persisted as HL7v2 XML and RDF triples.

## Databases
### XML Document Database
The application requires access to a Mark Logic >= 8.0 database in which to store ingested messages as well as outgoing ACK-messages.

### RDF triple store
Optionally, the application can also store messages as RDF triples to aid further analysis and validation. This requires access to an OpenLink Virtuoso triple store. It has been tested with Virtuoso Open-Source Edition 7.2.1.

## Build
The application is written in Java version 8 and uses Gradle for automatic building and dependency management.

Before building the project, make sure to download the `virtjena_2.jar` and `virtjdbc4.jar` for Jena 2.10 and later from [OpenLink's Vituoso Open-Source Edition download page](http://virtuoso.openlinksw.com/dataspace/doc/dav/wiki/Main/VOSDownload#Jena%20Provider) and put them in the `lib/` folder.

Assuming that Java EE 8 development kit is installed and exist on the PATH environment variable, the project can be built with the following command from the project root folder:

    ./gradlew build fatJar

This outputs a .jar-file into the `build/lib` directory.

## Deployment

The application is a stand-alone Java program which can be run with a compatible Java virtual machine, e. g:

    java -jar t5poc.jar

The application uses the following environment variables:

* `T5_DATABASE_HOST` - IP/hostname of the database server.
* `T5_DATABASE_PORT` - Port number of Mark Logic HTTP REST API endpoint.
* `T5_DATABASE_USER` - Database username.
* `T5_DATABASE_PASSWORD` - Database password.
* `T5_RDF_HOST` - IP/hostname of the triple store.
* `T5_RDF_PORT` - Port number of the triple store.
* `T5_RDF_GRAPH` - Graph name prefix to use for storing triplified HL7 messages.
* `T5_RDF_USER` - Triple store username.
* `T5_RDF_PASSWORD` - Triple store password.
* `T5_DATABASE_XCC_NAME`- Name of the database schema 
* `T5_DATABASE_XCC_PORT`- Port number of XCC endpoint.
* `T5_DATA_INJECTION_ENABLED`- Enable/Disable device identification injection.

## References
* HL7v2 http://www.hl7.org/implement/standards/product_brief.cfm?product_id=185
* IHE PCD http://www.ihe.net/Patient_Care_Devices/
* MLLP http://www.hl7.org/implement/standards/product_brief.cfm?product_id=55
