/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.documentation.mapper.pojo.standalone.entitydefinition;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Set;

import org.hibernate.search.documentation.testsupport.BackendConfigurations;
import org.hibernate.search.documentation.testsupport.TestConfiguration;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.AnnotatedTypeSource;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.TypeMappingStep;
import org.hibernate.search.mapper.pojo.standalone.cfg.StandalonePojoMapperSettings;
import org.hibernate.search.mapper.pojo.standalone.entity.SearchIndexedEntity;
import org.hibernate.search.mapper.pojo.standalone.mapping.CloseableSearchMapping;
import org.hibernate.search.mapper.pojo.standalone.mapping.SearchMapping;
import org.hibernate.search.mapper.pojo.standalone.mapping.StandalonePojoMappingConfigurationContext;
import org.hibernate.search.mapper.pojo.standalone.mapping.StandalonePojoMappingConfigurer;
import org.hibernate.search.util.impl.integrationtest.common.TestConfigurationProvider;

import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class StandalonePojoSearchEntityIT {

	@RegisterExtension
	public TestConfigurationProvider configurationProvider = new TestConfigurationProvider();

	private CloseableSearchMapping searchMapping;

	public static List<? extends Arguments> params() {
		return List.of(
				Arguments.of( new StandalonePojoMappingConfigurer() {
					@Override
					public String toString() {
						return "annotation mapping";
					}

					@Override
					public void configure(StandalonePojoMappingConfigurationContext context) {
						context.annotationMapping().add( Set.of( Book.class, Author.class ) );
					}
				} ),
				Arguments.of( new StandalonePojoMappingConfigurer() {
					@Override
					public String toString() {
						return "programmatic mapping";
					}

					@Override
					public void configure(StandalonePojoMappingConfigurationContext context) {
						var mapping = context.programmaticMapping();
						//tag::programmatic[]
						TypeMappingStep bookMapping = mapping.type( Book.class );
						bookMapping.searchEntity();
						TypeMappingStep authorMapping = mapping.type( Author.class );
						authorMapping.searchEntity().name( "MyAuthorName" );
						//end::programmatic[]
					}
				} )
		);
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("params")
	void test(StandalonePojoMappingConfigurer configurer) {
		this.searchMapping = SearchMapping.builder( AnnotatedTypeSource.empty() )
				.property( StandalonePojoMapperSettings.MAPPING_CONFIGURER,
						configurer )
				.properties( TestConfiguration.standalonePojoMapperProperties(
						configurationProvider,
						BackendConfigurations.simple()
				) )
				.build();

		assertThat( searchMapping.allIndexedEntities() )
				.extracting( SearchIndexedEntity::name )
				.containsExactlyInAnyOrder( "Book", "MyAuthorName" );
	}

}
