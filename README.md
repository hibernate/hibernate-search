# Hibernate Search

*Version: 5.0.0.Alpha1*

## Description

Full text search engines like Apache Lucene are very powerful technologies to add efficient free text search
capabilities to applications. However, Lucene suffers several mismatches when dealing with object domain models.
Amongst other things indexes have to be kept up to date and mismatches between index structure and domain model
as well as query mismatches have to be avoided.

Hibernate Search addresses these shortcomings - it indexes your domain model with the help of a few annotations,
takes care of database/index synchronization and brings back regular managed objects from free text queries.

Hibernate Search is using [Apache Lucene](http://lucene.apache.org/) under the cover.

## Requirements

This version of Hibernate Search requires:

* Hibernate Core 4.3.x
* Apache Lucene 4.6.x

## Instructions

### Maven 

Include the following to your dependency list:

    <dependency>
     <groupId>org.hibernate</groupId>
     <artifactId>hibernate-search</artifactId>
     <version>5.0.0.Alpha1</version>
    </dependency>

### Sourceforge Bundle

Download the distribution bundle from [SourceForge](http://sourceforge.net/projects/hibernate/files/hibernate-search) and unzip to installation directory. Then read the documentation available in *docs/reference*.

### Building from source

    > git clone git@github.com:hibernate/hibernate-search.git
    > cd hibernate-search
    > mvn clean install -s settings-example.xml

#### Build options (profiles)

Per default the documentation is not built. To include it in the full build, run:

    > mvn clean install -Pdocs -s settings-example.xml

To build the distribution bundle run:

    > mvn clean install -Pdist -s settings-example.xml

If you want to run the performance test under _integration/performance_:

    > mvn clean install -Pperf -s settings-example.xml

You can also build the above mentioned modules directly by changing into these directories and executing maven in the
module directory.

### Contributing
    
If you want to contribute, you find all you need to know in [Contributing to Hibernate Search](http://community.jboss.org/wiki/ContributingtoHibernateSearch)

### Source code structure

The project is split in several Maven modules.

backends:
Remote backends receiving an indexing job and executing it via different
protocols.

build-config:
Code related artefacts like checkstyle rules.

distribution:
Builds the distribution package.

documentation:
The project documentation.

engine:
Engine of the project. Most of the beef is here.

infinispan:
Backend storing indexes in Infinispan.

integrationtest:
Integration tests with various technologies like WildFly, Spring. Also include performance tests.

legacy:
Old Maven GAV kept for backward compatibility.

modules:
Integration with containers like WildFly.

orm:
Native integration offering Hibernate Search for Hibernate ORM.

serialization:
Serialization code used by remote backends.

testing:
Various helper classes to write tests using Hibernate Search. This module is
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

This software and its documentation are distributed under the terms of the FSF Lesser GNU Public License (see lgpl.txt).

