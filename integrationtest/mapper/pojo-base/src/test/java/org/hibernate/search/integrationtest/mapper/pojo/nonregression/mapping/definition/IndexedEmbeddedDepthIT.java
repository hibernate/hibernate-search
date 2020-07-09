/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.pojo.nonregression.mapping.definition;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.integrationtest.mapper.pojo.testsupport.util.rule.JavaBeanMappingSetupHelper;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Rule;
import org.junit.Test;

public class IndexedEmbeddedDepthIT {

	private static final String INDEX_NAME = "IndexName";

	@Rule
	public BackendMock backendMock = new BackendMock();

	@Rule
	public JavaBeanMappingSetupHelper setupHelper = JavaBeanMappingSetupHelper.withBackendMock( MethodHandles.lookup(), backendMock );

	@Test
	@TestForIssue(jiraKey = "HSEARCH-1467")
	public void maxDepth_differentDepths() {
		class IndexedEmbeddedLevel2 {
			String level2Property;
			@GenericField
			public String getLevel2Property() {
				return level2Property;
			}
		}
		class IndexedEmbeddedLevel1 {
			String level1Property;
			IndexedEmbeddedLevel2 level2;
			@GenericField
			public String getLevel1Property() {
				return level1Property;
			}
			@IndexedEmbedded
			public IndexedEmbeddedLevel2 getLevel2() {
				return level2;
			}
		}
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			Integer id;
			IndexedEmbeddedLevel1 level1Depth1;
			IndexedEmbeddedLevel1 level1Depth2;

			public IndexedEntity(int id, String level1Value, String level2Value) {
				this.id = id;
				level1Depth1 = create( level1Value, level2Value );
				level1Depth2 = create( level1Value, level2Value );
			}

			@DocumentId
			public Integer getId() {
				return id;
			}
			@IndexedEmbedded(maxDepth = 1)
			public IndexedEmbeddedLevel1 getLevel1Depth1() {
				return level1Depth1;
			}
			@IndexedEmbedded(maxDepth = 2)
			public IndexedEmbeddedLevel1 getLevel1Depth2() {
				return level1Depth2;
			}

			private IndexedEmbeddedLevel1 create(String level1Value, String level2Value) {
				IndexedEmbeddedLevel1 level = new IndexedEmbeddedLevel1();
				level.level1Property = level1Value;
				level.level2 = new IndexedEmbeddedLevel2();
				level.level2.level2Property = level2Value;
				return level;
			}
		}

		backendMock.expectSchema( INDEX_NAME, b -> b
				.objectField( "level1Depth1", b2 -> b2
						.field( "level1Property", String.class )
				)
				.objectField( "level1Depth2", b2 -> b2
						.field( "level1Property", String.class )
						.objectField( "level2", b3 -> b3
								.field( "level2Property", String.class )
						)
				)
		);
		setupHelper.start()
				.withAnnotatedEntityTypes( IndexedEntity.class )
				.withAnnotatedTypes( IndexedEmbeddedLevel1.class, IndexedEmbeddedLevel2.class )
				.setup();
		backendMock.verifyExpectationsMet();
	}
}
