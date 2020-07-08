/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.documentation.mapper.orm.indexing;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CompletableFuture;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import org.hibernate.search.documentation.testsupport.BackendConfigurations;
import org.hibernate.search.documentation.testsupport.DocumentationSetupHelper;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.automaticindexing.AutomaticIndexingStrategyName;
import org.hibernate.search.mapper.orm.cfg.HibernateOrmMapperSettings;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendConfiguration;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class HibernateOrmMassIndexerIT {

	private static final int NUMBER_OF_BOOKS = 1000;
	private static final int INIT_DATA_TRANSACTION_SIZE = 500;

	@Parameterized.Parameters(name = "{0}")
	public static Object[] backendConfigurations() {
		return BackendConfigurations.simple().toArray();
	}

	@Rule
	public DocumentationSetupHelper setupHelper;

	private EntityManagerFactory entityManagerFactory;

	public HibernateOrmMassIndexerIT(BackendConfiguration backendConfiguration) {
		this.setupHelper = DocumentationSetupHelper.withSingleBackend( backendConfiguration );
	}

	@Before
	public void setup() {
		this.entityManagerFactory = setupHelper.start()
				.withProperty(
						HibernateOrmMapperSettings.AUTOMATIC_INDEXING_STRATEGY,
						AutomaticIndexingStrategyName.NONE
				)
				.setup( Book.class, Author.class );
		initData();
	}

	@Test
	public void simple() {
		OrmUtils.withinEntityManager( entityManagerFactory, entityManager -> {
			try {
				// tag::simple[]
				SearchSession searchSession = Search.session( entityManager ); // <1>
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
	public void selectType() {
		OrmUtils.withinEntityManager( entityManagerFactory, entityManager -> {
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
	public void async() {
		OrmUtils.withinEntityManager( entityManagerFactory, entityManager -> {
			SearchSession searchSession = Search.session( entityManager );
			// tag::async[]
			CompletableFuture<?> future = searchSession.massIndexer() // <1>
					.start(); // <2>
			// end::async[]
			future.join();
			assertBookCount( entityManager, NUMBER_OF_BOOKS );
			assertAuthorCount( entityManager, NUMBER_OF_BOOKS );
		} );
	}

	@Test
	public void parameters() {
		OrmUtils.withinEntityManager( entityManagerFactory, entityManager -> {
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

	private void assertBookCount(EntityManager entityManager, int expectedCount) {
		SearchSession searchSession = Search.session( entityManager );
		assertThat(
				searchSession.search( Book.class )
						.where( f -> f.matchAll() )
						.fetchTotalHitCount()
		)
				.isEqualTo( expectedCount );
	}

	private void assertAuthorCount(EntityManager entityManager, int expectedCount) {
		SearchSession searchSession = Search.session( entityManager );
		assertThat(
				searchSession.search( Author.class )
						.where( f -> f.matchAll() )
						.fetchTotalHitCount()
		)
				.isEqualTo( expectedCount );
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
		OrmUtils.withinEntityManager( entityManagerFactory, entityManager -> {
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
