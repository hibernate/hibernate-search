/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.documentation.mapper.orm.indexing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.with;
import static org.hibernate.search.util.impl.test.FutureAssert.assertThatFuture;

import java.lang.invoke.MethodHandles;
import java.time.LocalDate;
import java.util.concurrent.Future;
import java.util.function.Function;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

import org.hibernate.search.documentation.testsupport.BackendConfigurations;
import org.hibernate.search.documentation.testsupport.DocumentationSetupHelper;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.cfg.HibernateOrmMapperSettings;
import org.hibernate.search.mapper.orm.massindexing.MassIndexer;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.hibernate.search.util.common.logging.impl.Log;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class HibernateOrmMassIndexerIT {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	static final int NUMBER_OF_BOOKS = 1000;
	static final int INIT_DATA_TRANSACTION_SIZE = 500;

	@Rule
	public DocumentationSetupHelper setupHelper = DocumentationSetupHelper.withSingleBackend( BackendConfigurations.simple() );

	private EntityManagerFactory entityManagerFactory;

	@Before
	public void setup() {
		this.entityManagerFactory = setupHelper.start()
				.withProperty( HibernateOrmMapperSettings.INDEXING_LISTENERS_ENABLED, false )
				.setup( Book.class, Author.class );
		initData( entityManagerFactory, HibernateOrmMassIndexerIT::newAuthor );
		with( entityManagerFactory ).runNoTransaction( entityManager -> {
			assertBookCount( entityManager, 0 );
			assertAuthorCount( entityManager, 0 );
		} );
	}

	@Test
	public void simple() {
		with( entityManagerFactory ).runNoTransaction( entityManager -> {
			try {
				// tag::simple[]
				SearchSession searchSession = /* ... */ // <1>
						// end::simple[]
						Search.session( entityManager );
				// tag::simple[]
				searchSession.massIndexer() // <2>
						.startAndWait(); // <3>
				// end::simple[]
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
			assertBookCount( entityManager, NUMBER_OF_BOOKS );
			assertAuthorCount( entityManager, NUMBER_OF_BOOKS );
		} );
	}

	@Test
	public void reindexOnly() {
		with( entityManagerFactory ).runNoTransaction( entityManager -> {
			try {
				// tag::reindexOnly[]
				SearchSession searchSession = /* ... */ // <1>
						// end::reindexOnly[]
						Search.session( entityManager );
				// tag::reindexOnly[]
				MassIndexer massIndexer = searchSession.massIndexer(); // <2>
				massIndexer.type( Book.class ).reindexOnly( "e.publicationYear <= 2100" ); // <3>
				massIndexer.type( Author.class ).reindexOnly( "e.birthDate < :birthDate" ) // <4>
						.param( "birthDate", LocalDate.ofYearDay( 2100, 77 ) ); // <5>
				massIndexer.startAndWait(); // <6>
				// end::reindexOnly[]
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
			assertBookCount( entityManager, 651 );
			assertAuthorCount( entityManager, 651 );
		} );
	}

	@Test
	public void selectType() {
		with( entityManagerFactory ).runNoTransaction( entityManager -> {
			try {
				SearchSession searchSession = Search.session( entityManager );
				// tag::select-type[]
				searchSession.massIndexer( Book.class ) // <1>
						.startAndWait(); // <2>
				// end::select-type[]
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
			assertBookCount( entityManager, NUMBER_OF_BOOKS );
			assertAuthorCount( entityManager, 0 );
		} );
	}

	@Test
	public void async_reactive() {
		with( entityManagerFactory ).runNoTransaction( entityManager -> {
			SearchSession searchSession = Search.session( entityManager );
			// tag::async[]
			searchSession.massIndexer() // <1>
					.start() // <2>
					.thenRun( () -> { // <3>
						log.info( "Mass indexing succeeded!" );
					} )
					.exceptionally( throwable -> {
						log.error( "Mass indexing failed!", throwable );
						return null;
					} );
			// end::async[]
			await().untilAsserted( () -> {
				assertBookCount( entityManager, NUMBER_OF_BOOKS );
				assertAuthorCount( entityManager, NUMBER_OF_BOOKS );
			} );
		} );
	}

	@Test
	public void async_future() {
		with( entityManagerFactory ).runNoTransaction( entityManager -> {
			SearchSession searchSession = Search.session( entityManager );
			// tag::async[]

			// OR
			Future<?> future = searchSession.massIndexer().start()
					.toCompletableFuture(); // <4>
			// end::async[]
			await().untilAsserted( () -> {
				assertBookCount( entityManager, NUMBER_OF_BOOKS );
				assertAuthorCount( entityManager, NUMBER_OF_BOOKS );
				assertThatFuture( future ).isSuccessful();
			} );
		} );
	}

	@Test
	public void parameters() {
		with( entityManagerFactory ).runNoTransaction( entityManager -> {
			try {
				SearchSession searchSession = Search.session( entityManager );
				// tag::parameters[]
				searchSession.massIndexer() // <1>
						.idFetchSize( 150 ) // <2>
						.batchSizeToLoadObjects( 25 ) // <3>
						.threadsToLoadObjects( 12 ) // <4>
						.startAndWait(); // <5>
				// end::parameters[]
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
			assertBookCount( entityManager, NUMBER_OF_BOOKS );
			assertAuthorCount( entityManager, NUMBER_OF_BOOKS );
		} );
	}

	static void assertBookCount(EntityManager entityManager, int expectedCount) {
		SearchSession searchSession = Search.session( entityManager );
		assertThat(
				searchSession.search( Book.class )
						.where( f -> f.matchAll() )
						.fetchTotalHitCount()
		)
				.isEqualTo( expectedCount );
	}

	static void assertAuthorCount(EntityManager entityManager, int expectedCount) {
		SearchSession searchSession = Search.session( entityManager );
		assertThat(
				searchSession.search( Author.class )
						.where( f -> f.matchAll() )
						.fetchTotalHitCount()
		)
				.isEqualTo( expectedCount );
	}

	static void initData(EntityManagerFactory entityManagerFactory, Function<Integer, Author> authorInit) {
		with( entityManagerFactory ).runNoTransaction( entityManager -> {
			try {
				int i = 0;
				while ( i < NUMBER_OF_BOOKS ) {
					entityManager.getTransaction().begin();
					int end = Math.min( i + INIT_DATA_TRANSACTION_SIZE, NUMBER_OF_BOOKS );
					for ( ; i < end; ++i ) {
						Author author = authorInit.apply( i );

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

	private static Book newBook(int id) {
		Book book = new Book();
		book.setId( id );
		book.setTitle( "This is the title of book #" + id );
		book.setPublicationYear( 1450 + id );
		return book;
	}

	private static Author newAuthor(int id) {
		Author author = new Author();
		author.setId( id );
		author.setFirstName( "John" + id );
		author.setLastName( "Smith" + id );
		author.setBirthDate( LocalDate.ofYearDay( 1450 + id, 33 ) );
		return author;
	}
}
