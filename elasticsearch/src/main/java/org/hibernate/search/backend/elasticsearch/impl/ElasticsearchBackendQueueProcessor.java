/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.impl;

import java.util.List;
import java.util.Properties;
import java.util.concurrent.locks.Lock;

import org.hibernate.search.backend.IndexingMonitor;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.backend.spi.BackendQueueProcessor;
import org.hibernate.search.indexes.spi.IndexManager;
import org.hibernate.search.spi.WorkerBuildContext;

/**
 * {@link BackendQueueProcessor} applying index changes to an Elasticsearch server.
 *
 * @author Gunnar Morling
 */
public class ElasticsearchBackendQueueProcessor implements BackendQueueProcessor {

	private ElasticsearchIndexManager indexManager;

	@Override
	public void initialize(Properties props, WorkerBuildContext context, IndexManager indexManager) {
		this.indexManager = (ElasticsearchIndexManager) indexManager;
	}

	@Override
	public void close() {
	}

	@Override
	public void applyWork(List<LuceneWork> workList, IndexingMonitor monitor) {
		for ( LuceneWork luceneWork : workList ) {
			luceneWork.acceptIndexWorkVisitor(
					new ElasticsearchIndexWorkVisitor(
							indexManager.getActualIndexName(),
							indexManager.searchIntegrator
					),
					null
			);
		}
	}

	@Override
	public void applyStreamWork(LuceneWork singleOperation, IndexingMonitor monitor) {
		singleOperation.acceptIndexWorkVisitor(
				new ElasticsearchIndexWorkVisitor(
						indexManager.getActualIndexName(),
						indexManager.searchIntegrator
				),
				null
		);
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
