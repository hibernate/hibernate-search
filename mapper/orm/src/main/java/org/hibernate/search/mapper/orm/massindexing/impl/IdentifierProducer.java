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
import org.hibernate.search.mapper.orm.logging.impl.Log;
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

	private final SessionFactory sessionFactory;
	private final MassIndexingNotifier notifier;
	private final String tenantId;

	private final HibernateOrmMassIndexingIndexedTypeContext<E> type;
	private final SingularAttribute<? super E, I> idAttributeOfType;

	private final ProducerConsumerQueue<List<I>> destination;
	private final int batchSize;
	private final long objectsLimit;
	private final int idFetchSize;

	/**
	 * @param sessionFactory the Hibernate SessionFactory to use to load entities
	 * @param tenantId the tenant identifier
	 * @param notifier the mass indexing notifier
	 * @param fromIdentifierListToEntities the target queue where the produced identifiers are sent to
	 * @param objectLoadingBatchSize affects mostly the next consumer: IdentifierConsumerEntityProducer
	 * @param type the entity type whose identifiers are to be loaded
	 * @param idAttributeOfType the id attribute to be loaded
	 * @param objectsLimit if not zero
	 * @param idFetchSize the fetch size
	 */
	IdentifierProducer(SessionFactory sessionFactory, String tenantId,
			MassIndexingNotifier notifier,
			ProducerConsumerQueue<List<I>> fromIdentifierListToEntities,
			int objectLoadingBatchSize,
			HibernateOrmMassIndexingIndexedTypeContext<E> type, SingularAttribute<? super E, I> idAttributeOfType,
			long objectsLimit, int idFetchSize) {
		this.sessionFactory = sessionFactory;
		this.tenantId = tenantId;
		this.notifier = notifier;
		this.type = type;
		this.idAttributeOfType = idAttributeOfType;
		this.destination = fromIdentifierListToEntities;
		this.batchSize = objectLoadingBatchSize;
		this.objectsLimit = objectsLimit;
		this.idFetchSize = idFetchSize;
		log.trace( "created" );
	}

	@Override
	public void run(StatelessSession upperSession) {
		log.trace( "started" );
		try {
			inTransactionWrapper( upperSession );
		}
		catch (RuntimeException exception) {
			notifier.notifyRunnableFailure( exception, log.massIndexerFetchingIds( type.getJpaEntityName() ) );
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

	private void loadAllIdentifiers(final StatelessSession session) throws InterruptedException {
		long totalCount = createTotalCountQuery( session ).uniqueResult();
		if ( objectsLimit != 0 && objectsLimit < totalCount ) {
			totalCount = objectsLimit;
		}
		if ( log.isDebugEnabled() ) {
			log.debugf( "going to fetch %d primary keys", (Long) totalCount );
		}
		notifier.notifyAddedTotalCount( totalCount );

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
						throw log.transactionNotActiveWhileProducingIdsForBatchIndexing(
								type.getJpaEntityName()
						);
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

		Root<E> root = criteriaQuery.from( type.getEntityTypeDescriptor() );
		criteriaQuery.select( criteriaBuilder.count( root ) );

		return session.createQuery( criteriaQuery )
				.setCacheable( false );
	}

	private Query<I> createIdentifiersQuery(StatelessSession session) {
		CriteriaBuilder criteriaBuilder = sessionFactory.getCriteriaBuilder();
		CriteriaQuery<I> criteriaQuery = criteriaBuilder.createQuery( idAttributeOfType.getJavaType() );

		Root<E> root = criteriaQuery.from( type.getEntityTypeDescriptor() );
		Path<I> idPath = root.get( idAttributeOfType );
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
