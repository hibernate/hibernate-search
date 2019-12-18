/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.workspace;

import java.util.concurrent.CompletableFuture;

import org.hibernate.search.mapper.orm.work.SearchWorkspace;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

@TestForIssue(jiraKey = "HSEARCH-3049")
public class SearchWorkspaceMergeSegmentsIT extends AbstractSearchWorkspaceSimpleOperationIT {
	@Override
	protected void expectWork(BackendMock backendMock, String indexName, CompletableFuture<?> future) {
		backendMock.expectIndexScopeWorks( indexName )
				.mergeSegments( future );
	}

	@Override
	protected void executeSync(SearchWorkspace workspace) {
		workspace.mergeSegments();
	}

	@Override
	protected CompletableFuture<?> executeAsync(SearchWorkspace workspace) {
		return workspace.mergeSegmentsAsync();
	}
}
