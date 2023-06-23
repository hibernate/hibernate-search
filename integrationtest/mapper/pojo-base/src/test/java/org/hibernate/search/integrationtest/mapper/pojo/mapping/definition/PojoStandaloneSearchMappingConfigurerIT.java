/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.integrationtest.mapper.pojo.standalone.StandalonePojoMappingSetupHelper;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Rule;
import org.junit.Test;

public class PojoStandaloneSearchMappingConfigurerIT {

	@Rule
	public BackendMock backendMock = new BackendMock();

	@Rule
	public StandalonePojoMappingSetupHelper setupHelper =
			StandalonePojoMappingSetupHelper.withBackendMock( MethodHandles.lookup(), backendMock );

	@Test
	public void none() {
		backendMock.expectSchema( IndexedEntity.INDEX, b -> b
				.field( "annotationMapped", String.class )
		);

		setupHelper.start()
				.setup( IndexedEntity.class );
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void single() {
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
	public void multiple() {
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
