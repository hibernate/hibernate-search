/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.locks.Lock;

import org.hibernate.search.backend.IndexingMonitor;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.backend.elasticsearch.client.impl.JestClient;
import org.hibernate.search.backend.spi.BackendQueueProcessor;
import org.hibernate.search.engine.integration.impl.ExtendedSearchIntegrator;
import org.hibernate.search.engine.service.spi.ServiceReference;
import org.hibernate.search.indexes.spi.IndexManager;
import org.hibernate.search.spi.WorkerBuildContext;

import io.searchbox.action.BulkableAction;
import io.searchbox.core.DeleteByQuery;
import io.searchbox.indices.Refresh;

/**
 * {@link BackendQueueProcessor} applying index changes to an Elasticsearch server.
 *
 * @author Gunnar Morling
 */
public class ElasticsearchBackendQueueProcessor implements BackendQueueProcessor {

	private ElasticsearchIndexManager indexManager;
	private ExtendedSearchIntegrator searchIntegrator;
	private ElasticsearchIndexWorkVisitor visitor;

	@Override
	public void initialize(Properties props, WorkerBuildContext context, IndexManager indexManager) {
		this.indexManager = (ElasticsearchIndexManager) indexManager;
		this.searchIntegrator = context.getUninitializedSearchIntegrator();
		this.visitor = new ElasticsearchIndexWorkVisitor(
				this.indexManager.getActualIndexName(),
				this.searchIntegrator
		);
	}

	@Override
	public void close() {
	}

	@Override
	public void applyWork(List<LuceneWork> workList, IndexingMonitor monitor) {
		// Run single action, with refresh
		if ( workList.size() == 1 ) {
			LuceneWork work = workList.iterator().next();
			BackendRequest<?> request = work.acceptIndexWorkVisitor( visitor, true );

			try ( ServiceReference<JestClient> client = searchIntegrator.getServiceManager().requestReference( JestClient.class ) ) {
				client.get().executeRequest( request );
			}

			// DBQ ignores the refresh parameter for some reason, so doing it explicitly
			if ( request.getAction() instanceof DeleteByQuery ) {
				refreshIndex();
			}
		}
		// Create bulk action
		else {
			List<BackendRequest<?>> actions = new ArrayList<>( workList.size() );

			// group actions into bulks if their type permits it; otherwise execute them right away and start a new bulk
			for ( LuceneWork luceneWork : workList ) {
				BackendRequest<?> request = luceneWork.acceptIndexWorkVisitor( visitor, false );

				// either add to bulk
				if ( request.getAction() instanceof BulkableAction ) {
					actions.add( request );
				}
				// or execute the bulk built so far and execute the non-bulkable action
				else {
					executeBulkAndClear( actions, false );

					try ( ServiceReference<JestClient> client = searchIntegrator.getServiceManager().requestReference( JestClient.class ) ) {
						client.get().executeRequest( request );
					}
				}
			}

			boolean lastActionWasBulk = executeBulkAndClear( actions, true );

			if ( !lastActionWasBulk ) {
				refreshIndex();
			}
		}
	}

	private void refreshIndex() {
		Refresh refresh = new Refresh.Builder().addIndex( indexManager.getActualIndexName() ).build();
		try ( ServiceReference<JestClient> client = searchIntegrator.getServiceManager().requestReference( JestClient.class ) ) {
			client.get().executeRequest( refresh );
		}
	}

	/**
	 * Creates a bulk action from the given list, executes it and clears the list.
	 */
	private boolean executeBulkAndClear(List<BackendRequest<?>> actions, boolean refresh) {
		if ( actions.isEmpty() ) {
			return false;
		}

		try ( ServiceReference<JestClient> client = searchIntegrator.getServiceManager().requestReference( JestClient.class ) ) {
			client.get().executeBulkRequest( actions, refresh );
		}

		actions.clear();

		return true;
	}

	@Override
	public void applyStreamWork(LuceneWork singleOperation, IndexingMonitor monitor) {
		BackendRequest<?> action = singleOperation.acceptIndexWorkVisitor( visitor, true );

		if ( action == null ) {
			return;
		}

		try ( ServiceReference<JestClient> client = searchIntegrator.getServiceManager().requestReference( JestClient.class ) ) {
			client.get().executeRequest( action );
		}
	}

	@Override
	public Lock getExclusiveWriteLock() {
		// TODO implement
		throw new UnsupportedOperationException( "Not implemented yet" );
	}

	@Override
	public void indexMappingChanged() {
		// TODO implement
		throw new UnsupportedOperationException( "Not implemented yet" );
	}

	@Override
	public void closeIndexWriter() {
		// no-op
	}
}
