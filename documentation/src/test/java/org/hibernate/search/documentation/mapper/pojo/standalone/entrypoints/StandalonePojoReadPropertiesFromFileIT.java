/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.documentation.mapper.pojo.standalone.entrypoints;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

import org.hibernate.search.documentation.testsupport.BackendConfigurations;
import org.hibernate.search.documentation.testsupport.TestConfiguration;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.AnnotatedTypeSource;
import org.hibernate.search.mapper.pojo.standalone.entity.SearchIndexedEntity;
import org.hibernate.search.mapper.pojo.standalone.mapping.CloseableSearchMapping;
import org.hibernate.search.mapper.pojo.standalone.mapping.SearchMapping;
import org.hibernate.search.mapper.pojo.standalone.session.SearchSession;
import org.hibernate.search.util.impl.integrationtest.common.TestConfigurationProvider;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class StandalonePojoReadPropertiesFromFileIT {

	private CloseableSearchMapping theSearchMapping;

	@RegisterExtension
	public TestConfigurationProvider configurationProvider = new TestConfigurationProvider();

	@BeforeEach
	void setup() throws IOException {
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
			CloseableSearchMapping searchMapping = SearchMapping.builder( AnnotatedTypeSource.empty() ) // <2>
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

	@AfterEach
	void cleanup() {
		if ( theSearchMapping != null ) {
			CloseableSearchMapping searchMapping = theSearchMapping;
			searchMapping.close();
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
		SearchMapping searchMapping = theSearchMapping;
		try ( SearchSession searchSession = searchMapping.createSession() ) {
			assertThat( searchSession ).isNotNull();
			assertThat( searchSession.isOpen() ).isTrue();
		}
	}
}
