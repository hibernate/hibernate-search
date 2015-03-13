# Hibernate Search

*Version: 5.1.1.Final*

## Description

Full text search engines like Apache Lucene are very powerful technologies to add efficient free
text search capabilities to applications. However, Lucene suffers several mismatches when dealing
with object domain models. Amongst other things indexes have to be kept up to date and mismatches
between index structure and domain model as well as query mismatches have to be avoided.

Hibernate Search addresses these shortcomings - it indexes your domain model with the help of a few
annotations, takes care of database/index synchronization and brings back regular managed objects
from free text queries.

Hibernate Search is using [Apache Lucene](http://lucene.apache.org/) under the cover.

## Requirements

This version of Hibernate Search requires:

* Hibernate Core 4.3.x
* Apache Lucene 4.10.x

## Instructions

### Maven

Include the following to your dependency list:

    <dependency>
       <groupId>org.hibernate</groupId>
       <artifactId>hibernate-search-orm</artifactId>
       <version>5.1.1.Final</version>
    </dependency>

### Sourceforge Bundle

Download the distribution bundle from
[SourceForge](http://sourceforge.net/projects/hibernate/files/hibernate-search) and unzip to
installation directory. Then read the documentation available in *docs/reference*.

### Building from source

    > git clone git@github.com:hibernate/hibernate-search.git
    > cd hibernate-search
    > mvn clean install -s settings-example.xml

#### Build options (profiles)

The documentation is based on [AsciiDoctor](http://asciidoctor.org/). Per default only the html
output is enabled. To also generate the docbok output and build the documentation from there use:

    > mvn clean install -Pdocbook -s settings-example.xml

To build the distribution bundle run:

    > mvn clean install -Pdocbook,dist -s settings-example.xml

You can also build the above mentioned modules directly by changing into these directories and
executing maven in the module directory.

### Contributing

If you want to contribute, you find all you need to know in
[Contributing to Hibernate Search](http://community.jboss.org/wiki/ContributingtoHibernateSearch)

### Source code structure

The project is split in several Maven modules:

* _backends_: Remote backends receiving an indexing job and executing it via different protocols.

* _build-config_: Code related artefacts like checkstyle rules.

* _distribution_: Builds the distribution package.

* _documentation_: The project documentation.

* _engine_: The engine of the project. Most of the beef is here.

* _infinispan_: Backend storing indexes in [Infinispan](http://infinispan.org/).

* _integrationtest_: Integration tests with various technologies like WildFly, Spring or Karaf.
Also includes performance tests.

* _legacy_: Old Maven GAV kept for backward compatibility.

* _modules_: Integration with [WildFly](http://www.wildfly.org/) using JBoss Modules.

* _orm_: Native integration for [Hibernate ORM](http://hibernate.org/orm/).

* serialization: Serialization code used by remote backends.

* testing: Various helper classes to write tests using Hibernate Search. This module is
semi private.

## Contact

### Latest Documentation:

* [http://search.hibernate.org](http://www.hibernate.org/subprojects/search/docs)

### Bug Reports:

* Hibernate JIRA [HSEARCH](https://hibernate.atlassian.net/browse/HSEARCH) (preferred)
* hibernate-dev@lists.jboss.org

### Free Technical Support:

* [Hibernate Forum](http://forum.hibernate.org/viewforum.php?f=9)

## License

This software and its documentation are distributed under the terms of the FSF Lesser GNU Public
License (see lgpl.txt).

