/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.documentation.gettingstarted.withhsearch.withanalysis;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.jpa.FullTextEntityManager;
import org.hibernate.search.mapper.orm.jpa.FullTextQuery;
import org.hibernate.search.util.impl.integrationtest.orm.OrmUtils;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class GettingStartedWithAnalysisIT {

	@Parameterized.Parameters(name = "{0}")
	public static Object[][] persistenceUnits() {
		return new Object[][] {
				{ "lucene" },
				{ "elasticsearch" }
		};
	}

	private final String persistenceUnitName;

	private EntityManagerFactory entityManagerFactory;

	public GettingStartedWithAnalysisIT(String persistenceUnitSuffix) {
		this.persistenceUnitName = "GettingStartedWithAnalysisIT_" + persistenceUnitSuffix;
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
			Author author = new Author();
			author.setName( "John Doe" );

			Book book = new Book();
			book.setTitle( "Refactoring: Improving the Design of Existing Code" );
			book.getAuthors().add( author );
			author.getBooks().add( book );

			entityManager.persist( author );
			entityManager.persist( book );

			bookIdHolder.set( book.getId() );
		} );

		OrmUtils.withinJPATransaction( entityManagerFactory, entityManager -> {
			// tag::searching[]
			// Not shown: get the entity manager and open a transaction
			FullTextEntityManager fullTextEntityManager = Search.getFullTextEntityManager( entityManager );

			FullTextQuery<Book> query = fullTextEntityManager.search( Book.class ).query()
					.asEntity()
					.predicate( factory -> factory.match()
							.onFields( "title", "authors.name" )
							.matching( "refactor" )
							.toPredicate()
					)
					.build();

			List<Book> result = query.getResultList();
			// Not shown: commit the transaction and close the entity manager
			// end::searching[]

			assertThat( result ).extracting( "id" )
					.containsOnly( bookIdHolder.get() );
		} );

		// Also test the other terms mentioned in the getting started guide
		OrmUtils.withinJPATransaction( entityManagerFactory, entityManager -> {
			FullTextEntityManager fullTextEntityManager = Search.getFullTextEntityManager( entityManager );

			for ( String term : new String[] { "Refactor", "refactors", "refactored", "refactoring" } ) {
				FullTextQuery<Book> query = fullTextEntityManager.search( Book.class ).query()
						.asEntity()
						.predicate( factory -> factory.match()
								.onFields( "title", "authors.name" )
								.matching( term )
								.toPredicate()
						)
						.build();
				List<Book> result = query.getResultList();
				assertThat( result ).as( "Result of searching for '" + term + "'" )
						.extracting( "id" )
						.containsOnly( bookIdHolder.get() );
			}
		} );
	}

}
