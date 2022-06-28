/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.documentation.mapper.pojo.standalone.gettingstarted.withhsearch.defaultanalysis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

import java.util.List;

import org.hibernate.search.documentation.testsupport.BackendConfigurations;
import org.hibernate.search.documentation.testsupport.TestConfiguration;
import org.hibernate.search.engine.search.query.SearchResult;
import org.hibernate.search.mapper.pojo.standalone.mapping.CloseableSearchMapping;
import org.hibernate.search.mapper.pojo.standalone.mapping.SearchMapping;
import org.hibernate.search.mapper.pojo.standalone.scope.SearchScope;
import org.hibernate.search.mapper.pojo.standalone.session.SearchSession;
import org.hibernate.search.util.impl.integrationtest.common.TestConfigurationProvider;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendConfiguration;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class GettingStartedDefaultAnalysisIT {

	@Rule
	public TestConfigurationProvider configurationProvider = new TestConfigurationProvider();

	private CloseableSearchMapping searchMapping;

	@Before
	public void setup() {
		if ( BackendConfiguration.isLucene() ) {
			// tag::setup-lucene[]
			CloseableSearchMapping searchMapping = SearchMapping.builder() // <1>
					.addEntityType( Book.class ) // <2>
					.addEntityType( Author.class )
					.property( "hibernate.search.backend.directory.root",
							"some/filesystem/path" ) // <3>
					// end::setup-lucene[]
					.properties( TestConfiguration.standalonePojoMapperProperties( configurationProvider,
							BackendConfigurations.plain() ) )
					// tag::setup-lucene[]
					.build(); // <4>
			// end::setup-lucene[]
			this.searchMapping = searchMapping;
		}
		else if ( BackendConfiguration.isElasticsearch() ) {
			// tag::setup-elasticsearch[]
			CloseableSearchMapping searchMapping = SearchMapping.builder() // <1>
					.addEntityType( Book.class ) // <2>
					.addEntityType( Author.class )
					.property( "hibernate.search.backend.hosts",
							"elasticsearch.mycompany.com" ) // <3>
					.property( "hibernate.search.backend.protocol",
							"https" ) // <4>
					.property( "hibernate.search.backend.username",
							"ironman" ) // <5>
					.property( "hibernate.search.backend.password",
							"j@rV1s" )
					// end::setup-elasticsearch[]
					.properties( TestConfiguration.standalonePojoMapperProperties( configurationProvider,
							BackendConfigurations.plain() ) )
					// tag::setup-elasticsearch[]
					.build(); // <6>
			// end::setup-elasticsearch[]
			this.searchMapping = searchMapping;
		}
	}

	@After
	public void cleanup() {
		if ( searchMapping != null ) {
			searchMapping.close();
		}
	}

	@Test
	public void test() {
		// tag::indexing[]
		try ( SearchSession session = searchMapping.createSession() ) { // <1>
			Author author = new Author(); // <2>
			author.setId( 1 );
			author.setName( "John Doe" );

			Book book = new Book();
			book.setId( 2 );
			book.setTitle( "Refactoring: Improving the Design of Existing Code" );
			book.setIsbn( "978-0-58-600835-5" );
			book.setPageCount( 200 );
			book.getAuthors().add( author );
			author.getBooks().add( book );

			session.indexingPlan().add( author ); // <3>
			session.indexingPlan().add( book );
		}
		// end::indexing[]
		Integer bookId = 2;
		// Make sure changes are visible
		searchMapping.scope( Book.class ).workspace().refresh();

		// tag::searching-objects[]
		try ( SearchSession session = searchMapping.createSession() ) { // <1>
			SearchScope<Book> scope = session.scope( Book.class ); // <2>

			SearchResult<Integer> result = session.search( scope ) // <3>
					.select( f -> f.id( Integer.class ) ) // <4>
					.where( scope.predicate().match() // <5>
							.fields( "title", "authors.name" )
							.matching( "refactoring" )
							.toPredicate() )
					.fetch( 20 ); // <6>

			long totalHitCount = result.total().hitCount(); // <7>
			List<Integer> hits = result.hits(); // <8>

			List<Integer> hits2 =
					/* ... same DSL calls as above... */
					// end::searching-objects[]
					session.search( scope )
							.select( f -> f.id( Integer.class ) )
							.where( scope.predicate().match()
									.fields( "title", "authors.name" )
									.matching( "refactoring" )
									.toPredicate() )
							// tag::searching-objects[]
							.fetchHits( 20 ); // <9>
			// end::searching-objects[]

			assertThat( totalHitCount ).isEqualTo( 1 );
			assertThat( hits )
					.containsExactlyInAnyOrder( bookId );
			assertThat( hits2 )
					.containsExactlyInAnyOrder( bookId );
			// tag::searching-objects[]
		}
		// end::searching-objects[]

		// tag::searching-lambdas[]
		try ( SearchSession session = searchMapping.createSession() ) { // <1>
			SearchResult<Integer> result = session.search( Book.class ) // <2>
					.select( f -> f.id( Integer.class ) ) // <3>
					.where( f -> f.match() // <4>
							.fields( "title", "authors.name" )
							.matching( "refactoring" ) )
					.fetch( 20 ); // <5>

			long totalHitCount = result.total().hitCount(); // <6>
			List<Integer> hits = result.hits(); // <7>

			List<Integer> hits2 =
					/* ... same DSL calls as above... */
					// end::searching-lambdas[]
					session.search( Book.class )
							.select( f -> f.id( Integer.class ) )
							.where( f -> f.match()
									.fields( "title", "authors.name" )
									.matching( "refactoring" ) )
							// tag::searching-lambdas[]
							.fetchHits( 20 ); // <8>
			// end::searching-lambdas[]

			assertThat( totalHitCount ).isEqualTo( 1 );
			assertThat( hits )
					.containsExactlyInAnyOrder( bookId );
			assertThat( hits2 )
					.containsExactlyInAnyOrder( bookId );
			// tag::searching-lambdas[]
		}
		// end::searching-lambdas[]

		// tag::counting[]
		try ( SearchSession session = searchMapping.createSession() ) {
			long totalHitCount = session.search( Book.class )
					.where( f -> f.match()
							.fields( "title", "authors.name" )
							.matching( "refactoring" ) )
					.fetchTotalHitCount(); // <1>
			// end::counting[]

			assertEquals( 1L, totalHitCount );
			// tag::counting[]
		}
		// end::counting[]

		// tag::indexing-addOrUpdate[]
		try ( SearchSession session = searchMapping.createSession() ) {
			// end::indexing-addOrUpdate[]
			Author author = new Author();
			author.setId( 1 );
			author.setName( "John Doe" );
			Book book = new Book();
			book.setId( 2 );
			book.setTitle( "Refactoring: Improving the Design of Existing Code" );
			book.setIsbn( "978-0-58-600835-5" );
			book.setPageCount( 200 );
			book.getAuthors().add( author );
			author.getBooks().add( book );
			// tag::indexing-addOrUpdate[]
			book.setTitle( "On Cleanup of Existing Code" );
			session.indexingPlan().addOrUpdate( book );
		}
		// end::indexing-addOrUpdate[]
		// Make sure changes are visible
		searchMapping.scope( Book.class ).workspace().refresh();
		try ( SearchSession session = searchMapping.createSession() ) {
			assertThat( session.search( Book.class )
					.select( f -> f.id( Integer.class ) )
					.where( f -> f.match()
							.fields( "title", "authors.name" )
							.matching( "refactoring" ) )
					.fetchHits( 20 ) )
					.isEmpty();
			assertThat( session.search( Book.class )
					.select( f -> f.id( Integer.class ) )
					.where( f -> f.match()
							.fields( "title", "authors.name" )
							.matching( "cleanup" ) )
					.fetchHits( 20 ) )
					.containsExactlyInAnyOrder( bookId );
		}

		// tag::indexing-delete[]
		try ( SearchSession session = searchMapping.createSession() ) {
			// end::indexing-delete[]
			Book book = new Book();
			book.setId( 2 );
			// tag::indexing-delete[]
			session.indexingPlan().delete( book );
		}
		// end::indexing-delete[]
		// Make sure changes are visible
		searchMapping.scope( Book.class ).workspace().refresh();
		try ( SearchSession session = searchMapping.createSession() ) {
			assertThat( session.search( Book.class )
					.select( f -> f.id( Integer.class ) )
					.where( f -> f.match()
							.fields( "title", "authors.name" )
							.matching( "Action" ) )
					.fetchHits( 20 ) )
					.isEmpty();
		}
	}

}
