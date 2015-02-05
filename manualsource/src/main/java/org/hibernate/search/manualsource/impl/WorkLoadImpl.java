/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.manualsource.impl;

import java.io.Serializable;
import java.util.Set;
import javax.transaction.Status;
import javax.transaction.Synchronization;

import org.apache.lucene.search.Query;

import org.hibernate.search.backend.spi.Work;
import org.hibernate.search.backend.spi.WorkType;
import org.hibernate.search.engine.integration.impl.ExtendedSearchIntegrator;
import org.hibernate.search.exception.AssertionFailure;
import org.hibernate.search.manualsource.SearchFactory;
import org.hibernate.search.manualsource.WorkLoad;
import org.hibernate.search.manualsource.backend.impl.BatchTransactionContext;

/**
 * This class represents an indexing work load with the ability to batch via the
 * start and stop methods.
 * Not thread-safe.
 *
 * @author Emmanuel Bernard &lt;emmanuel@hibernate.org&gt;
 */
public class WorkLoadImpl implements WorkLoad {
	private Synchronization synchronization;
	private BatchTransactionContext transactionContext;
	private boolean transactionInProgress = false;
	private WorkLoadManagerImpl workLoadManager;

	public WorkLoadImpl(WorkLoadManagerImpl workLoadManager) {
		this.transactionContext = new BatchTransactionContext( this );
		this.workLoadManager = workLoadManager;
	}


	// Batch boundaries methods
	public void startBatch() {
		if ( transactionInProgress ) {
			throw new IllegalStateException( "Work load batching already started" );
		}
		this.transactionInProgress = true;
	}

	public void commitBatch() {
		if ( !transactionInProgress ) {
			throw new IllegalStateException( "No work load batching started" );
		}
		synchronization.beforeCompletion();
		synchronization.afterCompletion( Status.STATUS_COMMITTED );
		transactionInProgress = false;
		synchronization = null; //TODO not sure if the sync should be cleared
	}

	public void endBatch() {
		if ( !transactionInProgress ) {
			throw new IllegalStateException( "No work load batching started" );
		}
		synchronization.afterCompletion( Status.STATUS_ROLLEDBACK );
		transactionInProgress = false;
		synchronization = null; //TODO not sure if the sync should be cleared
	}

	@Override
	public Object createFullTextQuery(Query luceneQuery, Class<?>... entities) {
		return null;
	}

	@Override
	public <T> void index(T entity) {
		if ( entity == null ) {
			throw new IllegalArgumentException( "Entity to index should not be null" );
		}

		ExtendedSearchIntegrator searchIntegrator = workLoadManager.getSearchIntegrator();
		Class<?> clazz = searchIntegrator.getInstanceInitializer().getClass( entity );
		//not strictly necessary but a small optimization
		if ( searchIntegrator.getIndexBinding( clazz ) == null ) {
			String msg = "Entity to index is not an @Indexed entity: " + entity.getClass().getName();
			throw new IllegalArgumentException( msg );
		}
		Serializable id = workLoadManager.getIdExtractor().getId( entity );
		Work work = new Work( entity, id, WorkType.INDEX );
		searchIntegrator.getWorker().performWork( work, transactionContext );
	}

	@Override
	public SearchFactory getSearchFactory() {
		// if necessary cache this instance in WorkLoadManagerImpl
		return new SearchFactoryImpl( workLoadManager.getSearchIntegrator() );
	}

	@Override
	public <T> void purge(Class<T> entityType, Serializable id) {
		if ( entityType == null ) {
			return;
		}

		Set<Class<?>> targetedClasses = workLoadManager.getSearchIntegrator().getIndexedTypesPolymorphic(
				new Class[] {
						entityType
				}
		);
		if ( targetedClasses.isEmpty() ) {
			String msg = entityType.getName() + " is not an indexed entity or a subclass of an indexed entity";
			throw new IllegalArgumentException( msg );
		}

		for ( Class<?> clazz : targetedClasses ) {
			if ( id == null ) {
				createAndPerformWork( clazz, null, WorkType.PURGE_ALL );
			}
			else {
				createAndPerformWork( clazz, id, WorkType.PURGE );
			}
		}
	}

	private void createAndPerformWork(Class<?> clazz, Serializable id, WorkType workType) {
		Work work = new Work( clazz, id, workType );
		workLoadManager.getSearchIntegrator().getWorker().performWork( work, transactionContext );
	}

	@Override
	public <T> void purgeAll(Class<T> entityType) {
		purge( entityType, null );
	}

	// internal methods
	public void registerSynchronization(Synchronization synchronization) {
		if ( this.synchronization != null ) {
			throw new AssertionFailure( "Should we receive several synchronization?" );
		}
		this.synchronization = synchronization;
	}

	public boolean isTransactionInProgress() {
		return transactionInProgress;
	}
}
