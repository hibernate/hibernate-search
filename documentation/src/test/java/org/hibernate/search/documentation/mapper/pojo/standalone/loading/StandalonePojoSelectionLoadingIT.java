/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.documentation.mapper.pojo.standalone.loading;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.search.documentation.mapper.pojo.standalone.loading.mydatastore.MyDatastore;
import org.hibernate.search.documentation.mapper.pojo.standalone.loading.mydatastore.MyDatastoreConnection;
import org.hibernate.search.documentation.testsupport.BackendConfigurations;
import org.hibernate.search.documentation.testsupport.TestConfiguration;
import org.hibernate.search.mapper.pojo.standalone.mapping.CloseableSearchMapping;
import org.hibernate.search.mapper.pojo.standalone.mapping.SearchMapping;
import org.hibernate.search.mapper.pojo.standalone.mapping.StandalonePojoMappingConfigurer;
import org.hibernate.search.mapper.pojo.standalone.session.SearchSession;
import org.hibernate.search.mapper.pojo.work.IndexingPlanSynchronizationStrategy;
import org.hibernate.search.util.impl.integrationtest.common.TestConfigurationProvider;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class StandalonePojoSelectionLoadingIT {

	@Rule
	public TestConfigurationProvider configurationProvider = new TestConfigurationProvider();

	private Map<String, Book> books;

	private MyDatastore datastore;
	private CloseableSearchMapping searchMapping;

	@Before
	public void setup() {
		Map<Class<?>, Map<String, ?>> entities = new HashMap<>();
		books = new LinkedHashMap<>();
		entities.put( Book.class, books );
		Arrays.asList(
				new Book( "cavesofsteel", "The Caves Of Steel" ),
				new Book( "eyeoftheworld", "The Eye of the World" )
		).forEach( book -> books.put( book.getId(), book ) );
		this.datastore = new MyDatastore( entities );

		// tag::setup[]
		CloseableSearchMapping searchMapping = SearchMapping.builder() // <1>
				.property(
						"hibernate.search.mapping.configurer",
						(StandalonePojoMappingConfigurer) c -> {
							c.addEntityType( Book.class, context -> // <2>
							context.selectionLoadingStrategy(
									new MySelectionLoadingStrategy<>( Book.class )
							) );
						}
				)
				// end::setup[]
				.properties( TestConfiguration.standalonePojoMapperProperties(
						configurationProvider,
						BackendConfigurations.simple()
				) )
				// tag::setup[]
				.build(); // <3>
		// end::setup[]
		this.searchMapping = searchMapping;

		// Initial indexing
		try ( SearchSession searchSession = searchMapping.createSessionWithOptions()
				.indexingPlanSynchronizationStrategy( IndexingPlanSynchronizationStrategy.sync() )
				.build() ) {
			books.values().forEach( searchSession.indexingPlan()::add );
		}
	}

	@After
	public void cleanup() {
		if ( searchMapping != null ) {
			searchMapping.close();
		}
	}

	@Test
	public void test() throws InterruptedException {
		try ( SearchSession searchSession = searchMapping.createSession() ) {
			assertThat( searchSession.search( Book.class )
					.select( f -> f.id( String.class ) )
					.where( f -> f.matchAll() )
					.fetchAllHits() )
					.containsExactlyInAnyOrderElementsOf( books.keySet() );
		}

		// tag::search[]
		MyDatastore datastore = /* ... */ // <1>
				// end::search[]
				this.datastore;
		// tag::search[]
		SearchMapping searchMapping = /* ... */ // <2>
				// end::search[]
				this.searchMapping;
		// tag::search[]
		try ( MyDatastoreConnection connection = datastore.connect(); // <3>
				SearchSession searchSession = searchMapping.createSessionWithOptions() // <4>
						.loading( o -> o.context( MyDatastoreConnection.class, connection ) ) // <5>
						.build() ) { // <6>
			List<Book> hits = searchSession.search( Book.class ) // <7>
					.where( f -> f.matchAll() )
					.fetchHits( 20 ); // <8>
			// end::search[]
			assertThat( hits )
					.containsExactlyInAnyOrderElementsOf( books.values() );
			// tag::search[]
		}
		// end::search[]
	}

}
