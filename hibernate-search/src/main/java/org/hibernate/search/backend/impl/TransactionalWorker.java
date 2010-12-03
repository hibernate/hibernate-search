/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat, Inc. and/or its affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.search.backend.impl;

import java.util.Properties;
import javax.transaction.Synchronization;

import org.slf4j.Logger;

import org.hibernate.search.SearchException;
import org.hibernate.search.backend.QueueingProcessor;
import org.hibernate.search.backend.TransactionContext;
import org.hibernate.search.backend.Work;
import org.hibernate.search.backend.WorkQueue;
import org.hibernate.search.backend.Worker;
import org.hibernate.search.engine.SearchFactoryImplementor;
import org.hibernate.search.spi.WorkerBuildContext;
import org.hibernate.search.util.HibernateHelper;
import org.hibernate.search.util.LoggerFactory;
import org.hibernate.search.util.WeakIdentityHashMap;

/**
 * Queue works per transaction.
 * If out of transaction, the work is executed right away
 * <p/>
 * When <code>hibernate.search.worker.type</code> is set to <code>async</code>
 * the work is done in a separate thread (threads are pooled)
 *
 * @author Emmanuel Bernard
 */
public class TransactionalWorker implements Worker {

	//note: there is one Worker instance per SearchFactory, reused concurrently for all sessions.

	private static final Logger log = LoggerFactory.make();

	//this is being used from different threads, but doesn't need a
	//synchronized map since for a given transaction, we have not concurrent access
	protected final WeakIdentityHashMap<Object, Synchronization> synchronizationPerTransaction = new WeakIdentityHashMap<Object, Synchronization>();
	private QueueingProcessor queueingProcessor;
	private SearchFactoryImplementor factory;

	public void performWork(Work<?> work, TransactionContext transactionContext) {
		final Class<?> entityType = HibernateHelper.getClassFromWork( work );
		if ( factory.getDocumentBuilderIndexedEntity( entityType ) == null
				&& factory.getDocumentBuilderContainedEntity( entityType ) == null ) {
			throw new SearchException( "Unable to perform work. Entity Class is not @Indexed nor hosts @ContainedIn: " + entityType );
		}
		if ( transactionContext.isTransactionInProgress() ) {
			Object transactionIdentifier = transactionContext.getTransactionIdentifier();
			PostTransactionWorkQueueSynchronization txSync = (PostTransactionWorkQueueSynchronization)
					synchronizationPerTransaction.get( transactionIdentifier );
			if ( txSync == null || txSync.isConsumed() ) {
				txSync = new PostTransactionWorkQueueSynchronization(
						queueingProcessor, synchronizationPerTransaction, factory
				);
				transactionContext.registerSynchronization( txSync );
				synchronizationPerTransaction.put( transactionIdentifier, txSync );
			}
			txSync.add( work );
		}
		else {
			// this is a workaround: isTransactionInProgress should return "true"
			// for correct configurations.
			log.warn(
					"It appears changes are being pushed to the index out of a transaction. " +
							"Register the IndexWorkFlushEventListener listener on flush to correctly manage Collections!"
			);
			WorkQueue queue = new WorkQueue( factory );
			queueingProcessor.add( work, queue );
			queueingProcessor.prepareWorks( queue );
			queueingProcessor.performWorks( queue );
		}
	}

	public void initialize(Properties props, WorkerBuildContext context) {
		this.queueingProcessor = new BatchedQueueingProcessor( context, props );
		this.factory = context.getUninitializedSearchFactory();
	}

	public void close() {
		queueingProcessor.close();
	}

	public void flushWorks(TransactionContext transactionContext) {
		if ( transactionContext.isTransactionInProgress() ) {
			Object transaction = transactionContext.getTransactionIdentifier();
			PostTransactionWorkQueueSynchronization txSync = (PostTransactionWorkQueueSynchronization)
					synchronizationPerTransaction.get( transaction );
			if ( txSync != null && !txSync.isConsumed() ) {
				txSync.flushWorks();
			}
		}
	}
}
