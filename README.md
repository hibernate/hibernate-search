# Hibernate Search

## Description

Full text search for Java objects

This project provides synchronization between entities managed by Hibernate ORM and full-text
indexing services like Apache Lucene and Elasticsearch.

It will automatically apply changes to indexes, which is tedious and error prone coding work,
while leaving you full control on the query aspects. The development community constantly
researches and refines the index writing techniques to improve performance.

Mapping your objects to the indexes is declarative, using a combination of Hibernate Search
specific annotations and the knowledge it can gather from your existing Hibernate/JPA
mapping.

Queries can be defined by any combination of:
 - "native" Apache Lucene queries
 - writing "native" Elasticsearch queries in Json format (if using Elasticsearch, which is optional)
 - using our DSL which abstracts the previous two generating optimal backend specific queries

Query results can include projections to be loaded directly from the index, or can materialize
fully managed Hibernate entities loaded from the database within the current transactional scope.

Hibernate Search is using [Apache Lucene](http://lucene.apache.org/) under the cover; this
can be used directly (running embedded in the same JVM) or remotely provided by Elasticsearch
over its REST API.

## Getting started

All necessary information is available on the Hibernate Search website:

* [Getting started guide for the latest stable version](http://hibernate.org/search/documentation/getting-started/)
* [Available versions and compatibility matrix](http://hibernate.org/search/releases/)
* [Reference documentation for all versions (current and past)](http://hibernate.org/search/documentation/)

For offline use, distribution bundles downloaded from [SourceForge](https://sourceforge.net/projects/hibernate/files/hibernate-search/)
also include the reference documentation for the downloaded version in PDF and HTML format. 

## Building from source

    > git clone git@github.com:hibernate/hibernate-search.git
    > cd hibernate-search
    > mvn clean install

### Build options (profiles and properties)

#### Documentation
The documentation is based on [Asciidoctor](http://asciidoctor.org/). By default only the HTML
output is enabled; to also generate the PDF output use:

    > mvn clean install -Pdocumentation-pdf

+You can then find the freshly built documentation in the following location:
  
    > ./documentation/target/asciidoctor/en-US

#### Distribution

To build the distribution bundle run:

    > mvn clean install -Pdocumentation-pdf,dist

#### Elasticsearch

The Elasticsearch module tests against one single version of Elasticsearch at a time,
launching an Elasticsearch server automatically on port 9200.
You may redefine the version to use by specifying the right profile and using the
`test.elasticsearch.host.version` property, while disabling the default profile:

    > mvn clean install -P!elasticsearch-5.2,elasticsearch-2.0 -Dtest.elasticsearch.host.version=2.1.0

The following profiles are available:

 * `elasticsearch-2.0` for 2.0.x and 2.1.x
 * `elasticsearch-2.2` for 2.2.x and later 2.x
 * `elasticsearch-5.0` for 5.0.x and 5.1.x
 * `elasticsearch-5.2` for 5.2.x and later 5.x (the default)

A list of available versions for `test.elasticsearch.host.version` can be found on
[Maven Central](http://search.maven.org/#search%7Cgav%7C1%7Cg%3A%22org.elasticsearch%22%20AND%20a%3A%22elasticsearch%22).

Alternatively, you can prevent the build from launching an Elasticsearch server automatically
and run Elasticsearch-related tests against your own server using the
`test.elasticsearch.host.provided` and `test.elasticsearch.host.url` properties:

    > mvn clean install -Dtest.elasticsearch.host.provided=true -Dtest.elasticsearch.host.url=http://localhost:9200

If you want to run tests against an older Elasticsearch version  (2.x for instance),
you will still have to select a profile among those listed above, and disable the default profile:

    > mvn clean install -P!elasticsearch-5.2,elasticsearch-2.2 -Dtest.elasticsearch.host.provided=true -Dtest.elasticsearch.host.url=http://localhost:9200

You may also use authentication:

    > mvn clean install -Dtest.elasticsearch.host.provided=true -Dtest.elasticsearch.host.url=https://localhost:9200 -Dtest.elasticsearch.host.username=ironman -Dtest.elasticsearch.host.password=j@rV1s

Also, the elasticsearch module (and only this one) can execute its integration tests
against an Elasticsearch service on AWS.
You will need to execute something along the lines of:

    > mvn integration-test -pl elasticsearch -Dtest.elasticsearch.host.provided=true -Dtest.elasticsearch.host.url=<The full URL of your Elasticsearch endpoint> -Dtest.elasticsearch.host.aws.access_key=<Your access key> -Dtest.elasticsearch.host.aws.secret_key=<Your secret key> -Dtest.elasticsearch.host.aws.region=<Your AWS region ID>

## Contributing

New contributors are always welcome. We collected some helpful hints on how to get started
on our website at [Contribute to Hibernate Search](http://hibernate.org/search/contribute/)

## Source code structure

The project is split in several Maven modules:

* _backends_: Remote backends receiving an indexing job and executing it via different protocols.

* _build-config_: Code-related artifacts like checkstyle rules.

* _distribution_: Builds the distribution package.

* _documentation_: The project documentation.

* _elasticsearch_: The core of the Elasticsearch integration.

* _elasticsearch-aws_: Specific bits enabling connection to an AWS-hosted Elasticsearch cluster

* _engine_: The engine of the project. Most of the beef is here.

* _integrationtest_: Integration tests with various technologies like WildFly, Spring and Karaf.
Also includes performance tests.

* _jbossmodules_: Integration with [WildFly](http://www.wildfly.org/) using JBoss Modules.

* _jsr352_: JSR 352 - Batch Applications for the Java Platform integration.

* _orm_: Native integration for [Hibernate ORM](http://hibernate.org/orm/), and also home of most public API code.

* _reports_: Last-built module, producing reports related to test coverage in particular.

* _serialization_: Serialization code used by remote backends.

* _sharedtestresources_: Internal module providing various test resources to our integration tests.

* _testing_: Various helper classes to write tests using Hibernate Search. This module is
semi private.

## Contact

### Latest Documentation:

* [http://hibernate.org/search/documentation/](http://hibernate.org/search/documentation/)

### Bug Reports:

* Hibernate JIRA [HSEARCH](https://hibernate.atlassian.net/browse/HSEARCH) (preferred)
* [hibernate-dev@lists.jboss.org](mailto:hibernate-dev@lists.jboss.org)

### Free Technical Support:

* [Hibernate Forum](https://discourse.hibernate.org/c/hibernate-search)
* [Stackoverflow](http://stackoverflow.com/questions/tagged/hibernate-search); please use tag `hibernate-search`.

## License

This software and its documentation are distributed under the terms of the FSF Lesser GNU Public
License (see lgpl.txt).

