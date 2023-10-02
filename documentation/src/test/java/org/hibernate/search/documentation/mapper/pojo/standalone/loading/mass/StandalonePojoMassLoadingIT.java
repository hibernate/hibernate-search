/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.documentation.mapper.pojo.standalone.loading.mass;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.hibernate.search.documentation.mapper.pojo.standalone.loading.mydatastore.MyDatastore;
import org.hibernate.search.documentation.testsupport.BackendConfigurations;
import org.hibernate.search.documentation.testsupport.SimulatedBeanProvider;
import org.hibernate.search.documentation.testsupport.TestConfiguration;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.AnnotatedTypeSource;
import org.hibernate.search.mapper.pojo.standalone.cfg.spi.StandalonePojoMapperSpiSettings;
import org.hibernate.search.mapper.pojo.standalone.mapping.CloseableSearchMapping;
import org.hibernate.search.mapper.pojo.standalone.mapping.SearchMapping;
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

		MyDatastore datastore = new MyDatastore( entities );
		this.searchMapping = SearchMapping.builder( AnnotatedTypeSource.fromClasses(
				Book.class
		) )
				// Simulate the use of a CDI integration
				.property( StandalonePojoMapperSpiSettings.BEAN_PROVIDER,
						SimulatedBeanProvider.builder()
								.add( MyLoadingBinder.class, new MyLoadingBinder( datastore ) )
								.build() )
				.properties( TestConfiguration.standalonePojoMapperProperties(
						configurationProvider,
						BackendConfigurations.simple()
				) )
				.build();
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
