/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.impl;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.hibernate.search.backend.IndexingMonitor;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.backend.spi.OperationDispatcher;
import org.hibernate.search.engine.integration.impl.ExtendedSearchIntegrator;
import org.hibernate.search.engine.spi.EntityIndexBinding;
import org.hibernate.search.indexes.impl.IndexManagerHolder;
import org.hibernate.search.indexes.spi.IndexManager;
import org.hibernate.search.spi.SearchIntegrator;
import org.hibernate.search.store.IndexShardingStrategy;

/**
 * A transactional dispatcher, sending works to the
 * {@link IndexManager#performOperations(List, org.hibernate.search.backend.IndexingMonitor)}
 * method of their respective index manager.
 *
 * @author Yoann Rodiere
 */
public class TransactionalOperationDispatcher implements OperationDispatcher {
	private final Function<Class<?>, EntityIndexBinding> bindingLookup;
	private final TransactionalOperationExecutorSelector executorSelector;

	public TransactionalOperationDispatcher(SearchIntegrator integrator) {
		this( integrator.unwrap( ExtendedSearchIntegrator.class ).getIndexManagerHolder(),
				integrator::getIndexBinding );
	}

	public TransactionalOperationDispatcher(IndexManagerHolder indexManagerHolder,
			Map<Class<?>, EntityIndexBinding> bindings) {
		this( indexManagerHolder, bindings::get );
	}

	private TransactionalOperationDispatcher(IndexManagerHolder indexManagerHolder,
			Function<Class<?>, EntityIndexBinding> bindingLookup) {
		this.executorSelector = new TransactionalOperationExecutorSelector( indexManagerHolder );
		this.bindingLookup = bindingLookup;
	}

	@Override
	public void dispatch(LuceneWork work, IndexingMonitor monitor) {
		WorkQueuePerIndexSplitter context = new WorkQueuePerIndexSplitter();
		appendWork( context, work );
		context.commitOperations( monitor );
	}

	@Override
	public void dispatch(List<LuceneWork> queue, IndexingMonitor monitor) {
		WorkQueuePerIndexSplitter context = new WorkQueuePerIndexSplitter();
		for ( LuceneWork work : queue ) {
			appendWork( context, work );
		}
		context.commitOperations( monitor );
	}

	private void appendWork(WorkQueuePerIndexSplitter context, LuceneWork work) {
		final Class<?> entityType = work.getEntityClass();
		EntityIndexBinding entityIndexBinding = bindingLookup.apply( entityType );
		IndexShardingStrategy shardingStrategy = entityIndexBinding.getSelectionStrategy();
		TransactionalOperationExecutor executor = work.acceptIndexWorkVisitor( executorSelector, null );
		executor.performOperation( work, shardingStrategy, context );
	}

}
