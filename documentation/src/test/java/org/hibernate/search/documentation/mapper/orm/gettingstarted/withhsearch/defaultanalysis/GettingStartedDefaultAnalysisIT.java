/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.documentation.mapper.orm.gettingstarted.withhsearch.defaultanalysis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.search.util.impl.integrationtest.common.rule.BackendConfiguration.BACKEND_TYPE;
import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.with;
import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

import org.hibernate.search.documentation.testsupport.TestConfiguration;
import org.hibernate.search.engine.search.query.SearchResult;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.massindexing.MassIndexer;
import org.hibernate.search.mapper.orm.scope.SearchScope;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.hibernate.search.util.impl.integrationtest.common.TestConfigurationProvider;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class GettingStartedDefaultAnalysisIT {

	private final String persistenceUnitName = "GettingStartedDefaultAnalysisIT_" + BACKEND_TYPE;

	private EntityManagerFactory entityManagerFactory;

	@Rule
	public TestConfigurationProvider configurationProvider = new TestConfigurationProvider();

	@Before
	public void setup() {
		entityManagerFactory = Persistence.createEntityManagerFactory( persistenceUnitName,
				TestConfiguration.ormMapperProperties( configurationProvider ) );
	}

	@After
	public void cleanup() {
		if ( entityManagerFactory != null ) {
			entityManagerFactory.close();
		}
	}

	@Test
	public void test() {
		AtomicReference<Integer> bookIdHolder = new AtomicReference<>();

		with( entityManagerFactory ).runInTransaction( entityManager -> {
			// tag::indexing[]
			// Not shown: get the entity manager and open a transaction
			Author author = new Author();
			author.setName( "John Doe" );

			Book book = new Book();
			book.setTitle( "Refactoring: Improving the Design of Existing Code" );
			book.setIsbn( "978-0-58-600835-5" );
			book.setPageCount( 200 );
			book.getAuthors().add( author );
			author.getBooks().add( book );

			entityManager.persist( author );
			entityManager.persist( book );
			// Not shown: commit the transaction and close the entity manager
			// end::indexing[]

			bookIdHolder.set( book.getId() );
		} );

		with( entityManagerFactory ).runInTransaction( entityManager -> {
			try {
			// tag::manual-index[]
				SearchSession searchSession = Search.session( entityManager ); // <1>

				MassIndexer indexer = searchSession.massIndexer( Book.class ) // <2>
						.threadsToLoadObjects( 7 ); // <3>

				indexer.startAndWait(); // <4>
			// end::manual-index[]
			}
			catch (InterruptedException e) {
				throw new RuntimeException( e );
			}
		} );

		with( entityManagerFactory ).runInTransaction( entityManager -> {
			// tag::searching-objects[]
			// Not shown: get the entity manager and open a transaction
			SearchSession searchSession = Search.session( entityManager ); // <1>

			SearchScope<Book> scope = searchSession.scope( Book.class ); // <2>

			SearchResult<Book> result = searchSession.search( scope ) // <3>
					.where( scope.predicate().match() // <4>
							.fields( "title", "authors.name" )
							.matching( "refactoring" )
							.toPredicate() )
					.fetch( 20 ); // <5>

			long totalHitCount = result.total().hitCount(); // <6>
			List<Book> hits = result.hits(); // <7>

			List<Book> hits2 =
					/* ... same DSL calls as above... */
			// end::searching-objects[]
					searchSession.search( scope )
					.where( scope.predicate().match()
							.fields( "title", "authors.name" )
							.matching( "refactoring" )
							.toPredicate() )
			// tag::searching-objects[]
					.fetchHits( 20 ); // <8>
			// Not shown: commit the transaction and close the entity manager
			// end::searching-objects[]

			assertThat( totalHitCount ).isEqualTo( 1 );
			assertThat( hits ).extracting( "id" )
					.containsExactlyInAnyOrder( bookIdHolder.get() );
			assertThat( hits2 ).extracting( "id" )
					.containsExactlyInAnyOrder( bookIdHolder.get() );
		} );

		with( entityManagerFactory ).runInTransaction( entityManager -> {
			// tag::searching-lambdas[]
			// Not shown: get the entity manager and open a transaction
			SearchSession searchSession = Search.session( entityManager ); // <1>

			SearchResult<Book> result = searchSession.search( Book.class ) // <2>
					.where( f -> f.match() // <3>
							.fields( "title", "authors.name" )
							.matching( "refactoring" ) )
					.fetch( 20 ); // <4>

			long totalHitCount = result.total().hitCount(); // <5>
			List<Book> hits = result.hits(); // <6>

			List<Book> hits2 =
					/* ... same DSL calls as above... */
			// end::searching-lambdas[]
					searchSession.search( Book.class )
							.where( f -> f.match()
									.fields( "title", "authors.name" )
									.matching( "refactoring" ) )
			// tag::searching-lambdas[]
					.fetchHits( 20 ); // <7>
			// Not shown: commit the transaction and close the entity manager
			// end::searching-lambdas[]

			assertThat( totalHitCount ).isEqualTo( 1 );
			assertThat( hits ).extracting( "id" )
					.containsExactlyInAnyOrder( bookIdHolder.get() );
			assertThat( hits2 ).extracting( "id" )
					.containsExactlyInAnyOrder( bookIdHolder.get() );
		} );

		with( entityManagerFactory ).runInTransaction( entityManager -> {
			// tag::counting[]
			// Not shown: get the entity manager and open a transaction
			SearchSession searchSession = Search.session( entityManager );

			long totalHitCount = searchSession.search( Book.class )
					.where( f -> f.match()
							.fields( "title", "authors.name" )
							.matching( "refactoring" ) )
					.fetchTotalHitCount(); // <1>
			// Not shown: commit the transaction and close the entity manager
			// end::counting[]

			assertEquals( 1L, totalHitCount );
		} );
	}

}
