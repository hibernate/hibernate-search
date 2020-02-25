# Hibernate Search

[![Maven Central](https://img.shields.io/maven-central/v/org.hibernate.search/hibernate-search-mapper-orm.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22org.hibernate.search%22)
[![Build Status](https://ci.hibernate.org/buildStatus/icon?job=hibernate-search/master)](https://ci.hibernate.org/job/hibernate-search/job/master)
[![Coverage Status](https://coveralls.io/repos/github/hibernate/hibernate-search/badge.svg?branch=master)](https://coveralls.io/github/hibernate/hibernate-search?branch=master)
[![Quality gate](https://sonarcloud.io/api/project_badges/measure?project=org.hibernate.search%3Ahibernate-search-parent&metric=alert_status)](https://sonarcloud.io/dashboard?id=org.hibernate.search%3Ahibernate-search-parent)
[![Language Grade: Java](https://img.shields.io/lgtm/grade/java/g/hibernate/hibernate-search.svg?logo=lgtm&logoWidth=18)](https://lgtm.com/projects/g/hibernate/hibernate-search/context:java)

## Warning: this is Hibernate Search 6 

This branch currently contains the code of Hibernate Search version 6,
a new major version which is very different from Hibernate Search 5,
and is still in its early stages.

If you are looking for the code of the version of Hibernate Search you are currently using,
you should try branch [5.11](https://github.com/hibernate/hibernate-search/tree/5.11) instead,
or any of the [older branches](https://github.com/hibernate/hibernate-search/branches/all).

If you are looking for version 6, then keep in mind that most of the old codebase
has been moved to the `legacy` directory, creating several modules with the same artifact ID.

When importing the Maven modules in Eclipse, you are advised to use
the `[groupId]:[artifactId]` project name template, in order to avoid conflicts.
You can distinguish between old and new modules by their group ID:
legacy modules use `org.hibernate`, while newer modules use `org.hibernate.search`.


## Description

*Full text search for Java objects*

This project provides synchronization between entities managed by Hibernate ORM and full-text
indexing backends like Apache Lucene and Elasticsearch.

It will automatically apply changes to indexes, which is tedious and error prone coding work,
while leaving you full control on the query aspects. The development community constantly
researches and refines the index writing techniques to improve performance.

Mapping your objects to the indexes is declarative, using a combination of Hibernate Search
specific annotations and the knowledge it can gather from your existing Hibernate/JPA
mapping.

Queries can be defined by any combination of:
* passing "native" queries directly (`org.apache.lucene.Query` for the Lucene backend, JSON for the Elasticsearch backend)
* using our backend-agnostic DSL which generates the appropriate native queries based on the available schema metadata

Query results can include projections to be loaded directly from the index, or can materialize
fully managed Hibernate entities loaded from the database within the current transactional scope.

Hibernate Search provides two backends, you can use whichever suits your application best:
* an embedded [Apache Lucene](http://lucene.apache.org/) backend,
which runs the indexing engine in the same JVM as your application.
* an [Elasticsearch](https://www.elastic.co/products/elasticsearch) backend,
which connects to an external Elasticsearch cluster over the network.

## Getting started

**NOTE**: Hibernate Search 6 is still work in progress.
Summary documentation will be made available with the first Alpha release,
and will be progressively improved with each subsequent Alpha/Beta release.

All necessary information is available on the Hibernate Search website:

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

The Elasticsearch integration tests run against one single version of Elasticsearch at a time,
launching an Elasticsearch server automatically on port 9200.
You may redefine the version to use by specifying the right profile and using the
`test.elasticsearch.connection.version` property:

    > mvn clean install -Pelasticsearch-6.0 -Dtest.elasticsearch.connection.version=6.0.0

The following profiles are available:

 * `elasticsearch-5.6` for 5.6.x and later 5.x
 * `elasticsearch-6.0` for 6.0.x to 6.2.x
 * `elasticsearch-6.3` for 6.3.x
 * `elasticsearch-6.4` for 6.4.x to 6.6.x
 * `elasticsearch-6.7` for 6.7 and later 6.x
 * `elasticsearch-7.0` for 7.x (the default)

A list of available versions for `test.elasticsearch.connection.version` can be found on
[Maven Central](https://search.maven.org/search?q=g:org.elasticsearch%20AND%20a:elasticsearch&core=gav).

Alternatively, you can prevent the build from launching an Elasticsearch server automatically
and run Elasticsearch-related tests against your own server using the
`test.elasticsearch.connection.hosts` properties:

    > mvn clean install -Dtest.elasticsearch.connection.hosts=http://localhost:9200

If you want to run tests against a different Elasticsearch version  (6.x for instance),
you will still have to select a profile among those listed above, and specify the version:

    > mvn clean install -Pelasticsearch-6.0 -Dtest.elasticsearch.connection.version=6.0.0 -Dtest.elasticsearch.connection.hosts=http://localhost:9200

You may also use authentication:

    > mvn clean install -Dtest.elasticsearch.connection.hosts=https://localhost:9200 -Dtest.elasticsearch.connection.username=ironman -Dtest.elasticsearch.connection.password=j@rV1s

Also, the elasticsearch integration tests can be executed
against an Elasticsearch service on AWS.
You will need to execute something along the lines of:

    > mvn clean install -Dtest.elasticsearch.connection.hosts=<The full URL of your Elasticsearch endpoint> -Dtest.elasticsearch.connection.aws.signing.access_key=<Your access key> -Dtest.elasticsearch.connection.aws.signing.secret_key=<Your secret key> -Dtest.elasticsearch.connection.aws.signing.region=<Your AWS region ID>

When building Hibernate Search with new JDKs, you may want to run Elasticsearch with a different JDK than the one used by Maven.
This can be done by setting a property
(**this will only work with the profiles for Elasticsearch 5 and above**):

    > mvn clean install -Dtest.elasticsearch.run.java_home=/path/to/my/jdk

## JQAssistant

You can request static analysis and sanity checks with the `jqassistant` profile.
Tests do not need to be run for these checks.

    > mvn clean install -Pjqassistant -DskipTests

To also check cyclic dependencies between packages, use `-Djqassistant.groups=default,cycles`.
Cyclic dependency analysis is costly and may add significant overhead to the build:
at least 10 seconds, maybe one minute or more depending on your setup.

    > mvn clean install -Pjqassistant -DskipTests -Djqassistant.groups=default,cycles

You can also inspect the created Neo4j datastore after a build,
provided that build had the `jqassistant` profile enabled:

    > mvn jqassistant:server -Pjqassistant-server -pl reports

The Neo4j web UI will be accessible from http://localhost:7474/.

## Contributing

New contributors are always welcome. We collected some helpful hints on how to get started
on our website at [Contribute to Hibernate Search](http://hibernate.org/search/contribute/)

## Source code structure

The project is split in several Maven modules:

* `backend`: The backends, i.e. the modules that provide integration to actual indexing services.
  * `elasticsearch`: A backend that connects to a remote Elasticsearch cluster.
  * `lucene`: A backend that uses an embedded (same JVM) Lucene instance.
* `build-config`: Code-related artifacts like [checkstyle](https://checkstyle.org/) and [forbiddenapis](https://github.com/policeman-tools/forbidden-apis) rules.
* `distribution`: Builds the distribution package.
* `documentation`: The project documentation.
* `engine`: The Hibernate Search engine.
This module handles most of the basic integration work (configuration properties, bean instantiation, ...),
defines APIs common to every mapper/backend (the Search DSL in particular),
and provides the "glue" between mappers and backends.
* `integrationtest`: Integration tests for backends (Elasticsearch, Lucene) and mappers (Hibernate ORM),
as well as any other technology Hibernate Search integrates with.
Here are some notable sub-directories:
  *  `performance`: performance tests.
  *  `showcase/library`: a sample application using Hibernate Search in a Spring Boot environment.
* `legacy`: Legacy code from Search 5. This code is not part of the distributed JARs.
Parts of it will progressively be re-integrated into the main (Search 6+) code base.
Note that compilation and tests in this directory are disabled by default
(they are only enabled when the property `legacy.skip` is set to `false`).
When enabled, Elasticsearch integration test in this directory 
are executed against Elasticsearch 5.6 by default (instead of 6.0).
* `mapper`: The mappers, i.e. the modules that expose APIs to index and search user entities,
and do the work of converting between user entities and documents to be indexed.
  * `javabean`: An experimental (not published) mapper for Java Beans without Hibernate ORM.
  Mostly useful for tests of the `pojo` module.
  * `orm`: A mapper for [Hibernate ORM](http://hibernate.org/orm/) entities.
  * `pojo`: Contains base classes and APIs that are re-used in other POJO-based mapper.
* `reports`: Module built last, producing reports related to test coverage in particular.
* `util`: Various modules containing util classes, both for runtime and for tests.

## Contact

### Latest Documentation

See <http://hibernate.org/search/documentation/>.

### Bug Reports

See the HSEARCH project on the Hibernate JIRA instance: <https://hibernate.atlassian.net/browse/HSEARCH>.

### Community Support

See <http://hibernate.org/community/>.

## License

This software and its documentation are distributed under the terms of the FSF Lesser GNU Public
License (see lgpl.txt).

