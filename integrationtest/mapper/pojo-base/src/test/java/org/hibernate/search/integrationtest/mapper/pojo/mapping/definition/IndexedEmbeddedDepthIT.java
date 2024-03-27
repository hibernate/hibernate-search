/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.pojo.mapping.definition;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.SearchEntity;
import org.hibernate.search.util.impl.integrationtest.common.extension.BackendMock;
import org.hibernate.search.util.impl.integrationtest.mapper.pojo.standalone.StandalonePojoMappingSetupHelper;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class IndexedEmbeddedDepthIT {

	private static final String INDEX_NAME = "IndexName";

	@RegisterExtension
	public BackendMock backendMock = BackendMock.create();

	@RegisterExtension
	public StandalonePojoMappingSetupHelper setupHelper =
			StandalonePojoMappingSetupHelper.withBackendMock( MethodHandles.lookup(), backendMock );

	@Test
	@TestForIssue(jiraKey = "HSEARCH-1467")
	void includeDepth_differentDepths() {
		class IndexedEmbeddedLevel2 {
			@GenericField
			String level2Property;
		}
		class IndexedEmbeddedLevel1 {
			@GenericField
			String level1Property;
			@IndexedEmbedded
			IndexedEmbeddedLevel2 level2;
		}
		@SearchEntity
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			Integer id;
			@IndexedEmbedded(includeDepth = 1)
			IndexedEmbeddedLevel1 level1Depth1;
			@IndexedEmbedded(includeDepth = 2)
			IndexedEmbeddedLevel1 level1Depth2;

			public IndexedEntity(int id, String level1Value, String level2Value) {
				this.id = id;
				level1Depth1 = create( level1Value, level2Value );
				level1Depth2 = create( level1Value, level2Value );
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
				.withAnnotatedTypes( IndexedEntity.class )
				.withAnnotatedTypes( IndexedEmbeddedLevel1.class, IndexedEmbeddedLevel2.class )
				.setup();
		backendMock.verifyExpectationsMet();
	}
}
