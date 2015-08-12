/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.impl;

import java.util.Properties;
import java.util.concurrent.ConcurrentMap;

import org.hibernate.search.cfg.Environment;
import org.hibernate.search.exception.AssertionFailure;
import org.hibernate.search.backend.spi.Work;
import org.hibernate.search.backend.spi.WorkType;
import org.hibernate.search.backend.spi.Worker;
import org.hibernate.search.engine.integration.impl.ExtendedSearchIntegrator;
import org.hibernate.search.engine.spi.EntityIndexBinding;
import org.hibernate.search.indexes.interceptor.EntityIndexingInterceptor;
import org.hibernate.search.indexes.interceptor.IndexingOverride;
import org.hibernate.search.util.configuration.impl.ConfigurationParseHelper;
import org.hibernate.search.util.impl.Maps;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.backend.TransactionContext;
import org.hibernate.search.spi.InstanceInitializer;
import org.hibernate.search.spi.WorkerBuildContext;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * Queue works per transaction.
 * If out of transaction, the work is executed right away
 * <p>
 * When <code>hibernate.search.worker.type</code> is set to <code>async</code>
 * the work is done in a separate thread (threads are pooled)
 *
 * @author Emmanuel Bernard
 */
public class PerTransactionWorker implements Worker {

	//note: there is only one Worker instance, reused concurrently for all sessions.

	private static final Log log = LoggerFactory.make();

	//this is being used from different threads, but doesn't need a
	//synchronized map since for a given transaction, we have not concurrent access
	protected final ConcurrentMap<Object, WorkQueueSynchronization> synchronizationPerTransaction = Maps.createIdentityWeakKeyConcurrentMap( 64, 32 );
	private QueueingProcessor queueingProcessor;
	private ExtendedSearchIntegrator factory;
	private InstanceInitializer instanceInitializer;

	private boolean transactionExpected;
	private boolean enlistInTransaction;

	@Override
	public void performWork(Work work, TransactionContext transactionContext) {
		final Class<?> entityType = instanceInitializer.getClassFromWork( work );
		EntityIndexBinding indexBindingForEntity = factory.getIndexBinding( entityType );
		if ( indexBindingForEntity == null
				&& factory.getDocumentBuilderContainedEntity( entityType ) == null ) {
			throw new SearchException( "Unable to perform work. Entity Class is not @Indexed nor hosts @ContainedIn: " + entityType );
		}
		work = interceptWork( indexBindingForEntity, work );
		if ( work == null ) {
			//nothing to do
			return;
		}
		if ( transactionContext.isTransactionInProgress() ) {
			final Object transactionIdentifier = transactionContext.getTransactionIdentifier();
			WorkQueueSynchronization txSync = synchronizationPerTransaction.get( transactionIdentifier );
			if ( txSync == null || txSync.isConsumed() ) {
				txSync = createTransactionWorkQueueSynchronization( transactionIdentifier );
				transactionContext.registerSynchronization( txSync );
				synchronizationPerTransaction.put( transactionIdentifier, txSync );
			}
			txSync.add( work );
		}
		else {
			if ( transactionExpected ) {
				// this is a workaround: isTransactionInProgress should return "true"
				// for correct configurations.
				log.pushedChangesOutOfTransaction();
			}
			WorkQueue queue = new WorkQueue( factory );
			queueingProcessor.add( work, queue );
			queueingProcessor.prepareWorks( queue );
			queueingProcessor.performWorks( queue );
		}
	}

	private WorkQueueSynchronization createTransactionWorkQueueSynchronization(Object transactionIdentifier) {
		if ( enlistInTransaction ) {
			return new InTransactionWorkQueueSynchronization(
					transactionIdentifier,
					queueingProcessor, synchronizationPerTransaction, factory
			);
		}
		else {
			return new PostTransactionWorkQueueSynchronization(
					transactionIdentifier,
					queueingProcessor, synchronizationPerTransaction, factory
			);
		}
	}

	private Work interceptWork(EntityIndexBinding indexBindingForEntity, Work work) {
		if ( indexBindingForEntity == null ) {
			return work;
		}
		EntityIndexingInterceptor interceptor = indexBindingForEntity.getEntityIndexingInterceptor();
		if ( interceptor == null ) {
			return work;
		}
		IndexingOverride operation;
		switch ( work.getType() ) {
			case ADD:
				operation = interceptor.onAdd( work.getEntity() );
				break;
			case UPDATE:
				operation = interceptor.onUpdate( work.getEntity() );
				break;
			case DELETE:
				operation = interceptor.onDelete( work.getEntity() );
				break;
			case COLLECTION:
				operation = interceptor.onCollectionUpdate( work.getEntity() );
				break;
			case PURGE:
			case PURGE_ALL:
			case INDEX:
			case DELETE_BY_QUERY:
				operation = IndexingOverride.APPLY_DEFAULT;
				break;
			default:
				throw new AssertionFailure( "Unknown work type: " + work.getType() );
		}
		Work result = work;
		Class<?> entityClass = work.getEntityClass();
		switch ( operation ) {
			case APPLY_DEFAULT:
				break;
			case SKIP:
				result = null;
				log.forceSkipIndexOperationViaInterception( entityClass, work.getType() );
				break;
			case UPDATE:
				result = new Work( work.getTenantIdentifier(), work.getEntity(), work.getId(), WorkType.UPDATE );
				log.forceUpdateOnIndexOperationViaInterception( entityClass, work.getType() );
				break;
			case REMOVE:
				//This works because other Work constructors are never used from WorkType ADD, UPDATE, REMOVE, COLLECTION
				//TODO should we force isIdentifierRollback to false if the operation is not a delete?
				result = new Work( work.getTenantIdentifier(), work.getEntity(), work.getId(), WorkType.DELETE, work.isIdentifierWasRolledBack() );
				log.forceRemoveOnIndexOperationViaInterception( entityClass, work.getType() );
				break;
			default:
				throw new AssertionFailure( "Unknown action type: " + operation );
		}
		return result;
	}

	@Override
	public void initialize(Properties props, WorkerBuildContext context, QueueingProcessor queueingProcessor) {
		this.queueingProcessor = queueingProcessor;
		this.factory = context.getUninitializedSearchIntegrator();
		this.transactionExpected = context.isTransactionManagerExpected();
		this.instanceInitializer = context.getInstanceInitializer();
		this.enlistInTransaction = ConfigurationParseHelper.getBooleanValue(
				props,
				Environment.WORKER_ENLIST_IN_TRANSACTION,
				false
		);
	}

	@Override
	public void close() {
	}

	@Override
	public void flushWorks(TransactionContext transactionContext) {
		if ( transactionContext.isTransactionInProgress() ) {
			Object transaction = transactionContext.getTransactionIdentifier();
			WorkQueueSynchronization txSync = synchronizationPerTransaction.get( transaction );
			if ( txSync != null && !txSync.isConsumed() ) {
				txSync.flushWorks();
			}
		}
	}
}
