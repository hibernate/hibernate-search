# Hibernate Search

[![Maven Central](https://img.shields.io/maven-central/v/org.hibernate.search/hibernate-search-mapper-orm.svg?label=Maven%20Central&style=for-the-badge)](https://search.maven.org/search?q=g:%22org.hibernate.search%22)
[![Build Status](https://img.shields.io/jenkins/build?jobUrl=https%3A%2F%2Fci.hibernate.org%2Fjob%2Fhibernate-search%2Fjob%2Fmain%2F&style=for-the-badge)](https://ci.hibernate.org/job/hibernate-search/job/main)
[![Sonar Coverage](https://img.shields.io/sonar/coverage/org.hibernate.search:hibernate-search-parent?server=https%3A%2F%2Fsonarcloud.io&style=for-the-badge)](https://sonarcloud.io/project/activity?id=org.hibernate.search%3Ahibernate-search-parent&graph=coverage)
[![Quality gate](https://img.shields.io/sonar/alert_status/org.hibernate.search:hibernate-search-parent?logo=sonarcloud&server=https%3A%2F%2Fsonarcloud.io&style=for-the-badge)](https://sonarcloud.io/dashboard?id=org.hibernate.search%3Ahibernate-search-parent)

## Description

Hibernate Search automatically extracts data from Hibernate ORM entities to push it to
local [Apache Lucene](http://lucene.apache.org/) indexes
or remote [Elasticsearch](https://www.elastic.co/products/elasticsearch) indexes.

It features:

* [**Declarative mapping**](https://docs.jboss.org/hibernate/stable/search/reference/en-US/html_single/#mapper-orm-mapping)
of entity properties to index fields,
either through annotations or a programmatic API.
* [**On-demand mass indexing**](https://docs.jboss.org/hibernate/stable/search/reference/en-US/html_single/#mapper-orm-indexing-massindexer)
of all entities in the database,
to initialize the indexes with pre-existing data.
* [**On-the-fly automatic indexing**](https://docs.jboss.org/hibernate/stable/search/reference/en-US/html_single/#mapper-orm-indexing-automatic)
of entities modified through a Hibernate ORM session,
to always keep the indexes up-to-date.
* [**A Search DSL**](https://docs.jboss.org/hibernate/stable/search/reference/en-US/html_single/#search-dsl)
to easily build full-text search queries
and retrieve the hits as Hibernate ORM entities.
* And more: [configuration of analyzers](https://docs.jboss.org/hibernate/stable/search/reference/en-US/html_single/#concepts-analysis),
many different [predicates](https://docs.jboss.org/hibernate/stable/search/reference/en-US/html_single/#search-dsl-predicate)
and [sorts](https://docs.jboss.org/hibernate/stable/search/reference/en-US/html_single/#search-dsl-sort)
in the Search DSL,
[spatial support](https://docs.jboss.org/hibernate/stable/search/reference/en-US/html_single/#mapper-orm-geopoint).
search queries returning [projections](https://docs.jboss.org/hibernate/stable/search/reference/en-US/html_single/#search-dsl-projection)
instead of entities,
[aggregations](https://docs.jboss.org/hibernate/stable/search/reference/en-US/html_single/#search-dsl-aggregation),
advanced customization of the mapping using [bridges](https://docs.jboss.org/hibernate/stable/search/reference/en-US/html_single/#mapper-orm-bridge),
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
    @FullTextField
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
    @FullTextField
    private String name;

    @ManyToMany(mappedBy = "authors")
    private Set<Book> books = new HashSet<>();

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
                .matching( "Isaac" ) )
        .fetch( 20 );

List<Book> hits = result.hits();
long totalHitCount = result.total().hitCount();
```

## License

This software and its documentation are distributed under the terms of
the GNU Lesser General Public License (LGPL), version 2.1 or later.

See the `lgpl.txt` file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.

## Getting started

A getting started guide is available
[in the reference documentation](https://docs.jboss.org/hibernate/stable/search/reference/en-US/html_single/#getting-started).

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

The contribution guide also includes build instructions. 
