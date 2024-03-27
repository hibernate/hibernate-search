/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.orm.schema.management.manager;

import org.hibernate.search.mapper.orm.schema.management.SearchSchemaManager;
import org.hibernate.search.util.impl.integrationtest.common.extension.SchemaManagementWorkBehavior;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.index.StubSchemaManagementWork;

class SearchSchemaManagerValidateIT extends AbstractSearchSchemaManagerValidatingSimpleOperationIT {

	@Override
	protected void execute(SearchSchemaManager manager) {
		manager.validate();
	}

	@Override
	protected void expectWork(String indexName, SchemaManagementWorkBehavior behavior) {
		backendMock.expectSchemaManagementWorks( indexName )
				.work( StubSchemaManagementWork.Type.VALIDATE, behavior );
	}

	@Override
	protected void expectOnClose(String indexName) {
		// No expectation
	}
}
