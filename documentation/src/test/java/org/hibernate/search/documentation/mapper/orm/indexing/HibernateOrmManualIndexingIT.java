/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.documentation.mapper.orm.indexing;

import static org.assertj.core.api.Assertions.assertThat;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import org.hibernate.search.documentation.testsupport.BackendConfigurations;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.automaticindexing.AutomaticIndexingStrategyName;
import org.hibernate.search.mapper.orm.cfg.HibernateOrmMapperSettings;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.hibernate.search.mapper.orm.work.SearchIndexingPlan;
import org.hibernate.search.mapper.orm.work.SearchWorkspace;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendConfiguration;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class HibernateOrmManualIndexingIT {

	private static final int NUMBER_OF_BOOKS = 1000;
	private static final int BATCH_SIZE = 100;
	private static final int INIT_DATA_TRANSACTION_SIZE = 500;

	@Parameterized.Parameters(name = "{0}")
	public static Object[] backendConfigurations() {
		return BackendConfigurations.simple().toArray();
	}

	@Rule
	public OrmSetupHelper setupHelper;

	public HibernateOrmManualIndexingIT(BackendConfiguration backendConfiguration) {
		this.setupHelper = OrmSetupHelper.withSingleBackend( backendConfiguration );
	}

	@Test
	public void persist_automaticIndexing_periodicFlushClear() {
		EntityManagerFactory entityManagerFactory = setup( AutomaticIndexingStrategyName.SESSION );

		OrmUtils.withinEntityManager( entityManagerFactory, entityManager -> {
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
		EntityManagerFactory entityManagerFactory = setup( AutomaticIndexingStrategyName.SESSION );

		OrmUtils.withinEntityManager( entityManagerFactory, entityManager -> {
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
		EntityManagerFactory entityManagerFactory = setup( AutomaticIndexingStrategyName.SESSION );

		OrmUtils.withinEntityManager( entityManagerFactory, entityManager -> {
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
		EntityManagerFactory entityManagerFactory = setup( AutomaticIndexingStrategyName.NONE );
		initBooksAndAuthors( entityManagerFactory, numberOfBooks );

		OrmUtils.withinEntityManager( entityManagerFactory, entityManager -> {
			assertBookCount( entityManager, 0 );

			// tag::indexing-plan-addOrUpdate[]
			SearchSession searchSession = Search.session( entityManager ); // <1>
			SearchIndexingPlan indexingPlan = searchSession.indexingPlan(); // <2>

			entityManager.getTransaction().begin();
			try {
				Book book = entityManager.getReference( Book.class, 5 ); // <3>

				indexingPlan.addOrUpdate( book ); // <4>

				entityManager.getTransaction().commit(); // <5>
			}
			catch (RuntimeException e) {
				entityManager.getTransaction().rollback();
				throw e;
			}
			// end::indexing-plan-addOrUpdate[]

			assertBookCount( entityManager, 1 );
		} );
	}

	@Test
	public void delete() {
		int numberOfBooks = 10;
		EntityManagerFactory entityManagerFactory = setup( AutomaticIndexingStrategyName.SESSION );
		initBooksAndAuthors( entityManagerFactory, numberOfBooks );

		OrmUtils.withinEntityManager( entityManagerFactory, entityManager -> {
			assertBookCount( entityManager, numberOfBooks );

			// tag::indexing-plan-delete[]
			SearchSession searchSession = Search.session( entityManager ); // <1>
			SearchIndexingPlan indexingPlan = searchSession.indexingPlan(); // <2>

			entityManager.getTransaction().begin();
			try {
				Book book = entityManager.getReference( Book.class, 5 ); // <3>

				indexingPlan.delete( book ); // <4>

				entityManager.getTransaction().commit(); // <5>
			}
			catch (RuntimeException e) {
				entityManager.getTransaction().rollback();
				throw e;
			}
			// end::indexing-plan-delete[]

			assertBookCount( entityManager, numberOfBooks - 1 );
		} );
	}

	@Test
	public void workspace() {
		int numberOfBooks = 10;
		EntityManagerFactory entityManagerFactory = setup( AutomaticIndexingStrategyName.SESSION );
		initBooksAndAuthors( entityManagerFactory, numberOfBooks );

		OrmUtils.withinEntityManager( entityManagerFactory, entityManager -> {
			// tag::workspace-retrieval[]
			SearchSession searchSession = Search.session( entityManager ); // <1>
			SearchWorkspace workspace1 = searchSession.workspace(); // <2>
			SearchWorkspace workspace2 = searchSession.workspace( Book.class ); // <3>
			SearchWorkspace workspace3 = searchSession.workspace( Book.class, Author.class ); // <4>
			// end::workspace-retrieval[]
		} );

		OrmUtils.withinEntityManager( entityManagerFactory, entityManager -> {
			assertBookCount( entityManager, numberOfBooks );
			assertAuthorCount( entityManager, numberOfBooks );

			// tag::workspace-purge[]
			SearchSession searchSession = Search.session( entityManager ); // <1>
			SearchWorkspace workspace = searchSession.workspace( Book.class, Author.class ); // <2>
			workspace.purge(); // <3>
			// end::workspace-purge[]

			assertBookCount( entityManager, 0 );
			assertAuthorCount( entityManager, 0 );
		} );
	}

	private void initBooksAndAuthors(EntityManagerFactory entityManagerFactory, int numberOfBooks) {
		OrmUtils.withinEntityManager( entityManagerFactory, entityManager -> {
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
		// Ensure every committed work is searchable: flush also executes a refresh on Elasticsearch
		searchSession.workspace().flush();
		assertThat(
				searchSession.search( Book.class )
						.where( f -> f.matchAll() )
						.fetchTotalHitCount()
		)
				.isEqualTo( expectedCount );
	}

	private void assertAuthorCount(EntityManager entityManager, int expectedCount) {
		SearchSession searchSession = Search.session( entityManager );
		// Ensure every committed work is searchable: flush also executes a refresh on Elasticsearch
		searchSession.workspace().flush();
		assertThat(
				searchSession.search( Author.class )
						.where( f -> f.matchAll() )
						.fetchTotalHitCount()
		)
				.isEqualTo( expectedCount );
	}

	private EntityManagerFactory setup(AutomaticIndexingStrategyName automaticIndexingStrategy) {
		return setupHelper.start()
				.withProperty(
						HibernateOrmMapperSettings.AUTOMATIC_INDEXING_STRATEGY,
						automaticIndexingStrategy
				)
				.setup( Book.class, Author.class );
	}

}
