/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.pojo.schema.management.strategy;

import org.hibernate.search.mapper.pojo.standalone.schema.management.SchemaManagementStrategyName;
import org.hibernate.search.util.impl.integrationtest.common.extension.SchemaManagementWorkBehavior;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.index.StubSchemaManagementWork;

class SchemaManagementStrategyDropAndCreateIT extends AbstractSchemaManagementStrategyIT {

	@Override
	protected SchemaManagementStrategyName getStrategyName() {
		return SchemaManagementStrategyName.DROP_AND_CREATE;
	}

	@Override
	protected void expectWork(String indexName, SchemaManagementWorkBehavior behavior) {
		backendMock.expectSchemaManagementWorks( indexName )
				.work( StubSchemaManagementWork.Type.DROP_AND_CREATE, behavior );
	}

	@Override
	protected void expectOnClose(String indexName) {
		// No expectation
	}

}
