/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.documentation.mapper.orm.indexing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.search.util.common.impl.CollectionHelper.asSet;
import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.with;

import java.time.LocalDate;
import java.util.function.Function;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.search.documentation.testsupport.BackendConfigurations;
import org.hibernate.search.documentation.testsupport.DocumentationSetupHelper;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.cfg.HibernateOrmMapperSettings;
import org.hibernate.search.mapper.orm.mapping.SearchMapping;
import org.hibernate.search.mapper.orm.session.SearchSession;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class HibernateOrmMassIndexerMultiTenancyIT {

	private static final String TENANT_1_ID = "tenant1";
	private static final String TENANT_2_ID = "tenant2";
	private static final String TENANT_3_ID = "tenant3";

	private static final int NUMBER_OF_BOOKS = 1000;
	private static final int INIT_DATA_TRANSACTION_SIZE = 500;

	@RegisterExtension
	public DocumentationSetupHelper setupHelper = DocumentationSetupHelper.withSingleBackend( BackendConfigurations.simple() );

	private SessionFactory sessionFactory;

	@BeforeEach
	void setup() {
		this.sessionFactory = setupHelper.start()
				.withProperty( HibernateOrmMapperSettings.INDEXING_LISTENERS_ENABLED, false )
				.tenants( TENANT_1_ID, TENANT_2_ID, TENANT_3_ID )
				.withProperty( HibernateOrmMapperSettings.MULTI_TENANCY_TENANT_IDS,
						String.join( ",", TENANT_1_ID, TENANT_2_ID, TENANT_3_ID ) )
				.setup( Book.class, Author.class );
		initData( sessionFactory, TENANT_1_ID, HibernateOrmMassIndexerMultiTenancyIT::newAuthor );
		with( sessionFactory, TENANT_1_ID ).runNoTransaction( session -> {
			assertBookAndAuthorCount( session, 0, 0 );
		} );
		initData( sessionFactory, TENANT_2_ID, HibernateOrmMassIndexerMultiTenancyIT::newAuthor );
		with( sessionFactory, TENANT_2_ID ).runNoTransaction( session -> {
			assertBookAndAuthorCount( session, 0, 0 );
		} );
		initData( sessionFactory, TENANT_3_ID, HibernateOrmMassIndexerMultiTenancyIT::newAuthor );
		with( sessionFactory, TENANT_3_ID ).runNoTransaction( session -> {
			assertBookAndAuthorCount( session, 0, 0 );
		} );
	}

	@Test
	void explicitTenant() throws InterruptedException {
		// tag::explicitTenants[]
		SearchMapping searchMapping = /* ... */ // <1>
				// end::explicitTenants[]
				Search.mapping( sessionFactory );
		// tag::explicitTenants[]
		searchMapping.scope( Object.class ) // <2>
				.massIndexer( asSet( "tenant1", "tenant2" ) ) // <3>
				// end::explicitTenants[]
				.purgeAllOnStart( BackendConfigurations.simple().supportsExplicitPurge() )
				// tag::explicitTenants[]
				.startAndWait(); // <4>
		// end::explicitTenants[]
		with( sessionFactory, TENANT_1_ID ).runInTransaction( session -> {
			assertBookAndAuthorCount( session, NUMBER_OF_BOOKS, NUMBER_OF_BOOKS );
		} );
		with( sessionFactory, TENANT_2_ID ).runInTransaction( session -> {
			assertBookAndAuthorCount( session, NUMBER_OF_BOOKS, NUMBER_OF_BOOKS );
		} );
		with( sessionFactory, TENANT_3_ID ).runInTransaction( session -> {
			assertBookAndAuthorCount( session, 0, 0 );
		} );
	}

	@Test
	void implicitTenants() throws InterruptedException {
		// tag::implicitTenants[]
		SearchMapping searchMapping = /* ... */ // <1>
				// end::implicitTenants[]
				Search.mapping( sessionFactory );
		// tag::implicitTenants[]
		searchMapping.scope( Object.class ) // <2>
				.massIndexer() // <3>
				// end::implicitTenants[]
				.purgeAllOnStart( BackendConfigurations.simple().supportsExplicitPurge() )
				// tag::implicitTenants[]
				.startAndWait(); // <4>
		// end::implicitTenants[]
		with( sessionFactory, TENANT_1_ID ).runInTransaction( session -> {
			assertBookAndAuthorCount( session, NUMBER_OF_BOOKS, NUMBER_OF_BOOKS );
		} );
		with( sessionFactory, TENANT_2_ID ).runInTransaction( session -> {
			assertBookAndAuthorCount( session, NUMBER_OF_BOOKS, NUMBER_OF_BOOKS );
		} );
		with( sessionFactory, TENANT_3_ID ).runInTransaction( session -> {
			assertBookAndAuthorCount( session, NUMBER_OF_BOOKS, NUMBER_OF_BOOKS );
		} );
	}

	void assertBookAndAuthorCount(Session session, int expectedBookCount, int expectedAuthorCount) {
		setupHelper.assertions().searchAfterIndexChangesAndPotentialRefresh( () -> {
			SearchSession searchSession = Search.session( session );
			assertThat( searchSession.search( Book.class )
					.where( f -> f.matchAll() )
					.fetchTotalHitCount() )
					.isEqualTo( expectedBookCount );
			assertThat( searchSession.search( Author.class )
					.where( f -> f.matchAll() )
					.fetchTotalHitCount() )
					.isEqualTo( expectedAuthorCount );
		} );
	}

	private static void initData(SessionFactory sessionFactory, String tenantId, Function<Integer, Author> authorInit) {
		with( sessionFactory, tenantId ).runNoTransaction( entityManager -> {
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
