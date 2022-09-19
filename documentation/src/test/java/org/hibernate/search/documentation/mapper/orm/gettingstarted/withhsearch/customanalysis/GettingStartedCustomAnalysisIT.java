/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.documentation.mapper.orm.gettingstarted.withhsearch.customanalysis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.search.util.impl.integrationtest.common.rule.BackendConfiguration.BACKEND_TYPE;
import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.with;

import java.util.concurrent.atomic.AtomicReference;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

import org.hibernate.search.documentation.testsupport.TestConfiguration;
import org.hibernate.search.engine.search.query.SearchResult;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.hibernate.search.util.impl.integrationtest.common.TestConfigurationProvider;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class GettingStartedCustomAnalysisIT {

	private final String persistenceUnitName = "GettingStartedCustomAnalysisIT_" + BACKEND_TYPE;

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

			bookIdHolder.set( book.getId() );
		} );

		with( entityManagerFactory ).runInTransaction( entityManager -> {
			// tag::searching[]
			// Not shown: get the entity manager and open a transaction
			SearchSession searchSession = Search.session( entityManager );

			SearchResult<Book> result = searchSession.search( Book.class )
					.where( f -> f.match()
							.fields( "title", "authors.name" )
							.matching( "refactored" ) )
					.fetch( 20 );
			// Not shown: commit the transaction and close the entity manager
			// end::searching[]

			assertThat( result.hits() ).extracting( "id" )
					.containsExactlyInAnyOrder( bookIdHolder.get() );
		} );

		// Also test the other terms mentioned in the getting started guide
		with( entityManagerFactory ).runInTransaction( entityManager -> {
			SearchSession searchSession = Search.session( entityManager );

			for ( String term : new String[] { "Refactor", "refactors", "refactor", "refactoring" } ) {
				SearchResult<Book> result = searchSession.search( Book.class )
						.where( f -> f.match()
								.fields( "title", "authors.name" )
								.matching( term ) )
						.fetch( 20 );
				assertThat( result.hits() ).as( "Result of searching for '" + term + "'" )
						.extracting( "id" )
						.containsExactlyInAnyOrder( bookIdHolder.get() );
			}
		} );
	}

}
