/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.pojo.schema.management.strategy;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.standalone.cfg.StandalonePojoMapperSettings;
import org.hibernate.search.mapper.pojo.standalone.mapping.CloseableSearchMapping;
import org.hibernate.search.mapper.pojo.standalone.schema.management.SchemaManagementStrategyName;
import org.hibernate.search.util.impl.integrationtest.common.extension.BackendMock;
import org.hibernate.search.util.impl.integrationtest.mapper.pojo.standalone.StandalonePojoMappingSetupHelper;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class SchemaManagementStrategyNoneIT {

	@RegisterExtension
	public BackendMock backendMock = BackendMock.create();

	@RegisterExtension
	public final StandalonePojoMappingSetupHelper setupHelper =
			StandalonePojoMappingSetupHelper.withBackendMock( MethodHandles.lookup(), backendMock );

	@Test
	void none() {
		CloseableSearchMapping mapper = setup();
		// Nothing should have happened
		backendMock.verifyExpectationsMet();

		mapper.close();
		// Nothing should have happened
		backendMock.verifyExpectationsMet();
	}

	protected final CloseableSearchMapping setup() {
		backendMock.expectAnySchema( IndexedEntity1.NAME );
		backendMock.expectAnySchema( IndexedEntity2.NAME );
		return (CloseableSearchMapping) setupHelper.start()
				.withProperty( StandalonePojoMapperSettings.SCHEMA_MANAGEMENT_STRATEGY,
						getStrategyName()
				)
				.setup( IndexedEntity1.class, IndexedEntity2.class );
	}

	protected String getStrategyName() {
		return SchemaManagementStrategyName.NONE.externalRepresentation();
	}

	@Indexed(index = IndexedEntity1.NAME)
	static class IndexedEntity1 {

		static final String NAME = "indexed1";

		@DocumentId
		private Integer id;
	}

	@Indexed(index = IndexedEntity2.NAME)
	static class IndexedEntity2 {

		static final String NAME = "indexed2";

		@DocumentId
		private Integer id;
	}
}
