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

import org.hibernate.search.backend.FlushLuceneWork;
import org.hibernate.search.backend.IndexingMonitor;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.backend.elasticsearch.client.impl.BackendRequest;
import org.hibernate.search.backend.elasticsearch.client.impl.BackendRequestProcessor;
import org.hibernate.search.backend.spi.BackendQueueProcessor;
import org.hibernate.search.engine.integration.impl.ExtendedSearchIntegrator;
import org.hibernate.search.indexes.spi.IndexManager;
import org.hibernate.search.spi.WorkerBuildContext;

/**
 * {@link BackendQueueProcessor} applying index changes to an Elasticsearch server.
 *
 * @author Gunnar Morling
 */
public class ElasticsearchBackendQueueProcessor implements BackendQueueProcessor {

	private ElasticsearchIndexManager indexManager;
	private ExtendedSearchIntegrator searchIntegrator;
	private ElasticsearchIndexWorkVisitor visitor;
	private BackendRequestProcessor requestProcessor;

	@Override
	public void initialize(Properties props, WorkerBuildContext context, IndexManager indexManager) {
		this.indexManager = (ElasticsearchIndexManager) indexManager;
		this.searchIntegrator = context.getUninitializedSearchIntegrator();
		this.visitor = new ElasticsearchIndexWorkVisitor(
				this.indexManager.getActualIndexName(),
				this.searchIntegrator
		);
		this.requestProcessor = context.getServiceManager().requestService( BackendRequestProcessor.class );
	}

	@Override
	public void close() {
	}

	@Override
	public void applyWork(List<LuceneWork> workList, IndexingMonitor monitor) {
		// Run single action, with refresh
		if ( workList.size() == 1 ) {
			LuceneWork singleWork = workList.iterator().next();
			BackendRequest<?> request = singleWork.acceptIndexWorkVisitor( visitor, true );
			requestProcessor.executeSync( request );
		}
		// Create bulk action
		else {
			doApplyListOfWork( workList );
		}
	}

	private void doApplyListOfWork(List<LuceneWork> workList) {
		List<BackendRequest<?>> requests = new ArrayList<>( workList.size() );
		for ( LuceneWork luceneWork : workList ) {
			requests.add( luceneWork.acceptIndexWorkVisitor( visitor, false ) );
		}

		requestProcessor.executeSync( requests );
	}

	@Override
	public void applyStreamWork(LuceneWork singleOperation, IndexingMonitor monitor) {
		if ( singleOperation == FlushLuceneWork.INSTANCE ) {
			requestProcessor.awaitAsyncProcessingCompletion();
		}
		else {
			BackendRequest<?> request = singleOperation.acceptIndexWorkVisitor( visitor, true );

			if ( request != null ) {
				requestProcessor.executeAsync( request );
			}
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
	public void flushAndReleaseResources() {
		// no-op
	}
}
