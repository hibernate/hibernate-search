/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.documentation.mapper.pojo.standalone.loading.selection;

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
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.AnnotatedTypeSource;
import org.hibernate.search.mapper.pojo.standalone.mapping.CloseableSearchMapping;
import org.hibernate.search.mapper.pojo.standalone.mapping.SearchMapping;
import org.hibernate.search.mapper.pojo.standalone.session.SearchSession;
import org.hibernate.search.mapper.pojo.work.IndexingPlanSynchronizationStrategy;
import org.hibernate.search.util.impl.integrationtest.common.TestConfigurationProvider;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class StandalonePojoSelectionLoadingIT {

	@RegisterExtension
	public TestConfigurationProvider configurationProvider = new TestConfigurationProvider();

	private Map<String, Book> books;

	private MyDatastore datastore;
	private CloseableSearchMapping searchMapping;

	@BeforeEach
	void setup() {
		Map<Class<?>, Map<String, ?>> entities = new HashMap<>();
		books = new LinkedHashMap<>();
		entities.put( Book.class, books );
		Arrays.asList(
				new Book( "cavesofsteel", "The Caves Of Steel" ),
				new Book( "eyeoftheworld", "The Eye of the World" )
		).forEach( book -> books.put( book.getId(), book ) );
		this.datastore = new MyDatastore( entities );

		this.searchMapping = SearchMapping.builder( AnnotatedTypeSource.fromClasses(
				Book.class
		) )
				.properties( TestConfiguration.standalonePojoMapperProperties(
						configurationProvider,
						BackendConfigurations.simple()
				) )
				.build();

		// Initial indexing
		try ( SearchSession searchSession = searchMapping.createSessionWithOptions()
				.indexingPlanSynchronizationStrategy( IndexingPlanSynchronizationStrategy.sync() )
				.build() ) {
			books.values().forEach( searchSession.indexingPlan()::add );
		}
	}

	@AfterEach
	void cleanup() {
		if ( searchMapping != null ) {
			searchMapping.close();
		}
	}

	@Test
	void test() {
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
