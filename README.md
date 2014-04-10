# Hibernate Search

*Version: 4.5.1.Final 10-04-2014*

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
* Apache Lucene 3.6.x

## Instructions

### Maven 

Include the following to your dependency list:

    <dependency>
     <groupId>org.hibernate</groupId>
     <artifactId>hibernate-search</artifactId>
     <version>4.5.0.Final</version>
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

Last but not least, if you want to run the performance test under _integration/performance_:

    > mvn clean install -Pperf -s settings-example.xml

You can also build the above mentioned modules directly by changing into these directories and executing maven in the
module directory.

### Contributing
    
If you want to contribute, you find all you need to know in [Contribute to Hibernate Search](http://hibernate.org/search/contribute/)

## Contact

### Latest Documentation:

* [http://search.hibernate.org](http://hibernate.org/search/documentation/)

### Bug Reports:

* Hibernate JIRA [HSEARCH](https://hibernate.atlassian.net/browse/HSEARCH) (preferred)
* hibernate-dev@lists.jboss.org

### Free Technical Support:

* [Hibernate Forum](http://forum.hibernate.org/viewforum.php?f=9)

## License

This software and its documentation are distributed under the terms of the FSF Lesser Gnu Public License (see lgpl.txt).

