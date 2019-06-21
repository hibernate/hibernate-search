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

import org.hibernate.search.documentation.testsupport.BackendSetupStrategy;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.cfg.HibernateOrmAutomaticIndexingStrategyName;
import org.hibernate.search.mapper.orm.cfg.HibernateOrmMapperSettings;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.hibernate.search.mapper.orm.session.SearchSessionWritePlan;
import org.hibernate.search.mapper.orm.writing.SearchWriter;
import org.hibernate.search.util.impl.integrationtest.orm.OrmSetupHelper;
import org.hibernate.search.util.impl.integrationtest.orm.OrmUtils;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class HibernateOrmExplicitIndexingIT {

	private static final int NUMBER_OF_BOOKS = 1000;
	private static final int BATCH_SIZE = 100;
	private static final int INIT_DATA_TRANSACTION_SIZE = 500;

	@Parameterized.Parameters(name = "{0}")
	public static Object[] backendSetups() {
		return BackendSetupStrategy.simple().toArray();
	}

	@Rule
	public OrmSetupHelper setupHelper = new OrmSetupHelper();

	private final BackendSetupStrategy backendSetupStrategy;

	public HibernateOrmExplicitIndexingIT(BackendSetupStrategy backendSetupStrategy) {
		this.backendSetupStrategy = backendSetupStrategy;
	}

	@Test
	public void persist_noAutomaticIndexing() {
		EntityManagerFactory entityManagerFactory = setup( HibernateOrmAutomaticIndexingStrategyName.NONE );

		OrmUtils.withinEntityManager( entityManagerFactory, entityManager -> {
			assertBookCount( entityManager, 0 );

			// tag::persist-no-automatic-indexing[]
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
			}
			// end::persist-no-automatic-indexing[]

			assertBookCount( entityManager, 0 );
		} );
	}

	@Test
	public void persist_automaticIndexing_periodicProcess() {
		EntityManagerFactory entityManagerFactory = setup( HibernateOrmAutomaticIndexingStrategyName.SESSION );

		OrmUtils.withinEntityManager( entityManagerFactory, entityManager -> {
			assertBookCount( entityManager, 0 );

			// tag::persist-automatic-indexing-periodic-process[]
			SearchSession searchSession = Search.session( entityManager ); // <1>
			SearchSessionWritePlan searchWritePlan = searchSession.writePlan(); // <2>

			entityManager.getTransaction().begin();
			try {
				for ( int i = 0 ; i < NUMBER_OF_BOOKS ; ++i ) {
					Book book = newBook( i );
					entityManager.persist( book ); // <3>

					if ( ( i + 1 ) % BATCH_SIZE == 0 ) {
						entityManager.flush();
						searchWritePlan.process(); // <4>
						entityManager.clear();
					}
				}
				entityManager.getTransaction().commit(); // <5>
			}
			catch (RuntimeException e) {
				entityManager.getTransaction().rollback();
			}
			// end::persist-automatic-indexing-periodic-process[]

			assertBookCount( entityManager, NUMBER_OF_BOOKS );
		} );
	}

	@Test
	public void persist_automaticIndexing_periodicExecute() {
		EntityManagerFactory entityManagerFactory = setup( HibernateOrmAutomaticIndexingStrategyName.SESSION );

		OrmUtils.withinEntityManager( entityManagerFactory, entityManager -> {
			assertBookCount( entityManager, 0 );

			// tag::persist-automatic-indexing-periodic-execute[]
			SearchSession searchSession = Search.session( entityManager ); // <1>
			SearchSessionWritePlan searchWritePlan = searchSession.writePlan(); // <2>

			entityManager.getTransaction().begin();
			try {
				for ( int i = 0 ; i < NUMBER_OF_BOOKS ; ++i ) {
					Book book = newBook( i );
					entityManager.persist( book ); // <3>

					if ( ( i + 1 ) % BATCH_SIZE == 0 ) {
						entityManager.flush();
						searchWritePlan.execute(); // <4>
						entityManager.clear();
					}
				}
				entityManager.getTransaction().commit(); // <5>
			}
			catch (RuntimeException e) {
				entityManager.getTransaction().rollback();
			}
			// end::persist-automatic-indexing-periodic-execute[]

			assertBookCount( entityManager, NUMBER_OF_BOOKS );
		} );
	}

	@Test
	public void persist_automaticIndexing_multipleTransactions() {
		EntityManagerFactory entityManagerFactory = setup( HibernateOrmAutomaticIndexingStrategyName.SESSION );

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
			}
			// end::persist-automatic-indexing-multiple-transactions[]

			assertBookCount( entityManager, NUMBER_OF_BOOKS );
		} );
	}

	@Test
	public void addOrUpdate() {
		int numberOfBooks = 10;
		EntityManagerFactory entityManagerFactory = setup( HibernateOrmAutomaticIndexingStrategyName.NONE );
		initBooksAndAuthors( entityManagerFactory, numberOfBooks );

		OrmUtils.withinEntityManager( entityManagerFactory, entityManager -> {
			assertBookCount( entityManager, 0 );

			// tag::write-plan-addOrUpdate[]
			SearchSession searchSession = Search.session( entityManager ); // <1>
			SearchSessionWritePlan searchWritePlan = searchSession.writePlan(); // <2>

			entityManager.getTransaction().begin();
			try {
				Book book = entityManager.getReference( Book.class, 5 ); // <3>

				searchWritePlan.addOrUpdate( book ); // <4>

				entityManager.getTransaction().commit(); // <5>
			}
			catch (RuntimeException e) {
				entityManager.getTransaction().rollback();
			}
			// end::write-plan-addOrUpdate[]

			assertBookCount( entityManager, 1 );
		} );
	}

	@Test
	public void delete() {
		int numberOfBooks = 10;
		EntityManagerFactory entityManagerFactory = setup( HibernateOrmAutomaticIndexingStrategyName.SESSION );
		initBooksAndAuthors( entityManagerFactory, numberOfBooks );

		OrmUtils.withinEntityManager( entityManagerFactory, entityManager -> {
			assertBookCount( entityManager, numberOfBooks );

			// tag::write-plan-delete[]
			SearchSession searchSession = Search.session( entityManager ); // <1>
			SearchSessionWritePlan searchWritePlan = searchSession.writePlan(); // <2>

			entityManager.getTransaction().begin();
			try {
				Book book = entityManager.getReference( Book.class, 5 ); // <3>

				searchWritePlan.delete( book ); // <4>

				entityManager.getTransaction().commit(); // <5>
			}
			catch (RuntimeException e) {
				entityManager.getTransaction().rollback();
			}
			// end::write-plan-delete[]

			assertBookCount( entityManager, numberOfBooks - 1 );
		} );
	}

	@Test
	public void writer() {
		int numberOfBooks = 10;
		EntityManagerFactory entityManagerFactory = setup( HibernateOrmAutomaticIndexingStrategyName.SESSION );
		initBooksAndAuthors( entityManagerFactory, numberOfBooks );

		OrmUtils.withinEntityManager( entityManagerFactory, entityManager -> {
			// tag::writer-retrieval[]
			SearchSession searchSession = Search.session( entityManager ); // <1>
			SearchWriter writer1 = searchSession.writer(); // <2>
			SearchWriter writer2 = searchSession.writer( Book.class ); // <3>
			SearchWriter writer3 = searchSession.writer( Book.class, Author.class ); // <4>
			// end::writer-retrieval[]
		} );

		OrmUtils.withinEntityManager( entityManagerFactory, entityManager -> {
			assertBookCount( entityManager, numberOfBooks );
			assertAuthorCount( entityManager, numberOfBooks );

			// tag::writer-purge[]
			SearchSession searchSession = Search.session( entityManager ); // <1>
			SearchWriter writer = searchSession.writer( Book.class, Author.class ); // <2>
			writer.purge(); // <3>
			// end::writer-purge[]

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
		searchSession.writer().flush();
		assertThat(
				searchSession.search( Book.class )
						.predicate( f -> f.matchAll() )
						.fetchTotalHitCount()
		)
				.isEqualTo( expectedCount );
	}

	private void assertAuthorCount(EntityManager entityManager, int expectedCount) {
		SearchSession searchSession = Search.session( entityManager );
		// Ensure every committed work is searchable: flush also executes a refresh on Elasticsearch
		searchSession.writer().flush();
		assertThat(
				searchSession.search( Author.class )
						.predicate( f -> f.matchAll() )
						.fetchTotalHitCount()
		)
				.isEqualTo( expectedCount );
	}

	private EntityManagerFactory setup(HibernateOrmAutomaticIndexingStrategyName automaticIndexingStrategy) {
		return backendSetupStrategy.withSingleBackend( setupHelper )
				.withProperty(
						HibernateOrmMapperSettings.AUTOMATIC_INDEXING_STRATEGY,
						automaticIndexingStrategy
				)
				.setup( Book.class, Author.class );
	}

}
