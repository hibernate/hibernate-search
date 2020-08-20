/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.impl;

import java.util.List;

import org.hibernate.search.backend.IndexingMonitor;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.backend.spi.OperationDispatcher;
import org.hibernate.search.engine.spi.EntityIndexBinding;
import org.hibernate.search.indexes.spi.IndexManager;
import org.hibernate.search.indexes.spi.IndexManagerSelector;
import org.hibernate.search.spi.IndexedTypeIdentifier;
import org.hibernate.search.spi.SearchIntegrator;

/**
 * A streaming dispatcher, sending works to the
 * {@link IndexManager#performStreamOperation(LuceneWork, IndexingMonitor, boolean)}
 * method of their respective index manager.
 *
 * @author Yoann Rodiere
 */
public class StreamingOperationDispatcher implements OperationDispatcher {

	private final boolean forceAsync;
	private final SearchIntegrator integrator;

	public StreamingOperationDispatcher(SearchIntegrator integrator, boolean forceAsync) {
		this.integrator = integrator;
		this.forceAsync = forceAsync;
	}

	@Override
	public void dispatch(LuceneWork work, IndexingMonitor monitor) {
		executeWork( work, monitor );
	}

	@Override
	public void dispatch(List<LuceneWork> queue, IndexingMonitor monitor) {
		for ( LuceneWork work : queue ) {
			executeWork( work, monitor );
		}
	}

	private void executeWork(LuceneWork work, IndexingMonitor progressMonitor) {
		final IndexedTypeIdentifier entityType = work.getEntityType();
		EntityIndexBinding entityIndexBinding = integrator.getIndexBinding( entityType );
		IndexManagerSelector selector = entityIndexBinding.getIndexManagerSelector();
		StreamingOperationExecutor executor =
				work.acceptIndexWorkVisitor( StreamingOperationExecutorSelector.INSTANCE, null );
		executor.performStreamOperation( work, selector, progressMonitor, forceAsync );
	}

}
