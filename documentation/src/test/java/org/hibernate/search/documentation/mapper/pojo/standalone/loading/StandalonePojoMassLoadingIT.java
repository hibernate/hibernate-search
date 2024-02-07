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
import java.util.Map;

import org.hibernate.search.documentation.mapper.pojo.standalone.loading.mydatastore.MyDatastore;
import org.hibernate.search.documentation.testsupport.BackendConfigurations;
import org.hibernate.search.documentation.testsupport.TestConfiguration;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.AnnotatedTypeSource;
import org.hibernate.search.mapper.pojo.standalone.mapping.CloseableSearchMapping;
import org.hibernate.search.mapper.pojo.standalone.mapping.SearchMapping;
import org.hibernate.search.mapper.pojo.standalone.mapping.StandalonePojoMappingConfigurer;
import org.hibernate.search.mapper.pojo.standalone.session.SearchSession;
import org.hibernate.search.util.impl.integrationtest.common.TestConfigurationProvider;
import org.hibernate.search.util.impl.integrationtest.mapper.pojo.standalone.StandalonePojoAssertionHelper;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class StandalonePojoMassLoadingIT {

	@RegisterExtension
	public TestConfigurationProvider configurationProvider = new TestConfigurationProvider();

	private Map<String, Book> books;

	private CloseableSearchMapping searchMapping;

	private StandalonePojoAssertionHelper assertions = new StandalonePojoAssertionHelper( BackendConfigurations.simple() );

	@BeforeEach
	void setup() {
		Map<Class<?>, Map<String, ?>> entities = new HashMap<>();
		books = new LinkedHashMap<>();
		entities.put( Book.class, books );
		Arrays.asList(
				new Book( "cavesofsteel", "The Caves Of Steel" ),
				new Book( "eyeoftheworld", "The Eye of the World" )
		).forEach( book -> books.put( book.getId(), book ) );

		// tag::setup[]
		MyDatastore datastore = /* ... */ // <1>
				// end::setup[]
				new MyDatastore( entities );
		// tag::setup[]
		CloseableSearchMapping searchMapping = SearchMapping.builder( AnnotatedTypeSource.fromClasses( // <2> 
				Book.class
		) )
				.property(
						"hibernate.search.mapping.configurer",
						(StandalonePojoMappingConfigurer) configurerContext -> {
							configurerContext.addEntityType( Book.class, context -> // <3>
							context.massLoadingStrategy(
									new MyMassLoadingStrategy<>( datastore, Book.class )
							) );
						}
				)
				// end::setup[]
				.properties( TestConfiguration.standalonePojoMapperProperties(
						configurationProvider,
						BackendConfigurations.simple()
				) )
				// tag::setup[]
				.build(); // <4>
		// end::setup[]
		this.searchMapping = searchMapping;
	}

	@AfterEach
	void cleanup() {
		if ( searchMapping != null ) {
			searchMapping.close();
		}
	}

	@Test
	void test() throws InterruptedException {
		try ( SearchSession searchSession = searchMapping.createSession() ) {
			assertThat( searchSession.search( Book.class )
					.select( f -> f.id( String.class ) )
					.where( f -> f.matchAll() )
					.fetchAllHits() )
					.isEmpty();
		}

		// tag::massIndexer[]
		SearchMapping searchMapping = /* ... */ // <1>
				// end::massIndexer[]
				this.searchMapping;
		// tag::massIndexer[]
		searchMapping.scope( Object.class ).massIndexer() // <2>
				// end::massIndexer[]
				.purgeAllOnStart( BackendConfigurations.simple().supportsExplicitPurge() )
				// tag::massIndexer[]
				.startAndWait(); // <3>
		// end::massIndexer[]

		try ( SearchSession searchSession = searchMapping.createSession() ) {
			assertions.searchAfterIndexChangesAndPotentialRefresh(
					() -> assertThat( searchSession.search( Book.class )
							.select( f -> f.id( String.class ) )
							.where( f -> f.matchAll() )
							.fetchAllHits() )
							.containsExactlyInAnyOrderElementsOf( books.keySet() ) );
		}
	}

}
