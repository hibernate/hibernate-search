/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.documentation.mapper.orm.indexing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.with;

import java.util.Arrays;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

import org.hibernate.search.documentation.testsupport.BackendConfigurations;
import org.hibernate.search.documentation.testsupport.DocumentationSetupHelper;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.cfg.HibernateOrmMapperSettings;
import org.hibernate.search.mapper.orm.mapping.SearchMapping;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.hibernate.search.mapper.orm.work.SearchIndexingPlan;
import org.hibernate.search.mapper.orm.work.SearchWorkspace;

import org.junit.Rule;
import org.junit.Test;

public class HibernateOrmManualIndexingIT {

	private static final int NUMBER_OF_BOOKS = 1000;
	private static final int BATCH_SIZE = 100;
	private static final int INIT_DATA_TRANSACTION_SIZE = 500;

	@Rule
	public DocumentationSetupHelper setupHelper = DocumentationSetupHelper.withSingleBackend( BackendConfigurations.simple() );

	@Test
	public void persist_automaticIndexing_periodicFlushClear() {
		EntityManagerFactory entityManagerFactory = setup( true );

		with( entityManagerFactory ).runNoTransaction( entityManager -> {
			assertBookCount( entityManager, 0 );

			// tag::persist-automatic-indexing-periodic-flush-clear[]
			entityManager.getTransaction().begin();
			try {
				for ( int i = 0 ; i < NUMBER_OF_BOOKS ; ++i ) { // <1>
					Book book = newBook( i );
					entityManager.persist( book ); // <2>

					if ( ( i + 1 ) % BATCH_SIZE == 0 ) {
						entityManager.flush(); // <3>
						entityManager.clear(); // <4>
					}
				}
				entityManager.getTransaction().commit();
			}
			catch (RuntimeException e) {
				entityManager.getTransaction().rollback();
				throw e;
			}
			// end::persist-automatic-indexing-periodic-flush-clear[]

			assertBookCount( entityManager, NUMBER_OF_BOOKS );
		} );
	}

	@Test
	public void persist_automaticIndexing_periodicFlushExecuteClear() {
		EntityManagerFactory entityManagerFactory = setup( true );

		with( entityManagerFactory ).runNoTransaction( entityManager -> {
			assertBookCount( entityManager, 0 );

			// tag::persist-automatic-indexing-periodic-flush-execute-clear[]
			SearchSession searchSession = Search.session( entityManager ); // <1>
			SearchIndexingPlan indexingPlan = searchSession.indexingPlan(); // <2>

			entityManager.getTransaction().begin();
			try {
				for ( int i = 0 ; i < NUMBER_OF_BOOKS ; ++i ) {
					Book book = newBook( i );
					entityManager.persist( book ); // <3>

					if ( ( i + 1 ) % BATCH_SIZE == 0 ) {
						entityManager.flush();
						entityManager.clear();
						indexingPlan.execute(); // <4>
					}
				}
				entityManager.getTransaction().commit(); // <5>
			}
			catch (RuntimeException e) {
				entityManager.getTransaction().rollback();
				throw e;
			}
			// end::persist-automatic-indexing-periodic-flush-execute-clear[]

			assertBookCount( entityManager, NUMBER_OF_BOOKS );
		} );
	}

	@Test
	public void persist_automaticIndexing_multipleTransactions() {
		EntityManagerFactory entityManagerFactory = setup( true );

		with( entityManagerFactory ).runNoTransaction( entityManager -> {
			assertBookCount( entityManager, 0 );

			// tag::persist-automatic-indexing-multiple-transactions[]
			try {
				int i = 0;
				while ( i < NUMBER_OF_BOOKS ) { // <1>
					entityManager.getTransaction().begin(); // <2>
					int end = Math.min( i + BATCH_SIZE, NUMBER_OF_BOOKS ); // <3>
					for ( ; i < end; ++i ) {
						Book book = newBook( i );
						entityManager.persist( book ); // <4>
					}
					entityManager.getTransaction().commit(); // <5>
				}
			}
			catch (RuntimeException e) {
				entityManager.getTransaction().rollback();
				throw e;
			}
			// end::persist-automatic-indexing-multiple-transactions[]

			assertBookCount( entityManager, NUMBER_OF_BOOKS );
		} );
	}

	@Test
	public void addOrUpdate() {
		int numberOfBooks = 10;
		EntityManagerFactory entityManagerFactory = setup( false );
		initBooksAndAuthors( entityManagerFactory, numberOfBooks );

		with( entityManagerFactory ).runInTransaction( entityManager -> {
			assertBookCount( entityManager, 0 );
		} );

		with( entityManagerFactory ).runInTransaction( entityManager -> {
			// tag::indexing-plan-addOrUpdate[]
			// Not shown: open a transaction if relevant

			SearchSession searchSession = /* ... */ // <1>
					// end::indexing-plan-addOrUpdate[]
					Search.session( entityManager );
			// tag::indexing-plan-addOrUpdate[]
			SearchIndexingPlan indexingPlan = searchSession.indexingPlan(); // <2>

			Book book = entityManager.getReference( Book.class, 5 ); // <3>

			indexingPlan.addOrUpdate( book ); // <4>

			// Not shown: commit the transaction or close the session if relevant
			// end::indexing-plan-addOrUpdate[]
		} );

		with( entityManagerFactory ).runInTransaction( entityManager -> {
			assertBookCount( entityManager, 1 );
		} );
	}

	@Test
	public void delete() {
		int numberOfBooks = 10;
		EntityManagerFactory entityManagerFactory = setup( true );
		initBooksAndAuthors( entityManagerFactory, numberOfBooks );

		with( entityManagerFactory ).runInTransaction( entityManager -> {
			assertBookCount( entityManager, numberOfBooks );
		} );

		with( entityManagerFactory ).runInTransaction( entityManager -> {
			// tag::indexing-plan-delete[]
			// Not shown: open a transaction if relevant

			SearchSession searchSession = /* ... */ // <1>
					// end::indexing-plan-delete[]
					Search.session( entityManager );
			// tag::indexing-plan-delete[]
			SearchIndexingPlan indexingPlan = searchSession.indexingPlan(); // <2>

			Book book = entityManager.getReference( Book.class, 5 ); // <3>

			indexingPlan.delete( book ); // <4>

			// Not shown: commit the transaction or close the session if relevant
			// end::indexing-plan-delete[]
		} );

		with( entityManagerFactory ).runInTransaction( entityManager -> {
			assertBookCount( entityManager, numberOfBooks - 1 );
		} );
	}

	@Test
	public void workspace() {
		int numberOfBooks = 10;
		EntityManagerFactory entityManagerFactory = setup( true );
		initBooksAndAuthors( entityManagerFactory, numberOfBooks );

		{
			// tag::workspace-retrieval-mapping[]
			SearchMapping searchMapping = /* ... */ // <1>
					// end::workspace-retrieval-mapping[]
					Search.mapping( entityManagerFactory );
			// tag::workspace-retrieval-mapping[]
			SearchWorkspace allEntitiesWorkspace = searchMapping.scope( Object.class ).workspace(); // <2>
			SearchWorkspace bookWorkspace = searchMapping.scope( Book.class ).workspace(); // <3>
			SearchWorkspace bookAndAuthorWorkspace = searchMapping.scope( Arrays.asList( Book.class, Author.class ) )
					.workspace(); // <4>
			// end::workspace-retrieval-mapping[]
		}

		with( entityManagerFactory ).runNoTransaction( entityManager -> {
			// tag::workspace-retrieval-session[]
			SearchSession searchSession = /* ... */ // <1>
					// end::workspace-retrieval-session[]
					Search.session( entityManager );
			// tag::workspace-retrieval-session[]
			SearchWorkspace allEntitiesWorkspace = searchSession.workspace(); // <2>
			SearchWorkspace bookWorkspace = searchSession.workspace( Book.class ); // <3>
			SearchWorkspace bookAndAuthorWorkspace = searchSession.workspace( Book.class, Author.class ); // <4>
			// end::workspace-retrieval-session[]
		} );

		with( entityManagerFactory ).runNoTransaction( entityManager -> {
			assertBookCount( entityManager, numberOfBooks );
			assertAuthorCount( entityManager, numberOfBooks );

			// tag::workspace-purge[]
			SearchSession searchSession = /* ... */ // <1>
					// end::workspace-purge[]
					Search.session( entityManager );
			// tag::workspace-purge[]
			SearchWorkspace workspace = searchSession.workspace( Book.class, Author.class ); // <2>
			workspace.purge(); // <3>
			// end::workspace-purge[]

			assertBookCount( entityManager, 0 );
			assertAuthorCount( entityManager, 0 );
		} );
	}

	private void initBooksAndAuthors(EntityManagerFactory entityManagerFactory, int numberOfBooks) {
		with( entityManagerFactory ).runNoTransaction( entityManager -> {
			try {
				int i = 0;
				while ( i < numberOfBooks ) {
					entityManager.getTransaction().begin();
					int end = Math.min( i + INIT_DATA_TRANSACTION_SIZE, numberOfBooks );
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

	private void assertBookCount(EntityManager entityManager, int expectedCount) {
		SearchSession searchSession = Search.session( entityManager );
		searchSession.workspace().refresh();
		assertThat(
				searchSession.search( Book.class )
						.where( f -> f.matchAll() )
						.fetchTotalHitCount()
		)
				.isEqualTo( expectedCount );
	}

	private void assertAuthorCount(EntityManager entityManager, int expectedCount) {
		SearchSession searchSession = Search.session( entityManager );
		searchSession.workspace().refresh();
		assertThat(
				searchSession.search( Author.class )
						.where( f -> f.matchAll() )
						.fetchTotalHitCount()
		)
				.isEqualTo( expectedCount );
	}

	private EntityManagerFactory setup(boolean automaticIndexingEnabled) {
		return setupHelper.start()
				.withProperty( HibernateOrmMapperSettings.AUTOMATIC_INDEXING_ENABLED, automaticIndexingEnabled )
				.setup( Book.class, Author.class );
	}

}
