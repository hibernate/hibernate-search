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

## Requirements

This version of Hibernate Search requires:

* Java SE 8
* Hibernate ORM 5.2.3.Final or a later 5.2.x
* Apache Lucene 5.5.x

Hibernate ORM versions 5.2.0.Final, 5.2.1.Final and 5.2.2.Final are NOT fully compatible.

## Instructions

### Maven

Include the following to your dependency list:

    <dependency>
       <groupId>org.hibernate</groupId>
       <artifactId>hibernate-search-orm</artifactId>
       <version>5.7.3.Final</version>
    </dependency>

### Sourceforge Bundle

Download the distribution bundle from
[SourceForge](http://sourceforge.net/projects/hibernate/files/hibernate-search) and unzip to
installation directory. Then read the documentation available in *docs/reference*.

### Building from source

    > git clone git@github.com:hibernate/hibernate-search.git
    > cd hibernate-search
    > mvn clean install

#### Build options (profiles and properties)

The documentation is based on [Asciidoctor](http://asciidoctor.org/). By default only the HTML
output is enabled; to also generate the PDF output use:

    > mvn clean install -Pdocumentation-pdf

To build the distribution bundle run:

    > mvn clean install -Pdocumentation-pdf,dist

The Elasticsearch module tests against one single version of Elasticsearch at a time.
You may redefine the version to use by specifying the right profile and using the
`testElasticsearchVersion` property:

    > mvn clean install -Pelasticsearch-2.0 -DtestElasticsearchVersion=2.1.0

The following profiles are available:

 * `elasticsearch-2.0` for 2.0.x and 2.1.x
 * `elasticsearch-2.2` for 2.2.x and later 2.x (the default)

A list of available versions for `testElasticsearchVersion` can be found on
[Maven Central](http://search.maven.org/#search%7Cgav%7C1%7Cg%3A%22org.elasticsearch%22%20AND%20a%3A%22elasticsearch%22).

### Contributing

New contributors are always welcome. We collected some helpful hints on how to get started
on our website at [Contribute to Hibernate Search](http://hibernate.org/search/contribute/)

### Source code structure

The project is split in several Maven modules:

* _backends_: Remote backends receiving an indexing job and executing it via different protocols.

* _build-config_: Code related artefacts like checkstyle rules.

* _distribution_: Builds the distribution package.

* _documentation_: The project documentation.

* _elasticsearch_: All code relating to the Elasticsearch integration.

* _engine_: The engine of the project. Most of the beef is here.

* _integrationtest_: Integration tests with various technologies like WildFly, Spring and Karaf.
Also includes performance tests.

* _modules_: Integration with [WildFly](http://www.wildfly.org/) using JBoss Modules.

* _orm_: Native integration for [Hibernate ORM](http://hibernate.org/orm/), and also home of most public API code.

* _serialization_: Serialization code used by remote backends.

* _testing_: Various helper classes to write tests using Hibernate Search. This module is
semi private.

## Contact

### Latest Documentation:

* [http://search.hibernate.org](http://hibernate.org/search/documentation/)

### Bug Reports:

* Hibernate JIRA [HSEARCH](https://hibernate.atlassian.net/browse/HSEARCH) (preferred)
* hibernate-dev@lists.jboss.org

### Free Technical Support:

* [Hibernate Forum](http://forum.hibernate.org/viewforum.php?f=9)
* [Stackoverflow](http://stackoverflow.com/questions/tagged/hibernate-search); please use tag `hibernate-search`.

## License

This software and its documentation are distributed under the terms of the FSF Lesser GNU Public
License (see lgpl.txt).

