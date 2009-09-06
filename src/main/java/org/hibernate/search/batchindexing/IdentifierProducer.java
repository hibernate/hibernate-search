package org.hibernate.search.batchindexing;

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
import org.hibernate.search.util.LoggerFactory;
import org.slf4j.Logger;

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
public class IdentifierProducer implements Runnable {
	
	private static final Logger log = LoggerFactory.make();

	private final ProducerConsumerQueue<List<Serializable>> destination;
	private final SessionFactory sessionFactory;
	private final int batchSize;
	private final Class<?> indexedType;
	private final MassIndexerProgressMonitor monitor;

	private final int objectsLimit;

	/**
	 * @param fromIdentifierListToEntities the target queue where the produced identifiers are sent to
	 * @param sessionFactory the Hibernate SessionFactory to use to load entities
	 * @param objectLoadingBatchSize affects mostly the next consumer: IdentifierConsumerEntityProducer
	 * @param indexedType the entity type to be loaded
	 * @param monitor to monitor indexing progress
	 * @param objectsLimit if not zero
	 */
	public IdentifierProducer(
			ProducerConsumerQueue<List<Serializable>> fromIdentifierListToEntities,
			SessionFactory sessionFactory,
			int objectLoadingBatchSize,
			Class<?> indexedType, MassIndexerProgressMonitor monitor,
			int objectsLimit) {
				this.destination = fromIdentifierListToEntities;
				this.sessionFactory = sessionFactory;
				this.batchSize = objectLoadingBatchSize;
				this.indexedType = indexedType;
				this.monitor = monitor;
				this.objectsLimit = objectsLimit;
				log.trace( "created" );
	}
	
	public void run() {
		log.trace( "started" );
		try {
			inTransactionWrapper();
		}
		finally{
			destination.producerStopping();
		}
		log.trace( "finished" );
	}

	private void inTransactionWrapper() {
		StatelessSession session = sessionFactory.openStatelessSession();
		try {
			Transaction transaction = session.beginTransaction();
			loadAllIdentifiers( session );
			transaction.commit();
		} catch (InterruptedException e) {
			// just quit
		}
		finally {
			session.close();
		}
	}

	private void loadAllIdentifiers(final StatelessSession session) throws InterruptedException {
		Integer totalCount = (Integer) session
			.createCriteria( indexedType )
			.setProjection( Projections.count( "id" ) )
			.setCacheable( false )
			.uniqueResult();
		
		if ( objectsLimit != 0 && objectsLimit < totalCount.intValue() ) {
			totalCount = objectsLimit;
		}
		log.debug( "going to fetch {} primary keys", totalCount);
		monitor.addToTotalCount( totalCount );
		
		Criteria criteria = session
			.createCriteria( indexedType )
			.setProjection( Projections.id() )
			.setCacheable( false )
			.setFetchSize( 100 );
		
		ScrollableResults results = criteria.scroll( ScrollMode.FORWARD_ONLY );
		ArrayList<Serializable> destinationList = new ArrayList<Serializable>( batchSize );
		int counter = 0;
		try {
			while ( results.next() ) {
				Serializable id = (Serializable) results.get( 0 );
				destinationList.add( id );
				if ( destinationList.size() == batchSize ) {
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
	
	private void enqueueList(final List<Serializable> idsList) throws InterruptedException {
		if ( ! idsList.isEmpty() ) {
			destination.put( idsList );
			log.trace( "produced a list of ids {}", idsList );
		}
	}

}
