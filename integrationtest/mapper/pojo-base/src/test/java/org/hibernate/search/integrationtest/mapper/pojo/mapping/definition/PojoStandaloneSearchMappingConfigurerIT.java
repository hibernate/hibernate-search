/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.pojo.mapping.definition;

import java.lang.invoke.MethodHandles;
import java.util.Arrays;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.KeywordField;
import org.hibernate.search.mapper.pojo.standalone.cfg.StandalonePojoMapperSettings;
import org.hibernate.search.mapper.pojo.standalone.mapping.StandalonePojoMappingConfigurationContext;
import org.hibernate.search.mapper.pojo.standalone.mapping.StandalonePojoMappingConfigurer;
import org.hibernate.search.util.impl.integrationtest.common.extension.BackendMock;
import org.hibernate.search.util.impl.integrationtest.mapper.pojo.standalone.StandalonePojoMappingSetupHelper;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class PojoStandaloneSearchMappingConfigurerIT {

	@RegisterExtension
	public BackendMock backendMock = BackendMock.create();

	@RegisterExtension
	public StandalonePojoMappingSetupHelper setupHelper =
			StandalonePojoMappingSetupHelper.withBackendMock( MethodHandles.lookup(), backendMock );

	@Test
	void none() {
		backendMock.expectSchema( IndexedEntity.INDEX, b -> b
				.field( "annotationMapped", String.class )
		);

		setupHelper.start()
				.setup( IndexedEntity.class );
		backendMock.verifyExpectationsMet();
	}

	@Test
	void single() {
		backendMock.expectSchema( IndexedEntity.INDEX, b -> b
				.field( "annotationMapped", String.class )
				.field( "nonAnnotationMapped1", String.class )
		);

		setupHelper.start()
				.expectCustomBeans()
				.withProperty( StandalonePojoMapperSettings.MAPPING_CONFIGURER,
						MappingConfigurer1.class.getName() )
				.setup( IndexedEntity.class );
		backendMock.verifyExpectationsMet();
	}

	public static class MappingConfigurer1 implements StandalonePojoMappingConfigurer {
		@Override
		public void configure(StandalonePojoMappingConfigurationContext context) {
			context.programmaticMapping().type( IndexedEntity.class )
					.property( "nonAnnotationMapped1" )
					.keywordField();
		}
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-4594")
	void multiple() {
		backendMock.expectSchema( IndexedEntity.INDEX, b -> b
				.field( "annotationMapped", String.class )
				.field( "nonAnnotationMapped1", String.class )
				.field( "nonAnnotationMapped2", String.class )
		);

		setupHelper.start()
				.expectCustomBeans()
				.withProperty(
						StandalonePojoMapperSettings.MAPPING_CONFIGURER,
						Arrays.asList( MappingConfigurer1.class.getName(), MappingConfigurer2.class.getName() )
				)
				.setup( IndexedEntity.class );
		backendMock.verifyExpectationsMet();
	}

	public static class MappingConfigurer2 implements StandalonePojoMappingConfigurer {
		@Override
		public void configure(StandalonePojoMappingConfigurationContext context) {
			context.programmaticMapping().type( IndexedEntity.class )
					.property( "nonAnnotationMapped2" )
					.keywordField();
		}
	}

	@Indexed(index = IndexedEntity.INDEX)
	public static final class IndexedEntity {
		public static final String INDEX = "IndexedEntity";

		@DocumentId
		private Integer id;

		@KeywordField
		private String annotationMapped;

		private String nonAnnotationMapped1;

		private String nonAnnotationMapped2;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getAnnotationMapped() {
			return annotationMapped;
		}

		public void setAnnotationMapped(String annotationMapped) {
			this.annotationMapped = annotationMapped;
		}

		public String getNonAnnotationMapped1() {
			return nonAnnotationMapped1;
		}

		public void setNonAnnotationMapped1(String nonAnnotationMapped1) {
			this.nonAnnotationMapped1 = nonAnnotationMapped1;
		}

		public String getNonAnnotationMapped2() {
			return nonAnnotationMapped2;
		}

		public void setNonAnnotationMapped2(String nonAnnotationMapped2) {
			this.nonAnnotationMapped2 = nonAnnotationMapped2;
		}
	}

}
