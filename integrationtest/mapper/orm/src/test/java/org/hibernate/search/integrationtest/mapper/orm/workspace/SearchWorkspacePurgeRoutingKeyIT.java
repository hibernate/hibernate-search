/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.workspace;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import org.hibernate.search.mapper.orm.work.SearchWorkspace;
import org.hibernate.search.util.common.impl.CollectionHelper;
import org.hibernate.search.util.impl.integrationtest.common.extension.BackendMock;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

@TestForIssue(jiraKey = "HSEARCH-3049")
public class SearchWorkspacePurgeRoutingKeyIT extends AbstractSearchWorkspaceSimpleOperationIT {

	private final Set<String> ROUTING_KEYS = CollectionHelper.asLinkedHashSet( "key1", "key2" );

	@Override
	protected void expectWork(BackendMock backendMock, String indexName, CompletableFuture<?> future) {
		backendMock.expectIndexScaleWorks( indexName )
				.purge( ROUTING_KEYS, future );
	}

	@Override
	protected void executeSync(SearchWorkspace workspace) {
		workspace.purge( ROUTING_KEYS );
	}

	@Override
	protected CompletionStage<?> executeAsync(SearchWorkspace workspace) {
		return workspace.purgeAsync( ROUTING_KEYS );
	}
}
