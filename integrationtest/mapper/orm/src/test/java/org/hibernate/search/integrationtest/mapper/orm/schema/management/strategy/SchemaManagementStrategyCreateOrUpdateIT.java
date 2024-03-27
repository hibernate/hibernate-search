/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.orm.schema.management.strategy;

import org.hibernate.search.mapper.orm.schema.management.SchemaManagementStrategyName;
import org.hibernate.search.util.impl.integrationtest.common.extension.SchemaManagementWorkBehavior;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.index.StubSchemaManagementWork;

public class SchemaManagementStrategyCreateOrUpdateIT extends AbstractSchemaManagementStrategyIT {

	@Override
	protected SchemaManagementStrategyName getStrategyName() {
		return SchemaManagementStrategyName.CREATE_OR_UPDATE;
	}

	@Override
	protected void expectWork(String indexName, SchemaManagementWorkBehavior behavior) {
		backendMock.expectSchemaManagementWorks( indexName )
				.work( StubSchemaManagementWork.Type.CREATE_OR_UPDATE, behavior );
	}

	@Override
	protected void expectOnClose(String indexName) {
		// No expectation
	}
}
