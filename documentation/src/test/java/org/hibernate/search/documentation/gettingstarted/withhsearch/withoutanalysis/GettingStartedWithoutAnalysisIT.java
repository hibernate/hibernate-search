/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.documentation.gettingstarted.withhsearch.withoutanalysis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.mapper.orm.search.SearchScope;
import org.hibernate.search.engine.search.query.SearchResult;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.hibernate.search.mapper.orm.massindexing.MassIndexer;
import org.hibernate.search.util.impl.integrationtest.orm.OrmUtils;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class GettingStartedWithoutAnalysisIT {

	@Parameterized.Parameters(name = "{0}")
	public static Object[][] persistenceUnits() {
		return new Object[][] {
				{ "lucene" },
				{ "elasticsearch" }
		};
	}

	private final String persistenceUnitName;

	private EntityManagerFactory entityManagerFactory;

	public GettingStartedWithoutAnalysisIT(String persistenceUnitSuffix) {
		this.persistenceUnitName = "GettingStartedWithoutAnalysisIT_" + persistenceUnitSuffix;
	}

	@Before
	public void setup() {
		entityManagerFactory = Persistence.createEntityManagerFactory( persistenceUnitName );
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

		OrmUtils.withinJPATransaction( entityManagerFactory, entityManager -> {
			// tag::indexing[]
			// Not shown: get the entity manager and open a transaction
			Author author = new Author();
			author.setName( "John Doe" );

			Book book = new Book();
			book.setTitle( "Refactoring: Improving the Design of Existing Code" );
			book.getAuthors().add( author );
			author.getBooks().add( book );

			entityManager.persist( author );
			entityManager.persist( book );
			// Not shown: commit the transaction and close the entity manager
			// end::indexing[]

			bookIdHolder.set( book.getId() );
		} );

		OrmUtils.withinJPATransaction( entityManagerFactory, entityManager -> {
			try {
			// tag::manual-index[]
				SearchSession searchSession = Search.getSearchSession( entityManager ); // <1>

				MassIndexer indexer = searchSession.createIndexer( Book.class ) // <2>
						.threadsToLoadObjects( 7 ); // <3>

				indexer.startAndWait(); // <4>
			// end::manual-index[]
			}
			catch (InterruptedException e) {
				throw new RuntimeException( e );
			}
		} );

		OrmUtils.withinJPATransaction( entityManagerFactory, entityManager -> {
			// tag::searching-objects[]
			// Not shown: get the entity manager and open a transaction
			SearchSession searchSession = Search.getSearchSession( entityManager ); // <1>

			SearchScope<Book> scope = searchSession.scope( Book.class ); // <2>

			SearchQuery<Book> query = scope.search() // <3>
					.asEntity() // <4>
					.predicate( scope.predicate().match() // <5>
							.onFields( "title", "authors.name" )
							.matching( "Refactoring: Improving the Design of Existing Code" )
							.toPredicate()
					)
					.toQuery(); // <6>

			SearchResult<Book> result = query.fetch(); // <7>
			long totalHitCount = result.getTotalHitCount(); // <8>
			List<Book> hits = result.getHits(); // <9>

			List<Book> hits2 = query.fetchHits(); // <10>
			// Not shown: commit the transaction and close the entity manager
			// end::searching-objects[]

			assertThat( result.getHits() ).extracting( "id" )
					.containsExactlyInAnyOrder( bookIdHolder.get() );
		} );

		OrmUtils.withinJPATransaction( entityManagerFactory, entityManager -> {
			// tag::searching-lambdas[]
			// Not shown: get the entity manager and open a transaction
			SearchSession searchSession = Search.getSearchSession( entityManager ); // <1>

			SearchQuery<Book> query = searchSession.search( Book.class ) // <2>
					.asEntity() // <3>
					.predicate( f -> f.match() // <4>
							.onFields( "title", "authors.name" )
							.matching( "Refactoring: Improving the Design of Existing Code" )
					)
					.toQuery(); // <5>

			SearchResult<Book> result = query.fetch(); // <6>
			long totalHitCount = result.getTotalHitCount(); // <7>
			List<Book> hits = result.getHits(); // <8>

			List<Book> hits2 = query.fetchHits(); // <9>
			// Not shown: commit the transaction and close the entity manager
			// end::searching-lambdas[]

			assertThat( result.getHits() ).extracting( "id" )
					.containsExactlyInAnyOrder( bookIdHolder.get() );
		} );

		OrmUtils.withinJPATransaction( entityManagerFactory, entityManager -> {
			// tag::counting[]
			// Not shown: get the entity manager and open a transaction
			SearchSession searchSession = Search.getSearchSession( entityManager );

			SearchQuery<Book> query = searchSession.search( Book.class )
					.asEntity()
					.predicate( f -> f.match()
							.onFields( "title", "authors.name" )
							.matching( "Refactoring: Improving the Design of Existing Code" )
					)
					.toQuery();

			long resultSize = query.fetchTotalHitCount(); // <1>
			// Not shown: commit the transaction and close the entity manager
			// end::counting[]

			assertEquals( 1L, resultSize );
		} );
	}

}
