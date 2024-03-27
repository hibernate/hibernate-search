/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.documentation.mapper.orm.schema.management;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.with;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

import org.hibernate.search.documentation.testsupport.BackendConfigurations;
import org.hibernate.search.documentation.testsupport.DocumentationSetupHelper;
import org.hibernate.search.engine.common.schema.management.SchemaExport;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.cfg.HibernateOrmMapperSettings;
import org.hibernate.search.mapper.orm.schema.management.SchemaManagementStrategyName;
import org.hibernate.search.mapper.orm.schema.management.SearchSchemaManager;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.hibernate.search.mapper.pojo.schema.management.SearchSchemaCollector;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;

class HibernateOrmSchemaManagerIT {

	private static final int NUMBER_OF_BOOKS = 200;
	private static final int INIT_DATA_TRANSACTION_SIZE = 100;

	@RegisterExtension
	public DocumentationSetupHelper setupHelper = DocumentationSetupHelper.withSingleBackend( BackendConfigurations.simple() );

	@TempDir
	public Path temporaryFolder;

	private EntityManagerFactory entityManagerFactory;

	@BeforeEach
	void setup() {
		this.entityManagerFactory = setupHelper.start()
				.withProperty( HibernateOrmMapperSettings.SCHEMA_MANAGEMENT_STRATEGY,
						SchemaManagementStrategyName.NONE )
				.withProperty( HibernateOrmMapperSettings.INDEXING_LISTENERS_ENABLED, false )
				.setup( Book.class, Author.class );
		initData();
	}

	@Test
	void simple() {
		with( entityManagerFactory ).runNoTransaction( entityManager -> {
			try {
				// tag::simple[]
				SearchSession searchSession = /* ... */ // <1>
						// end::simple[]
						Search.session( entityManager );
				// tag::simple[]
				SearchSchemaManager schemaManager = searchSession.schemaManager(); // <2>
				schemaManager.dropAndCreate(); // <3>
				searchSession.massIndexer()
						.purgeAllOnStart( false )
						.startAndWait(); // <4>
				// end::simple[]
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
			assertBookAndAuthorCount( entityManager, NUMBER_OF_BOOKS, NUMBER_OF_BOOKS );
		} );
	}

	@Test
	void selectType() {
		with( entityManagerFactory ).runNoTransaction( entityManager -> {
			try {
				SearchSession searchSession = Search.session( entityManager );
				// tag::select-type[]
				SearchSchemaManager schemaManager = searchSession.schemaManager( Book.class ); // <1>
				schemaManager.dropAndCreate(); // <2>
				// end::select-type[]
				searchSession.massIndexer( Book.class )
						.purgeAllOnStart( false )
						.startAndWait();
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
			assertBookAndAuthorCount( entityManager, NUMBER_OF_BOOKS, null );
		} );
	}

	@Test
	void walkingTheSchema() {
		with( entityManagerFactory ).runNoTransaction( entityManager -> {
			SearchSession searchSession = Search.session( entityManager );
			List<String> indexNames = new ArrayList<>();
			// tag::walking-the-schema[]
			SearchSchemaManager schemaManager = searchSession.schemaManager(); // <1>
			schemaManager.exportExpectedSchema(
					new SearchSchemaCollector() { // <2>
						@Override
						public void indexSchema(Optional<String> backendName, String indexName, SchemaExport export) {
							String name = backendName.orElse( "default" ) + ":" + indexName; // <3>
							// perform any other actions with an index schema export
							// end::walking-the-schema[]
							indexNames.add( indexName );
							// tag::walking-the-schema[]
						}
					}
			);
			// end::walking-the-schema[]
			assertThat( indexNames ).containsOnly( Book.class.getSimpleName(), Author.class.getSimpleName() );
		} );
	}

	@Test
	void exportSchemaToFiles() throws IOException {
		with( entityManagerFactory ).runNoTransaction( entityManager -> {
			SearchSession searchSession = Search.session( entityManager );
			Path targetDirectory = temporaryFolder;

			// tag::schema-export[]
			SearchSchemaManager schemaManager = searchSession.schemaManager(); // <1>
			schemaManager.exportExpectedSchema( targetDirectory ); // <2>
			// end::schema-export[]

			assertThat( Files.list( targetDirectory.resolve( "backend" )
					.resolve( "indexes" )
					.resolve( Book.class.getSimpleName() ) )
					.map( f -> f.getFileName().toString() )
					.collect( Collectors.toSet() ) )
					.containsAnyOf(
							"create-index.json",
							"create-index-query-params.json",
							"no-schema.txt"
					);

			assertThat( Files.list( targetDirectory.resolve( "backend" )
					.resolve( "indexes" )
					.resolve( Author.class.getSimpleName() ) )
					.map( f -> f.getFileName().toString() )
					.collect( Collectors.toSet() ) )
					.containsAnyOf(
							"create-index.json",
							"create-index-query-params.json",
							"no-schema.txt"
					);
		} );
	}

	void assertBookAndAuthorCount(EntityManager entityManager, int expectedBookCount, Integer expectedAuthorCount) {
		setupHelper.assertions().searchAfterIndexChangesAndPotentialRefresh( () -> {
			SearchSession searchSession = Search.session( entityManager );
			assertThat( searchSession.search( Book.class )
					.where( f -> f.matchAll() )
					.fetchTotalHitCount() )
					.isEqualTo( expectedBookCount );
			if ( expectedAuthorCount != null ) {
				assertThat( searchSession.search( Author.class )
						.where( f -> f.matchAll() )
						.fetchTotalHitCount() )
						.isEqualTo( (int) expectedAuthorCount );
			}
		} );
	}

	private Book newBook(int id) {
		Book book = new Book();
		book.setId( id );
		book.setTitle( "This is the title of book #" + id );
		return book;
	}

	private Author newAuthor(int id) {
		Author author = new Author();
		author.setId( id );
		author.setFirstName( "John" + id );
		author.setLastName( "Smith" + id );
		return author;
	}

	private void initData() {
		with( entityManagerFactory ).runNoTransaction( entityManager -> {
			try {
				int i = 0;
				while ( i < NUMBER_OF_BOOKS ) {
					entityManager.getTransaction().begin();
					int end = Math.min( i + INIT_DATA_TRANSACTION_SIZE, NUMBER_OF_BOOKS );
					for ( ; i < end; ++i ) {
						Author author = newAuthor( i );

						Book book = newBook( i );
						book.setAuthor( author );
						author.getBooks().add( book );

						entityManager.persist( author );
						entityManager.persist( book );
					}
					entityManager.getTransaction().commit();
				}
			}
			catch (RuntimeException e) {
				entityManager.getTransaction().rollback();
				throw e;
			}
		} );
	}

}
