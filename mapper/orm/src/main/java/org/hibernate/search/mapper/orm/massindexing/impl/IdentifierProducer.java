/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.massindexing.impl;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Root;
import javax.persistence.metamodel.SingularAttribute;

import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.SessionFactory;
import org.hibernate.StatelessSession;
import org.hibernate.Transaction;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.query.Query;
import org.hibernate.search.engine.reporting.FailureHandler;
import org.hibernate.search.mapper.orm.logging.impl.Log;
import org.hibernate.search.mapper.orm.massindexing.monitor.MassIndexingMonitor;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

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
 * @param <E> The entity type
 * @param <I> The identifier type
 *
 * @author Sanne Grinovero
 */
public class IdentifierProducer<E, I> implements StatelessSessionAwareRunnable {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final ProducerConsumerQueue<List<I>> destination;
	private final SessionFactory sessionFactory;
	private final int batchSize;
	private final Class<E> indexedType;
	private final SingularAttribute<? super E, I> idAttributeOfIndexedType;
	private final MassIndexingMonitor monitor;
	private final FailureHandler failureHandler;
	private final long objectsLimit;
	private final int idFetchSize;
	private final String tenantId;

	/**
	 * @param fromIdentifierListToEntities the target queue where the produced identifiers are sent to
	 * @param sessionFactory the Hibernate SessionFactory to use to load entities
	 * @param objectLoadingBatchSize affects mostly the next consumer: IdentifierConsumerEntityProducer
	 * @param indexedType the entity type whose identifiers are to be loaded
	 * @param idAttributeOfIndexedType the id attribute to be loaded
	 * @param monitor the indexing monitor
	 * @param objectsLimit if not zero
	 * @param idFetchSize the fetch size
	 * @param tenantId the tenant identifier
	 */
	public IdentifierProducer(
			ProducerConsumerQueue<List<I>> fromIdentifierListToEntities, SessionFactory sessionFactory,
			int objectLoadingBatchSize,
			Class<E> indexedType, SingularAttribute<? super E, I> idAttributeOfIndexedType,
			MassIndexingMonitor monitor, FailureHandler failureHandler,
			long objectsLimit, int idFetchSize, String tenantId) {
		this.destination = fromIdentifierListToEntities;
		this.sessionFactory = sessionFactory;
		this.batchSize = objectLoadingBatchSize;
		this.indexedType = indexedType;
		this.idAttributeOfIndexedType = idAttributeOfIndexedType;
		this.monitor = monitor;
		this.failureHandler = failureHandler;
		this.objectsLimit = objectsLimit;
		this.idFetchSize = idFetchSize;
		this.tenantId = tenantId;
		log.trace( "created" );
	}

	@Override
	public void run(StatelessSession upperSession) {
		log.trace( "started" );
		try {
			inTransactionWrapper( upperSession );
		}
		catch (Exception exception) {
			String errorMessage = log.massIndexerExceptionWhileFetchingIds();

			failureHandler.handleException( errorMessage, exception );
		}
		finally {
			destination.producerStopping();
		}
		log.trace( "finished" );
	}

	private void inTransactionWrapper(StatelessSession upperSession) {
		StatelessSession session = upperSession;
		if ( upperSession == null ) {
			if ( tenantId == null ) {
				session = sessionFactory.openStatelessSession();
			}
			else {
				session = sessionFactory.withStatelessOptions().tenantIdentifier( tenantId ).openStatelessSession();
			}
		}
		try {
			Transaction transaction = ( (SharedSessionContractImplementor) session ).accessTransaction();
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

	// Criteria on a stateless session will be un-deprecated soon.
	// See https://hibernate.atlassian.net/browse/HHH-13154.
	@SuppressWarnings("deprecation")
	private void loadAllIdentifiers(final StatelessSession session) throws InterruptedException {
		long totalCount = createTotalCountQuery( session ).uniqueResult();
		if ( objectsLimit != 0 && objectsLimit < totalCount ) {
			totalCount = objectsLimit;
		}
		if ( log.isDebugEnabled() ) {
			log.debugf( "going to fetch %d primary keys", (Long) totalCount );
		}
		monitor.addToTotalCount( totalCount );

		ArrayList<I> destinationList = new ArrayList<>( batchSize );
		long counter = 0;
		try ( ScrollableResults results = createIdentifiersQuery( session ).scroll( ScrollMode.FORWARD_ONLY ) ) {
			while ( results.next() ) {
				@SuppressWarnings("unchecked")
				I id = (I) results.get( 0 );
				destinationList.add( id );
				if ( destinationList.size() == batchSize ) {
					// Explicitly checking whether the TX is still open; Depending on the driver implementation new ids
					// might be produced otherwise if the driver fetches all rows up-front
					SharedSessionContractImplementor sessionImpl = (SharedSessionContractImplementor) session;
					if ( !sessionImpl.isTransactionInProgress() ) {
						throw log.transactionNotActiveWhileProducingIdsForBatchIndexing( indexedType );
					}

					enqueueList( destinationList );
					destinationList = new ArrayList<>( batchSize );
				}
				counter++;
				if ( counter == totalCount ) {
					break;
				}
			}
		}
		enqueueList( destinationList );
	}

	private Query<Long> createTotalCountQuery(StatelessSession session) {
		CriteriaBuilder criteriaBuilder = sessionFactory.getCriteriaBuilder();
		CriteriaQuery<Long> criteriaQuery = criteriaBuilder.createQuery( Long.class );

		Root<E> root = criteriaQuery.from( indexedType );
		criteriaQuery.select( criteriaBuilder.count( root ) );

		return session.createQuery( criteriaQuery )
				.setCacheable( false );
	}

	private Query<I> createIdentifiersQuery(StatelessSession session) {
		CriteriaBuilder criteriaBuilder = sessionFactory.getCriteriaBuilder();
		CriteriaQuery<I> criteriaQuery = criteriaBuilder.createQuery( idAttributeOfIndexedType.getJavaType() );

		Root<E> root = criteriaQuery.from( indexedType );
		Path<I> idPath = root.get( idAttributeOfIndexedType );
		criteriaQuery.select( idPath );

		return session.createQuery( criteriaQuery )
				.setCacheable( false )
				.setFetchSize( idFetchSize );
	}

	private void enqueueList(final List<I> idsList) throws InterruptedException {
		if ( ! idsList.isEmpty() ) {
			destination.put( idsList );
			log.tracef( "produced a list of ids %s", idsList );
		}
	}

}
