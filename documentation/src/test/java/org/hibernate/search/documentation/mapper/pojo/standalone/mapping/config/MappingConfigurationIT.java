/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.documentation.mapper.pojo.standalone.mapping.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.hibernate.search.documentation.testsupport.BackendConfigurations;
import org.hibernate.search.documentation.testsupport.TestConfiguration;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.AnnotatedTypeSource;
import org.hibernate.search.mapper.pojo.standalone.mapping.CloseableSearchMapping;
import org.hibernate.search.mapper.pojo.standalone.mapping.SearchMapping;
import org.hibernate.search.mapper.pojo.standalone.mapping.SearchMappingBuilder;
import org.hibernate.search.mapper.pojo.standalone.session.SearchSession;
import org.hibernate.search.mapper.pojo.work.IndexingPlanSynchronizationStrategy;
import org.hibernate.search.util.impl.integrationtest.common.TestConfigurationProvider;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class MappingConfigurationIT {

	@RegisterExtension
	public TestConfigurationProvider configurationProvider = new TestConfigurationProvider();

	private CloseableSearchMapping searchMapping;

	@BeforeEach
	void setup() {
		SearchMappingBuilder builder = SearchMapping.builder( AnnotatedTypeSource.empty() )
				.properties( TestConfiguration.standalonePojoMapperProperties(
						configurationProvider,
						BackendConfigurations.simple()
				) )
				.property(
						"hibernate.search.mapping.configurer",
						MySearchMappingConfigurer.class.getName()
				);

		CloseableSearchMapping searchMapping = builder.build();
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
	void simple() {
		try ( SearchSession session = searchMapping.createSessionWithOptions()
				.indexingPlanSynchronizationStrategy( IndexingPlanSynchronizationStrategy.sync() )
				.build() ) {
			Book book = new Book();
			book.setId( 1 );
			book.setTitle( "The Caves Of Steel" );

			session.indexingPlan().add( book );
		}

		try ( SearchSession session = searchMapping.createSession() ) {
			List<Integer> result = session.search( Book.class )
					.select( f -> f.id( Integer.class ) )
					.where( f -> f.match().field( "title" ).matching( "steel" ) )
					.fetchHits( 20 );
			assertThat( result ).hasSize( 1 );
		}
	}
}
