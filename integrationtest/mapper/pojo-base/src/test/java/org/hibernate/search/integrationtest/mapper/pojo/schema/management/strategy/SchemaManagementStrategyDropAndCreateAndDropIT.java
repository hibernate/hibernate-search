/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.pojo.schema.management.strategy;

import java.util.concurrent.CompletableFuture;

import org.hibernate.search.util.impl.integrationtest.common.rule.SchemaManagementWorkBehavior;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.index.StubSchemaManagementWork;
import org.hibernate.search.util.impl.test.rule.ExpectedLog4jLog;

import org.junit.Rule;
import org.hibernate.search.mapper.javabean.schema.management.SchemaManagementStrategyName;

public class SchemaManagementStrategyDropAndCreateAndDropIT extends AbstractSchemaManagementStrategyIT {

	@Rule
	public ExpectedLog4jLog logged = ExpectedLog4jLog.create();

	@Override
	protected SchemaManagementStrategyName getStrategyName() {
		return SchemaManagementStrategyName.DROP_AND_CREATE_AND_DROP;
	}

	@Override
	protected void expectWork(String indexName, SchemaManagementWorkBehavior behavior) {
		backendMock.expectSchemaManagementWorks( indexName )
				.work( StubSchemaManagementWork.Type.DROP_AND_CREATE, behavior );
	}

	@Override
	protected void expectOnClose(String indexName) {
		backendMock.expectSchemaManagementWorks( indexName )
				.work( StubSchemaManagementWork.Type.DROP_IF_EXISTING );
	}

	protected void expectOnClose(String indexName, CompletableFuture<?> future) {
		backendMock.expectSchemaManagementWorks( indexName )
				.work( StubSchemaManagementWork.Type.DROP_IF_EXISTING, future );
	}
}
