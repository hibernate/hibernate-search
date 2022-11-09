/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.documentation.mapper.pojo.standalone.entrypoints;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;

import org.hibernate.search.documentation.testsupport.BackendConfigurations;
import org.hibernate.search.documentation.testsupport.TestConfiguration;
import org.hibernate.search.engine.backend.work.execution.DocumentCommitStrategy;
import org.hibernate.search.engine.backend.work.execution.DocumentRefreshStrategy;
import org.hibernate.search.mapper.pojo.standalone.entity.SearchIndexedEntity;
import org.hibernate.search.mapper.pojo.standalone.mapping.CloseableSearchMapping;
import org.hibernate.search.mapper.pojo.standalone.mapping.SearchMapping;
import org.hibernate.search.mapper.pojo.standalone.scope.SearchScope;
import org.hibernate.search.mapper.pojo.standalone.session.SearchSession;
import org.hibernate.search.util.impl.integrationtest.common.TestConfigurationProvider;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class StandalonePojoEntryPointsIT {

	private CloseableSearchMapping theSearchMapping;

	@Rule
	public TestConfigurationProvider configurationProvider = new TestConfigurationProvider();

	@Before
	public void setup() {
		// tag::setup[]
		CloseableSearchMapping searchMapping = SearchMapping.builder() // <1>
				.property(
						"hibernate.search.mapping.configurer", // <2>
						"class:org.hibernate.search.documentation.mapper.pojo.standalone.entrypoints.StandalonePojoConfigurer"
				)
				.property(
						"hibernate.search.backend.hosts", // <3>
						"elasticsearch.mycompany.com"
				)
				// end::setup[]
				.properties( TestConfiguration.standalonePojoMapperProperties(
						configurationProvider,
						BackendConfigurations.simple()
				) )
				// tag::setup[]
				.build(); // <4>
		// end::setup[]
		this.theSearchMapping = searchMapping;
	}

	@After
	public void cleanup() {
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
	public void mappingContainsExpectedEntities() {
		assertThat( theSearchMapping.allIndexedEntities() )
				.extracting( SearchIndexedEntity::name )
				.contains( "Book", "Associate", "Manager" )
		;
	}

	@Test
	public void searchSession() {
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
	public void searchSession_withOptions() {
		// tag::searchSession-withOptions[]
		SearchMapping searchMapping = /* ... */ // <1>
				// end::searchSession-withOptions[]
				theSearchMapping;
		// tag::searchSession-withOptions[]
		try ( SearchSession searchSession = searchMapping.createSessionWithOptions() // <2>
				.commitStrategy( DocumentCommitStrategy.FORCE ) // <3>
				.refreshStrategy( DocumentRefreshStrategy.FORCE )
				.tenantId( "myTenant" )
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
	public void searchScope_fromSearchMapping() {
		SearchMapping searchMapping = theSearchMapping;
		// tag::searchScope-fromSearchMapping[]
		SearchScope<Book> bookScope = searchMapping.scope( Book.class );
		SearchScope<Person> associateAndManagerScope = searchMapping.scope( Arrays.asList( Associate.class, Manager.class ) );
		SearchScope<Person> personScope = searchMapping.scope( Person.class );
		SearchScope<Object> allScope = searchMapping.scope( Object.class );
		// end::searchScope-fromSearchMapping[]
		assertThat( bookScope.includedTypes() )
				.extracting( SearchIndexedEntity::name )
				.containsExactlyInAnyOrder( "Book" );
		assertThat( associateAndManagerScope.includedTypes() )
				.extracting( SearchIndexedEntity::name )
				.containsExactlyInAnyOrder( "Manager", "Associate" );
		assertThat( personScope.includedTypes() )
				.extracting( SearchIndexedEntity::name )
				.containsExactlyInAnyOrder( "Manager", "Associate" );
		assertThat( allScope.includedTypes() )
				.extracting( SearchIndexedEntity::name )
				.containsExactlyInAnyOrder( "Book", "Manager", "Associate" );
	}

	@Test
	public void searchScope_fromSearchSession() {
		SearchMapping searchMapping = theSearchMapping;
		try ( SearchSession searchSession = searchMapping.createSession() ) {
			SearchScope<Book> bookScope = searchSession.scope( Book.class );
			SearchScope<Person> associateAndManagerScope = searchSession.scope( Arrays.asList( Associate.class, Manager.class ) );
			SearchScope<Person> personScope = searchSession.scope( Person.class );
			SearchScope<Object> allScope = searchSession.scope( Object.class );
			// end::searchScope-fromSearchSession[]
			assertThat( bookScope.includedTypes() )
					.extracting( SearchIndexedEntity::name )
					.containsExactlyInAnyOrder( "Book" );
			assertThat( associateAndManagerScope.includedTypes() )
					.extracting( SearchIndexedEntity::name )
					.containsExactlyInAnyOrder( "Manager", "Associate" );
			assertThat( personScope.includedTypes() )
					.extracting( SearchIndexedEntity::name )
					.containsExactlyInAnyOrder( "Manager", "Associate" );
			assertThat( allScope.includedTypes() )
					.extracting( SearchIndexedEntity::name )
					.containsExactlyInAnyOrder( "Book", "Manager", "Associate" );
		}
	}

}
