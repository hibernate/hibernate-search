/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.pojo.schema.management.manager;

import org.hibernate.search.mapper.pojo.standalone.schema.management.SearchSchemaManager;
import org.hibernate.search.util.impl.integrationtest.common.rule.SchemaManagementWorkBehavior;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.index.StubSchemaManagementWork;

public class SearchSchemaManagerDropAndCreateIT extends AbstractSearchSchemaManagerSimpleOperationIT {

	@Override
	protected void execute(SearchSchemaManager manager) {
		manager.dropAndCreate();
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
