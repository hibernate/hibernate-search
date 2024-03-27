/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.pojo.work.operations;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.hibernate.search.engine.backend.work.execution.DocumentCommitStrategy;
import org.hibernate.search.engine.backend.work.execution.DocumentRefreshStrategy;
import org.hibernate.search.mapper.pojo.standalone.session.SearchSession;
import org.hibernate.search.mapper.pojo.standalone.work.SearchIndexer;
import org.hibernate.search.mapper.pojo.work.IndexingPlanSynchronizationStrategy;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Tests of individual operations in {@link org.hibernate.search.mapper.pojo.work.spi.PojoIndexer}
 * when the entity passed to the operation is null.
 */
@TestForIssue(jiraKey = "HSEARCH-4153")
public abstract class AbstractPojoIndexerAddOrUpdateNullEntityIT extends AbstractPojoIndexingOperationIT {

	@ParameterizedTest(name = "commit: {0}, refresh: {1}, tenantID: {2}, routing: {3}")
	@MethodSource("params")
	void simple(DocumentCommitStrategy commitStrategy, DocumentRefreshStrategy refreshStrategy, String tenantId,
			MyRoutingBinder routingBinder, IndexingPlanSynchronizationStrategy strategy) {
		setup( commitStrategy, refreshStrategy, tenantId, routingBinder, strategy );
		try ( SearchSession session = createSession() ) {
			SearchIndexer indexer = session.indexer();

			assertThatThrownBy( () -> scenario().execute( indexer, 42 ) )
					.isInstanceOf( SearchException.class )
					.hasMessageContaining( "Invalid indexing request",
							"the add and update operations require a non-null entity" );
		}
	}

	@Override
	protected boolean isImplicitRoutingEnabled() {
		// Entities are null and are not implicitly loaded, so implicit routing simply cannot work.
		return false;
	}

}
