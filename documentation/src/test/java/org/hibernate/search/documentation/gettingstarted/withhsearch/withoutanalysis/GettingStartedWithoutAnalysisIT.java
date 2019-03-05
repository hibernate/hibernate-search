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
import org.hibernate.search.mapper.orm.search.query.FullTextQuery;
import org.hibernate.search.mapper.orm.search.FullTextSearchTarget;
import org.hibernate.search.mapper.orm.session.FullTextSession;
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
				FullTextSession fullTextSession = Search.getFullTextSession( entityManager ); // <1>

				MassIndexer indexer = fullTextSession.createIndexer( Book.class ) // <2>
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
			FullTextSession fullTextSession = Search.getFullTextSession( entityManager ); // <1>

			FullTextSearchTarget<Book> searchTarget = fullTextSession.target( Book.class ); // <2>

			FullTextQuery<Book> query = searchTarget.search() // <3>
					.asEntity() // <4>
					.predicate( searchTarget.predicate().match() // <5>
							.onFields( "title", "authors.name" )
							.matching( "Refactoring: Improving the Design of Existing Code" )
							.toPredicate()
					)
					.build(); // <6>

			List<Book> result = query.getResultList(); // <7>
			// Not shown: commit the transaction and close the entity manager
			// end::searching-objects[]

			assertThat( result ).extracting( "id" )
					.containsExactlyInAnyOrder( bookIdHolder.get() );
		} );

		OrmUtils.withinJPATransaction( entityManagerFactory, entityManager -> {
			// tag::searching-lambdas[]
			// Not shown: get the entity manager and open a transaction
			FullTextSession fullTextSession = Search.getFullTextSession( entityManager );

			FullTextQuery<Book> query = fullTextSession.search( Book.class )
					.asEntity()
					.predicate( factory -> factory.match()
							.onFields( "title", "authors.name" )
							.matching( "Refactoring: Improving the Design of Existing Code" )
					)
					.build();

			List<Book> result = query.getResultList();
			// Not shown: commit the transaction and close the entity manager
			// end::searching-lambdas[]

			assertThat( result ).extracting( "id" )
					.containsExactlyInAnyOrder( bookIdHolder.get() );
		} );

		OrmUtils.withinJPATransaction( entityManagerFactory, entityManager -> {
			// tag::counting[]
			// Not shown: get the entity manager and open a transaction
			FullTextSession fullTextSession = Search.getFullTextSession( entityManager );

			FullTextQuery<Book> query = fullTextSession.search( Book.class )
					.asEntity()
					.predicate( factory -> factory.match()
							.onFields( "title", "authors.name" )
							.matching( "Refactoring: Improving the Design of Existing Code" )
					)
					.build();

			long resultSize = query.getResultSize(); // <1>
			// Not shown: commit the transaction and close the entity manager
			// end::counting[]

			assertEquals( 1L, resultSize );
		} );
	}

}
