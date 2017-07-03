/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.impl;

import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

import org.hibernate.search.backend.IndexingMonitor;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.backend.spi.OperationDispatcher;
import org.hibernate.search.engine.integration.impl.ExtendedSearchIntegrator;
import org.hibernate.search.engine.spi.EntityIndexBinding;
import org.hibernate.search.indexes.impl.IndexManagerHolder;
import org.hibernate.search.indexes.spi.IndexManager;
import org.hibernate.search.indexes.spi.IndexManagerSelector;
import org.hibernate.search.spi.IndexedTypeIdentifier;
import org.hibernate.search.spi.IndexedTypeMap;
import org.hibernate.search.spi.SearchIntegrator;

/**
 * A transactional dispatcher, sending works to the
 * {@link IndexManager#performOperations(List, org.hibernate.search.backend.IndexingMonitor)}
 * method of their respective index manager.
 *
 * @author Yoann Rodiere
 */
public class TransactionalOperationDispatcher implements OperationDispatcher {
	private final Function<IndexedTypeIdentifier, EntityIndexBinding> bindingLookup;
	private final IndexManagerHolder indexManagerHolder;
	private final Predicate<IndexManager> indexManagerFilter;

	public TransactionalOperationDispatcher(SearchIntegrator integrator) {
		this( integrator, indexManager -> true );
	}

	public TransactionalOperationDispatcher(SearchIntegrator integrator, Predicate<IndexManager> indexManagerFilter) {
		this( integrator.unwrap( ExtendedSearchIntegrator.class ).getIndexManagerHolder(),
				integrator::getIndexBinding, indexManagerFilter );
	}

	public TransactionalOperationDispatcher(IndexManagerHolder indexManagerHolder,
			IndexedTypeMap<EntityIndexBinding> bindings) {
		this( indexManagerHolder, bindings::get, indexManager -> true );
	}

	private TransactionalOperationDispatcher(IndexManagerHolder indexManagerHolder,
			Function<IndexedTypeIdentifier, EntityIndexBinding> bindingLookup,
			Predicate<IndexManager> indexManagerFilter) {
		this.indexManagerHolder = indexManagerHolder;
		this.bindingLookup = bindingLookup;
		this.indexManagerFilter = indexManagerFilter;
	}

	@Override
	public void dispatch(LuceneWork work, IndexingMonitor monitor) {
		WorkQueuePerIndexSplitter context = new WorkQueuePerIndexSplitter( indexManagerHolder, indexManagerFilter );
		appendWork( context, work );
		context.commitOperations( monitor );
	}

	@Override
	public void dispatch(List<LuceneWork> queue, IndexingMonitor monitor) {
		WorkQueuePerIndexSplitter context = new WorkQueuePerIndexSplitter( indexManagerHolder, indexManagerFilter );
		for ( LuceneWork work : queue ) {
			appendWork( context, work );
		}
		context.commitOperations( monitor );
	}

	private void appendWork(WorkQueuePerIndexSplitter context, LuceneWork work) {
		final IndexedTypeIdentifier entityType = work.getEntityType();
		EntityIndexBinding entityIndexBinding = bindingLookup.apply( entityType );
		IndexManagerSelector selector = entityIndexBinding.getIndexManagerSelector();
		TransactionalOperationExecutor executor = work.acceptIndexWorkVisitor( TransactionalOperationExecutorSelector.INSTANCE, null );
		executor.performOperation( work, selector, context );
	}

}
