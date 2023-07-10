/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.documentation.mapper.pojo.standalone.entrypoints;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

import org.hibernate.search.documentation.testsupport.BackendConfigurations;
import org.hibernate.search.documentation.testsupport.TestConfiguration;
import org.hibernate.search.mapper.pojo.standalone.entity.SearchIndexedEntity;
import org.hibernate.search.mapper.pojo.standalone.mapping.CloseableSearchMapping;
import org.hibernate.search.mapper.pojo.standalone.mapping.SearchMapping;
import org.hibernate.search.mapper.pojo.standalone.session.SearchSession;
import org.hibernate.search.util.impl.integrationtest.common.TestConfigurationProvider;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class StandalonePojoReadPropertiesFromFileIT {

	private CloseableSearchMapping theSearchMapping;

	@Rule
	public TestConfigurationProvider configurationProvider = new TestConfigurationProvider();

	@Before
	public void setup() throws IOException {
		// tag::setup[]
		try (
				Reader propertyFileReader = /* ... */ // <1>
						// end::setup[]
						new InputStreamReader(
								getClass().getClassLoader().getResourceAsStream( "configuration/standalone-test.properties" ),
								StandardCharsets.UTF_8
						)
		// tag::setup[]
		) {
			CloseableSearchMapping searchMapping = SearchMapping.builder() // <2>
					.properties( propertyFileReader ) // <3>
					// end::setup[]
					.properties( TestConfiguration.standalonePojoMapperProperties(
							configurationProvider,
							BackendConfigurations.simple()
					) )
					// tag::setup[]
					.build();
			// end::setup[]
			this.theSearchMapping = searchMapping;
			// tag::setup[]
		}
		// end::setup[]
	}

	@After
	public void cleanup() {
		if ( theSearchMapping != null ) {
			CloseableSearchMapping searchMapping = theSearchMapping;
			searchMapping.close();
		}
	}

	@Test
	public void mappingContainsExpectedEntities() {
		assertThat( theSearchMapping.allIndexedEntities() )
				.extracting( SearchIndexedEntity::name )
				.contains( "Book", "Associate", "Manager" );
	}

	@Test
	public void searchSession() {
		SearchMapping searchMapping = theSearchMapping;
		try ( SearchSession searchSession = searchMapping.createSession() ) {
			assertThat( searchSession ).isNotNull();
			assertThat( searchSession.isOpen() ).isTrue();
		}
	}
}
