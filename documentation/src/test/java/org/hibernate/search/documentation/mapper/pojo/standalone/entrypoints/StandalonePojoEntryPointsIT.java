/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.documentation.mapper.pojo.standalone.entrypoints;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;

import org.hibernate.search.documentation.testsupport.BackendConfigurations;
import org.hibernate.search.documentation.testsupport.TestConfiguration;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.AnnotatedTypeSource;
import org.hibernate.search.mapper.pojo.standalone.entity.SearchIndexedEntity;
import org.hibernate.search.mapper.pojo.standalone.mapping.CloseableSearchMapping;
import org.hibernate.search.mapper.pojo.standalone.mapping.SearchMapping;
import org.hibernate.search.mapper.pojo.standalone.scope.SearchScope;
import org.hibernate.search.mapper.pojo.standalone.session.SearchSession;
import org.hibernate.search.mapper.pojo.work.IndexingPlanSynchronizationStrategy;
import org.hibernate.search.util.impl.integrationtest.common.TestConfigurationProvider;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class StandalonePojoEntryPointsIT {

	private CloseableSearchMapping theSearchMapping;

	@RegisterExtension
	public TestConfigurationProvider configurationProvider = new TestConfigurationProvider();

	@BeforeEach
	void setup() {
		// tag::setup[]
		CloseableSearchMapping searchMapping =
				SearchMapping.builder( AnnotatedTypeSource.fromClasses( // <1>
						Book.class, Associate.class, Manager.class
				) )
						.property(
								"hibernate.search.backend.hosts", // <2>
								"elasticsearch.mycompany.com"
						)
						// end::setup[]
						.properties( TestConfiguration.standalonePojoMapperProperties(
								configurationProvider,
								BackendConfigurations.simple()
						) )
						// tag::setup[]
						.build(); // <3>
		// end::setup[]
		this.theSearchMapping = searchMapping;
	}

	@AfterEach
	void cleanup() {
		if ( theSearchMapping != null ) {
			// tag::shutdown[]
			CloseableSearchMapping searchMapping = /* ... */ // <1>
					// end::shutdown[]
					theSearchMapping;
			// tag::shutdown[]
			searchMapping.close(); // <2>
			// end::shutdown[]
		}
	}

	@Test
	void mappingContainsExpectedEntities() {
		assertThat( theSearchMapping.allIndexedEntities() )
				.extracting( SearchIndexedEntity::name )
				.contains( "Book", "Associate", "Manager" );
	}

	@Test
	void searchSession() {
		// tag::searchSession-simple[]
		SearchMapping searchMapping = /* ... */ // <1>
				// end::searchSession-simple[]
				theSearchMapping;
		// tag::searchSession-simple[]
		try ( SearchSession searchSession = searchMapping.createSession() ) { // <2>
			// ...
			// end::searchSession-simple[]
			assertThat( searchSession ).isNotNull();
			assertThat( searchSession.isOpen() ).isTrue();
			// tag::searchSession-simple[]
		}
		// end::searchSession-simple[]
	}

	@Test
	void searchSession_withOptions() {
		// tag::searchSession-withOptions[]
		SearchMapping searchMapping = /* ... */ // <1>
				// end::searchSession-withOptions[]
				theSearchMapping;
		// tag::searchSession-withOptions[]
		Object tenantId = "myTenant";
		try ( SearchSession searchSession = searchMapping.createSessionWithOptions() // <2>
				.indexingPlanSynchronizationStrategy( IndexingPlanSynchronizationStrategy.sync() )// <3>
				.tenantId( tenantId )
				.build() ) { // <4>
			// ...
			// end::searchSession-withOptions[]
			assertThat( searchSession ).isNotNull();
			assertThat( searchSession.isOpen() ).isTrue();
			// tag::searchSession-withOptions[]
		}
		// end::searchSession-withOptions[]
	}

	@Test
	void searchScope_fromSearchMapping() {
		SearchMapping searchMapping = theSearchMapping;
		SearchScope<Book> bookScope = searchMapping.scope( Book.class );
		SearchScope<Person> associateAndManagerScope = searchMapping.scope( Arrays.asList( Associate.class, Manager.class ) );
		SearchScope<Person> personScope = searchMapping.scope( Person.class );
		SearchScope<Person> personSubTypesScope = searchMapping.scope( Person.class,
				Arrays.asList( "Manager", "Associate" ) );
		SearchScope<Object> allScope = searchMapping.scope( Object.class );
		assertThat( bookScope.includedTypes() )
				.extracting( SearchIndexedEntity::name )
				.containsExactlyInAnyOrder( "Book" );
		assertThat( associateAndManagerScope.includedTypes() )
				.extracting( SearchIndexedEntity::name )
				.containsExactlyInAnyOrder( "Manager", "Associate" );
		assertThat( personScope.includedTypes() )
				.extracting( SearchIndexedEntity::name )
				.containsExactlyInAnyOrder( "Manager", "Associate" );
		assertThat( personSubTypesScope.includedTypes() )
				.extracting( SearchIndexedEntity::name )
				.containsExactlyInAnyOrder( "Manager", "Associate" );
		assertThat( allScope.includedTypes() )
				.extracting( SearchIndexedEntity::name )
				.containsExactlyInAnyOrder( "Book", "Manager", "Associate" );
	}

	@Test
	void searchScope_fromSearchSession() {
		SearchMapping searchMapping = theSearchMapping;
		try ( SearchSession searchSession = searchMapping.createSession() ) {
			SearchScope<Book> bookScope = searchSession.scope( Book.class );
			SearchScope<Person> associateAndManagerScope =
					searchSession.scope( Arrays.asList( Associate.class, Manager.class ) );
			SearchScope<Person> personScope = searchSession.scope( Person.class );
			SearchScope<Person> personSubTypesScope = searchSession.scope( Person.class,
					Arrays.asList( "Manager", "Associate" ) );
			SearchScope<Object> allScope = searchSession.scope( Object.class );
			assertThat( bookScope.includedTypes() )
					.extracting( SearchIndexedEntity::name )
					.containsExactlyInAnyOrder( "Book" );
			assertThat( associateAndManagerScope.includedTypes() )
					.extracting( SearchIndexedEntity::name )
					.containsExactlyInAnyOrder( "Manager", "Associate" );
			assertThat( personScope.includedTypes() )
					.extracting( SearchIndexedEntity::name )
					.containsExactlyInAnyOrder( "Manager", "Associate" );
			assertThat( personSubTypesScope.includedTypes() )
					.extracting( SearchIndexedEntity::name )
					.containsExactlyInAnyOrder( "Manager", "Associate" );
			assertThat( allScope.includedTypes() )
					.extracting( SearchIndexedEntity::name )
					.containsExactlyInAnyOrder( "Book", "Manager", "Associate" );
		}
	}

}
