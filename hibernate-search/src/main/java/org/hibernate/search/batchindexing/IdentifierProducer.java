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
package org.hibernate.search.batchindexing;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.ScrollableResults;
import org.hibernate.SessionFactory;
import org.hibernate.StatelessSession;
import org.hibernate.Transaction;
import org.hibernate.search.SearchException;
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
public class IdentifierProducer implements StatelessSessionAwareRunnable {
	
	private static final Logger log = LoggerFactory.make();

	private final ProducerConsumerQueue<List<Serializable>> destination;
	private final SessionFactory sessionFactory;
	private final int batchSize;
	private final MassIndexerProgressMonitor monitor;
	private final long objectsLimit;
	private final IdentifierLoadingStrategy loaderStrategy;
	
	/**
	 * @param fromIdentifierListToEntities
	 *            the target queue where the produced identifiers are sent to
	 * @param sessionFactory
	 *            the Hibernate SessionFactory to use to load entities
	 * @param objectLoadingBatchSize
	 *            affects mostly the next consumer: IdentifierConsumerEntityProducer
	 * @param indexedType
	 *            the entity type to be loaded
	 * @param monitor
	 *            to monitor indexing progress
	 * @param objectsLimit
	 *            if not zero
	 * @param countHQL
	 *            HQL query which performs the count of to be indexed entities. must return a single Number. when null,
	 *            an appropriate Criteria is generated.
	 * @param idLoadingHQL
	 *            HQL query which selects all primary keys to be indexed for this entity type. when null, an Criteria
	 *            scrolling on all primary keys is generated.
	 * @param customQueriesParameters
	 *            must contain the named parameters of queries countHQL and idLoadingHQL.
	 */
	public IdentifierProducer(
			ProducerConsumerQueue<List<Serializable>> fromIdentifierListToEntities,
			SessionFactory sessionFactory,
			int objectLoadingBatchSize,
			Class<?> indexedType, MassIndexerProgressMonitor monitor,
			long objectsLimit,
			IdentifierLoadingStrategy loaderStrategy) {
				this.destination = fromIdentifierListToEntities;
				this.sessionFactory = sessionFactory;
				this.batchSize = objectLoadingBatchSize;
				this.monitor = monitor;
				this.objectsLimit = objectsLimit;
				this.loaderStrategy = loaderStrategy == null ? new CriteriaLoadingStrategy( indexedType ) : loaderStrategy;
				log.trace( "created" );
	}
	
	public void run(StatelessSession upperSession) {
		log.trace( "started" );
		try {
			inTransactionWrapper(upperSession);
		}
		catch (Throwable e) {
			log.error( "error during batch indexing: ", e );
		}
		finally{
			destination.producerStopping();
		}
		log.trace( "finished" );
	}

	private void inTransactionWrapper(StatelessSession upperSession) throws Exception {
		StatelessSession session = upperSession;
		if (upperSession == null) {
			session = sessionFactory.openStatelessSession();
		}
		try {
			Transaction transaction = Helper.getTransactionAndMarkForJoin( session );
			transaction.begin();
			loadAllIdentifiers( session );
			transaction.commit();
		} catch (InterruptedException e) {
			// just quit
		}
		finally {
			if (upperSession == null) {
				session.close();
			}
		}
	}

	private void loadAllIdentifiers(final StatelessSession session) throws InterruptedException {
		Number indexedEntitiesCount = loaderStrategy.countToBeIndexedEntities( session );
		if ( indexedEntitiesCount == null ) {
			throw new SearchException( "Selected loading strategy returned 'null' on method countToBeIndexedEntities(StatelessSession)" );
		}
		long totalCount = indexedEntitiesCount.longValue();
		if ( objectsLimit != 0 && objectsLimit < totalCount ) {
			totalCount = objectsLimit;
		}
		log.debug( "going to fetch {} primary keys", totalCount);
		monitor.addToTotalCount( totalCount );
		
		ScrollableResults results = loaderStrategy.getToIndexIdentifiersScrollable( session );
		ArrayList<Serializable> destinationList = new ArrayList<Serializable>( batchSize );
		long counter = 0;
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
					break; // early exit if an objectsLimit was defined
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
