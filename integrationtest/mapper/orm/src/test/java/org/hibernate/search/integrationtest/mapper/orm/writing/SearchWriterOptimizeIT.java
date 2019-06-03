/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.writing;

import java.util.concurrent.CompletableFuture;

import org.hibernate.search.mapper.orm.writing.SearchWriter;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

@TestForIssue(jiraKey = "HSEARCH-3049")
public class SearchWriterOptimizeIT extends AbstractSearchWriterSimpleOperationIT {
	@Override
	protected void expectWork(BackendMock backendMock, String indexName, CompletableFuture<?> future) {
		backendMock.expectWorks( indexName )
				.optimize()
				.executed( future );
	}

	@Override
	protected void executeSync(SearchWriter writer) {
		writer.optimize();
	}

	@Override
	protected CompletableFuture<?> executeAsync(SearchWriter writer) {
		return writer.optimizeAsync();
	}
}
