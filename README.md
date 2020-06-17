# Hibernate Search

[![Maven Central](https://img.shields.io/maven-central/v/org.hibernate.search/hibernate-search-mapper-orm.svg?label=Maven%20Central&style=for-the-badge)](https://search.maven.org/search?q=g:%22org.hibernate.search%22)
[![Build Status](https://img.shields.io/jenkins/build?jobUrl=https%3A%2F%2Fci.hibernate.org%2Fjob%2Fhibernate-search%2Fjob%2Fmaster%2F&style=for-the-badge)](https://ci.hibernate.org/job/hibernate-search/job/master)
[![Coverage Status](https://img.shields.io/coveralls/github/hibernate/hibernate-search/master?logo=coveralls&style=for-the-badge)](https://coveralls.io/github/hibernate/hibernate-search?branch=master)
[![Quality gate](https://img.shields.io/sonar/alert_status/org.hibernate.search:hibernate-search-parent?logo=sonarcloud&server=https%3A%2F%2Fsonarcloud.io&style=for-the-badge)](https://sonarcloud.io/dashboard?id=org.hibernate.search%3Ahibernate-search-parent)
[![Language Grade: Java](https://img.shields.io/lgtm/grade/java/g/hibernate/hibernate-search.svg?logo=lgtm&logoWidth=18&style=for-the-badge)](https://lgtm.com/projects/g/hibernate/hibernate-search/context:java)

## Description

Hibernate Search automatically extracts data from Hibernate ORM entities to push it to
local [Apache Lucene](http://lucene.apache.org/) indexes
or remote [Elasticsearch](https://www.elastic.co/products/elasticsearch) indexes.

It features:

* [**Declarative mapping**](https://docs.jboss.org/hibernate/search/6.0/reference/en-US/html_single/#mapper-orm-mapping)
of entity properties to index fields,
either through annotations or a programmatic API.
* [**On-demand mass indexing**](https://docs.jboss.org/hibernate/search/6.0/reference/en-US/html_single/#mapper-orm-indexing-massindexer)
of all entities in the database,
to initialize the indexes with pre-existing data.
* [**On-the-fly automatic indexing**](https://docs.jboss.org/hibernate/search/6.0/reference/en-US/html_single/#mapper-orm-indexing-automatic)
of entities modified through a Hibernate ORM session,
to always keep the indexes up-to-date.
* [**A Search DSL**](https://docs.jboss.org/hibernate/search/6.0/reference/en-US/html_single/#search-dsl)
to easily build full-text search queries
and retrieve the hits as Hibernate ORM entities.
* And more: [configuration of analyzers](https://docs.jboss.org/hibernate/search/6.0/reference/en-US/html_single/#concepts-analysis),
many different [predicates](https://docs.jboss.org/hibernate/search/6.0/reference/en-US/html_single/#search-dsl-predicate)
and [sorts](https://docs.jboss.org/hibernate/search/6.0/reference/en-US/html_single/#search-dsl-sort)
in the Search DSL,
[spatial support](https://docs.jboss.org/hibernate/search/6.0/reference/en-US/html_single/#mapper-orm-geopoint).
search queries returning [projections](https://docs.jboss.org/hibernate/search/6.0/reference/en-US/html_single/#search-dsl-projection)
instead of entities,
[aggregations](https://docs.jboss.org/hibernate/search/6.0/reference/en-US/html_single/#search-dsl-aggregation),
advanced customization of the mapping using [bridges](https://docs.jboss.org/hibernate/search/6.0/reference/en-US/html_single/#mapper-orm-bridge),
...

For example, map your entities like this:

```java
@Entity
// This entity is mapped to an index
@Indexed
public class Book {

    // The entity ID is the document ID
    @Id
    @GeneratedValue
    private Integer id;

    // This property is mapped to a document field
    @FullTextField(analyzer = "english")
    private String title;

    @ManyToMany
    // Authors will be embedded in Book documents
    @IndexedEmbedded
    private Set<Author> authors = new HashSet<>();

    // Getters and setters
    // ...
}

@Entity
public class Author {

    @Id
    @GeneratedValue
    private Integer id;

    // This property is mapped to a document field
    @FullTextField(analyzer = "name")
    private String name;

    @ManyToMany(mappedBy = "authors")
    private Set<Book> books = new HashSet<>();

    public Author() {
    }

    // Getters and setters
    // ...
}
```

Index existing data like this:

```java
SearchSession searchSession = Search.session( entityManager );
MassIndexer indexer = searchSession.massIndexer( Book.class );
indexer.startAndWait();
```

Automatic indexing does not require any change to code based on JPA or Hibernate ORM:

```java
Author author = new Author();
author.setName( "Isaac Asimov" );

Book book = new Book();
book.setTitle( "The Caves Of Steel" );
book.getAuthors().add( author );
author.getBooks().add( book );

entityManager.persist( author );
entityManager.persist( book );
```

And search like this:

```java
SearchResult<Book> result = Search.session( entityManager )
        .search( Book.class )
        .where( f -> f.match()
                .fields( "title", "authors.name" )
                .matching( "Isaac" )
        )
        .fetch( 20 );

long totalHitCount = result.getTotalHitCount();
List<Book> hits = result.getHits();
```

## License

This software and its documentation are distributed under the terms of the FSF Lesser GNU Public
License (see lgpl.txt).

## Getting started

A getting started guide is available
[in the reference documentation](https://docs.jboss.org/hibernate/search/6.0/reference/en-US/html_single/#getting-started).

Fore more information, refer to the Hibernate Search website:

* [Available versions and compatibility matrix](http://hibernate.org/search/releases/)
* [Reference documentation for all versions (current and past)](http://hibernate.org/search/documentation/)

For offline use, distribution bundles downloaded from [SourceForge](https://sourceforge.net/projects/hibernate/files/hibernate-search/)
also include the reference documentation for the downloaded version in PDF and HTML format. 

## Contact

### Latest Documentation

See <http://hibernate.org/search/documentation/>.

### Bug Reports

See the HSEARCH project on the Hibernate JIRA instance: <https://hibernate.atlassian.net/browse/HSEARCH>.

### Community Support

See <http://hibernate.org/community/>.

## Contributing

New contributors are always welcome.
See [CONTRIBUTING.md](CONTRIBUTING.md) to get started.

## Building from source

```bash
git clone git@github.com:hibernate/hibernate-search.git
cd hibernate-search
mvn clean install
```

### Build options (profiles and properties)

#### Documentation
The documentation is based on [Asciidoctor](http://asciidoctor.org/). By default only the HTML
output is enabled; to also generate the PDF output use:

```bash
mvn clean install -Pdocumentation-pdf
```

You can then find the freshly built documentation in the following location:

```
./documentation/target/asciidoctor/en-US
```

#### Distribution

To build the distribution bundle run:

```bash
mvn clean install -Pdocumentation-pdf,dist
```

#### Elasticsearch

The Elasticsearch integration tests run against one single version of Elasticsearch at a time,
launching an Elasticsearch server automatically on port 9200.
You may redefine the version to use by specifying the right profile and using the
`test.elasticsearch.connection.version` property:

```bash
mvn clean install -Pelasticsearch-6.0 -Dtest.elasticsearch.connection.version=6.0.0
```

The following profiles are available:

 * `elasticsearch-5.6` for 5.6.x and later 5.x
 * `elasticsearch-6.0` for 6.0.x to 6.2.x
 * `elasticsearch-6.3` for 6.3.x
 * `elasticsearch-6.4` for 6.4.x to 6.6.x
 * `elasticsearch-6.7` for 6.7 and later 6.x
 * `elasticsearch-7.0` for 7.0 to 7.2
 * `elasticsearch-7.3` for 7.3+ (the default)

A list of available versions for `test.elasticsearch.connection.version` can be found on
[Maven Central](https://search.maven.org/search?q=g:org.elasticsearch%20AND%20a:elasticsearch&core=gav).

Alternatively, you can prevent the build from launching an Elasticsearch server automatically
and run Elasticsearch-related tests against your own server using the
`test.elasticsearch.connection.hosts` properties:

```bash
mvn clean install -Dtest.elasticsearch.connection.hosts=http://localhost:9200
```

If you want to run tests against a different Elasticsearch version  (6.x for instance),
you will still have to select a profile among those listed above, and specify the version:

```bash
mvn clean install -Pelasticsearch-6.0 -Dtest.elasticsearch.connection.version=6.0.0 -Dtest.elasticsearch.connection.hosts=http://localhost:9200
```

You may also use authentication:

```bash
mvn clean install -Dtest.elasticsearch.connection.hosts=https://localhost:9200 -Dtest.elasticsearch.connection.username=ironman -Dtest.elasticsearch.connection.password=j@rV1s
```

Also, the elasticsearch integration tests can be executed
against an Elasticsearch service on AWS.
You will need to execute something along the lines of:

```bash
mvn clean install -Dtest.elasticsearch.connection.hosts=<The full URL of your Elasticsearch endpoint> -Dtest.elasticsearch.connection.aws.signing.access_key=<Your access key> -Dtest.elasticsearch.connection.aws.signing.secret_key=<Your secret key> -Dtest.elasticsearch.connection.aws.signing.region=<Your AWS region ID>
```

When building Hibernate Search with new JDKs, you may want to run Elasticsearch with a different JDK than the one used by Maven.
This can be done by setting a property
(**this will only work with the profiles for Elasticsearch 5 and above**):

```bash
mvn clean install -Dtest.elasticsearch.run.java_home=/path/to/my/jdk
```

## JQAssistant

You can request static analysis and sanity checks with the `jqassistant` profile.
Tests do not need to be run for these checks.

```bash
mvn clean install -Pjqassistant -DskipTests
```

To also check cyclic dependencies between packages, use `-Djqassistant.groups=default,cycles`.
Cyclic dependency analysis is costly and may add significant overhead to the build:
at least 10 seconds, maybe one minute or more depending on your setup.

```bash
mvn clean install -Pjqassistant -DskipTests -Djqassistant.groups=default,cycles
```

You can also inspect the created Neo4j datastore after a build,
provided that build had the `jqassistant` profile enabled:

```bash
mvn jqassistant:server -Pjqassistant-server -pl reports
```

The Neo4j web UI will be accessible from http://localhost:7474/.

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
are executed against Elasticsearch 5.6 by default.
* `mapper`: The mappers, i.e. the modules that expose APIs to index and search user entities,
and do the work of converting between user entities and documents to be indexed.
  * `javabean`: An experimental (not published) mapper for Java Beans without Hibernate ORM.
  Mostly useful for tests of the `pojo` module.
  * `orm`: A mapper for [Hibernate ORM](http://hibernate.org/orm/) entities.
  * `pojo-base`: Contains base classes and APIs that are re-used in other POJO-based mapper.
* `reports`: Module built last, producing reports related to test coverage in particular.
* `util`: Various modules containing util classes, both for runtime and for tests.


