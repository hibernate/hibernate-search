/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.pojo.workspace;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import org.hibernate.search.mapper.pojo.standalone.work.SearchWorkspace;
import org.hibernate.search.util.impl.integrationtest.common.extension.BackendMock;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

@TestForIssue(jiraKey = "HSEARCH-4621")
class SearchWorkspaceFlushIT extends AbstractSearchWorkspaceSimpleOperationIT {
	@Override
	protected void expectWork(BackendMock backendMock, String indexName, CompletableFuture<?> future) {
		backendMock.expectIndexScaleWorks( indexName )
				.flush( future );
	}

	@Override
	protected void executeSync(SearchWorkspace workspace) {
		workspace.flush();
	}

	@Override
	protected CompletionStage<?> executeAsync(SearchWorkspace workspace) {
		return workspace.flushAsync();
	}
}
