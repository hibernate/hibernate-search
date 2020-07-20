/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.pojo.work;

import java.util.concurrent.CompletableFuture;

import org.hibernate.search.engine.backend.work.execution.DocumentCommitStrategy;
import org.hibernate.search.engine.backend.work.execution.DocumentRefreshStrategy;
import org.hibernate.search.mapper.javabean.work.SearchIndexer;
import org.hibernate.search.mapper.javabean.work.SearchIndexingPlan;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;

public class PojoIndexingPurgeIT extends AbstractPojoIndexingOperationIT {

	public PojoIndexingPurgeIT(DocumentCommitStrategy commitStrategy,
			DocumentRefreshStrategy refreshStrategy, String tenantId) {
		super( commitStrategy, refreshStrategy, tenantId );
	}

	@Override
	protected void expectOperation(BackendMock.DocumentWorkCallListContext context, String tenantId,
			String id, String routingKey, String value) {
		context.delete( b -> addWorkInfo( b, tenantId, id, routingKey ) );
	}

	@Override
	protected void addTo(SearchIndexingPlan indexingPlan, int id) {
		indexingPlan.purge( IndexedEntity.class, id, null );
	}

	@Override
	protected void addTo(SearchIndexingPlan indexingPlan, Object providedId, int id) {
		indexingPlan.purge( IndexedEntity.class, providedId, null );
	}

	@Override
	protected void addTo(SearchIndexingPlan indexingPlan, Object providedId, String providedRoutingKey, int id) {
		indexingPlan.purge( IndexedEntity.class, providedId, providedRoutingKey );
	}

	@Override
	protected CompletableFuture<?> execute(SearchIndexer indexer, int id) {
		return indexer.purge( IndexedEntity.class, id, null );
	}

	@Override
	protected CompletableFuture<?> execute(SearchIndexer indexer, Object providedId, int id) {
		return indexer.purge( IndexedEntity.class, providedId, null );
	}

	@Override
	protected CompletableFuture<?> execute(SearchIndexer indexer, Object providedId, String providedRoutingKey, int id) {
		return indexer.purge( IndexedEntity.class, providedId, providedRoutingKey );
	}
}
