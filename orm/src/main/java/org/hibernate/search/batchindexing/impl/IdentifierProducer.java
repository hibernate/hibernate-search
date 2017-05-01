/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.batchindexing.impl;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.SessionFactory;
import org.hibernate.StatelessSession;
import org.hibernate.Transaction;
import org.hibernate.criterion.Projections;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.CriteriaImpl;
import org.hibernate.internal.StatelessSessionImpl;
import org.hibernate.search.batchindexing.MassIndexerProgressMonitor;
import org.hibernate.search.exception.ErrorHandler;
import org.hibernate.search.spi.IndexedTypeIdentifier;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * This Runnable is going to feed the indexing queue
 * with the identifiers of all the entities going to be indexed.
 * This step in the indexing process is not parallel (should be
 * done by one thread per type) so that a single transaction is used
 * to define the group of entities to be indexed.
 * Produced identifiers are put in the destination queue grouped in List
 * instances: the reason for this is to load them in batches
 * in the next step and reduce contention on the queue.
 *
 * @author Sanne Grinovero
 */
public class IdentifierProducer implements StatelessSessionAwareRunnable {

	private static final Log log = LoggerFactory.make();

	private final ProducerConsumerQueue<List<Serializable>> destination;
	private final SessionFactory sessionFactory;
	private final int batchSize;
	private final IndexedTypeIdentifier indexedType;
	private final MassIndexerProgressMonitor monitor;
	private final long objectsLimit;
	private final int idFetchSize;
	private final ErrorHandler errorHandler;
	private final String tenantId;

	/**
	 * @param fromIdentifierListToEntities the target queue where the produced identifiers are sent to
	 * @param sessionFactory the Hibernate SessionFactory to use to load entities
	 * @param objectLoadingBatchSize affects mostly the next consumer: IdentifierConsumerEntityProducer
	 * @param indexedType the entity type to be loaded
	 * @param monitor to monitor indexing progress
	 * @param objectsLimit if not zero
	 * @param errorHandler how to handle unexpected errors
	 * @param idFetchSize the fetch size
	 * @param tenantId the tenant identifier
	 */
	public IdentifierProducer(
			ProducerConsumerQueue<List<Serializable>> fromIdentifierListToEntities,
			SessionFactory sessionFactory,
			int objectLoadingBatchSize,
			IndexedTypeIdentifier indexedType, MassIndexerProgressMonitor monitor,
			long objectsLimit, ErrorHandler errorHandler, int idFetchSize, String tenantId) {
				this.destination = fromIdentifierListToEntities;
				this.sessionFactory = sessionFactory;
				this.batchSize = objectLoadingBatchSize;
				this.indexedType = indexedType;
				this.monitor = monitor;
				this.objectsLimit = objectsLimit;
				this.errorHandler = errorHandler;
				this.idFetchSize = idFetchSize;
				this.tenantId = tenantId;
				log.trace( "created" );
	}

	@Override
	public void run(StatelessSession upperSession) throws Exception {
		log.trace( "started" );
		try {
			inTransactionWrapper( (StatelessSessionImpl) upperSession );
		}
		catch (Exception exception) {
			errorHandler.handleException( log.massIndexerExceptionWhileFetchingIds(), exception );
		}
		finally {
			destination.producerStopping();
		}
		log.trace( "finished" );
	}

	private void inTransactionWrapper(StatelessSessionImpl upperSession) throws Exception {
		StatelessSessionImpl session = upperSession;
		if ( upperSession == null ) {
			if ( tenantId == null ) {
				session = (StatelessSessionImpl) sessionFactory.openStatelessSession();
			}
			else {
				session = (StatelessSessionImpl) sessionFactory.withStatelessOptions().tenantIdentifier( tenantId ).openStatelessSession();
			}
		}
		try {
			Transaction transaction = session.accessTransaction();
			final boolean controlTransactions = ! transaction.isActive();
			if ( controlTransactions ) {
				transaction.begin();
			}
			try {
				loadAllIdentifiers( session );
			}
			finally {
				if ( controlTransactions ) {
					transaction.commit();
				}
			}
		}
		catch (InterruptedException e) {
			// just quit
			Thread.currentThread().interrupt();
		}
		finally {
			if ( upperSession == null ) {
				session.close();
			}
		}
	}

	private void loadAllIdentifiers(final StatelessSession session) throws InterruptedException {
		Number countAsNumber = (Number) createCriteria( session )
			.setProjection( Projections.rowCount() )
			.setCacheable( false )
			.uniqueResult();
		long totalCount = countAsNumber.longValue();
		if ( objectsLimit != 0 && objectsLimit < totalCount ) {
			totalCount = objectsLimit;
		}
		if ( log.isDebugEnabled() ) {
			log.debugf( "going to fetch %d primary keys", (Long) totalCount );
		}
		monitor.addToTotalCount( totalCount );

		Criteria criteria = createCriteria( session )
			.setProjection( Projections.id() )
			.setCacheable( false )
			.setFetchSize( idFetchSize );

		ScrollableResults results = criteria.scroll( ScrollMode.FORWARD_ONLY );
		ArrayList<Serializable> destinationList = new ArrayList<Serializable>( batchSize );
		long counter = 0;
		try {
			while ( results.next() ) {
				Serializable id = (Serializable) results.get( 0 );
				destinationList.add( id );
				if ( destinationList.size() == batchSize ) {
					// Explicitly checking whether the TX is still open; Depending on the driver implementation new ids
					// might be produced otherwise if the driver fetches all rows up-front
					StatelessSessionImpl sessionImpl = (StatelessSessionImpl) session;
					if ( sessionImpl.isTransactionInProgress() == false ) {
						throw log.transactionNotActiveWhileProducingIdsForBatchIndexing( indexedType );
					}

					enqueueList( destinationList );
					destinationList = new ArrayList<Serializable>( batchSize );
				}
				counter++;
				if ( counter == totalCount ) {
					break;
				}
			}
		}
		finally {
			results.close();
		}
		enqueueList( destinationList );
	}

	private Criteria createCriteria(final StatelessSession session) {
		return new CriteriaImpl( indexedType.getName(), (SharedSessionContractImplementor) session );
	}

	private void enqueueList(final List<Serializable> idsList) throws InterruptedException {
		if ( ! idsList.isEmpty() ) {
			destination.put( idsList );
			log.tracef( "produced a list of ids %s", idsList );
		}
	}

}
