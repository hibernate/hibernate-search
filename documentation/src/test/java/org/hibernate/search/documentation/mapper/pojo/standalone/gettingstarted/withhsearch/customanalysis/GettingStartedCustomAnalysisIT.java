/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.documentation.mapper.pojo.standalone.gettingstarted.withhsearch.customanalysis;

import static org.assertj.core.api.Assertions.assertThat;

import org.hibernate.search.documentation.testsupport.BackendConfigurations;
import org.hibernate.search.documentation.testsupport.TestConfiguration;
import org.hibernate.search.engine.environment.bean.BeanReference;
import org.hibernate.search.engine.environment.bean.BeanRetrieval;
import org.hibernate.search.engine.search.query.SearchResult;
import org.hibernate.search.mapper.pojo.standalone.mapping.CloseableSearchMapping;
import org.hibernate.search.mapper.pojo.standalone.mapping.SearchMapping;
import org.hibernate.search.mapper.pojo.standalone.mapping.StandalonePojoMappingConfigurer;
import org.hibernate.search.mapper.pojo.standalone.session.SearchSession;
import org.hibernate.search.util.impl.integrationtest.common.TestConfigurationProvider;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendConfiguration;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class GettingStartedCustomAnalysisIT {

	@Rule
	public TestConfigurationProvider configurationProvider = new TestConfigurationProvider();

	private CloseableSearchMapping searchMapping;

	@Before
	public void setup() {
		if ( BackendConfiguration.isLucene() ) {
			// tag::setup-lucene[]
			CloseableSearchMapping searchMapping = SearchMapping.builder()
					.property(
							"hibernate.search.mapping.configurer",
							(StandalonePojoMappingConfigurer) context -> {
								context.addEntityType( Book.class )
										.addEntityType( Author.class );
							}
					)
					.property( "hibernate.search.backend.directory.root",
							"some/filesystem/path" )
					.property( "hibernate.search.backend.analysis.configurer",
							BeanReference.of( MyLuceneAnalysisConfigurer.class, BeanRetrieval.CLASS ) ) // <6>
					// end::setup-lucene[]
					.properties( TestConfiguration.standalonePojoMapperProperties( configurationProvider,
							BackendConfigurations.plain() ) )
					// tag::setup-lucene[]
					.build();
			// end::setup-lucene[]
			this.searchMapping = searchMapping;
		}
		else if ( BackendConfiguration.isElasticsearch() ) {
			// tag::setup-elasticsearch[]
			CloseableSearchMapping searchMapping = SearchMapping.builder()
					.property(
							"hibernate.search.mapping.configurer",
							(StandalonePojoMappingConfigurer) context -> {
								context.addEntityType( Book.class )
										.addEntityType( Author.class );
							}
					)
					.property( "hibernate.search.backend.hosts",
							"elasticsearch.mycompany.com" )
					.property( "hibernate.search.backend.analysis.configurer",
							BeanReference.of( MyElasticsearchAnalysisConfigurer.class, BeanRetrieval.CLASS ) ) // <7>
					// end::setup-elasticsearch[]
					.properties( TestConfiguration.standalonePojoMapperProperties( configurationProvider,
							BackendConfigurations.plain() ) )
					// tag::setup-elasticsearch[]
					.build();
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
		Integer bookId = 2;
		try ( SearchSession session = searchMapping.createSession() ) {
			Author author = new Author();
			author.setId( 1 );
			author.setName( "John Doe" );

			Book book = new Book();
			book.setId( bookId );
			book.setTitle( "Refactoring: Improving the Design of Existing Code" );
			book.setIsbn( "978-0-58-600835-5" );
			book.setPageCount( 200 );
			book.getAuthors().add( author );
			author.getBooks().add( book );

			session.indexingPlan().add( author );
			session.indexingPlan().add( book );
		}
		// Make sure changes are visible
		searchMapping.scope( Book.class ).workspace().refresh();

		// tag::searching[]
		try ( SearchSession session = searchMapping.createSession() ) {
			SearchResult<Integer> result = session.search( Book.class )
					.select( f -> f.id( Integer.class ) )
					.where( f -> f.match()
							.fields( "title", "authors.name" )
							.matching( "refactored" ) )
					.fetch( 20 );
			// end::searching[]

			assertThat( result.hits() )
					.containsExactlyInAnyOrder( bookId );
			// tag::searching[]
		}
		// end::searching[]

		// Also test the other terms mentioned in the getting started guide
		try ( SearchSession session = searchMapping.createSession() ) {
			for ( String term : new String[] { "Refactor", "refactors", "refactor", "refactoring" } ) {
				SearchResult<Integer> result = session.search( Book.class )
						.select( f -> f.id( Integer.class ) )
						.where( f -> f.match()
								.fields( "title", "authors.name" )
								.matching( term ) )
						.fetch( 20 );
				assertThat( result.hits() ).as( "Result of searching for '" + term + "'" )
						.containsExactlyInAnyOrder( bookId );
			}
		}
	}

}
