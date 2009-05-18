package org.hibernate.search.batchindexing;

import java.io.Serializable;
import java.util.List;

import org.hibernate.CacheMode;
import org.hibernate.Criteria;
import org.hibernate.FlushMode;
import org.hibernate.LockMode;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.criterion.Restrictions;
import org.hibernate.search.util.LoggerFactory;
import org.slf4j.Logger;

/**
 * This Runnable is consuming entity identifiers and
 * producing loaded detached entities for the next queue.
 * It will finish when the queue it's consuming from will
 * signal there are no more identifiers.
 * 
 * @author Sanne Grinovero
 */
public class IdentifierConsumerEntityProducer implements Runnable {
	
	private static final Logger log = LoggerFactory.make();

	private final ProducerConsumerQueue source;
	private final ProducerConsumerQueue destination;
	private final SessionFactory sessionFactory;
	private final CacheMode cacheMode;
	private final Class<?> type;
	private final IndexerProgressMonitor monitor;

	public IdentifierConsumerEntityProducer(
			ProducerConsumerQueue fromIdentifierListToEntities,
			ProducerConsumerQueue fromEntityToAddwork,
			IndexerProgressMonitor monitor,
			SessionFactory sessionFactory,
			CacheMode cacheMode, Class<?> type) {
				this.source = fromIdentifierListToEntities;
				this.destination = fromEntityToAddwork;
				this.monitor = monitor;
				this.sessionFactory = sessionFactory;
				this.cacheMode = cacheMode;
				this.type = type;
				log.trace( "created" );
	}

	public void run() {
		log.trace( "started" );
		Session session = sessionFactory.openSession();
		session.setFlushMode( FlushMode.MANUAL );
		session.setCacheMode( cacheMode );
		try {
			Transaction transaction = session.beginTransaction();
			loadAllFromQueue( session );
			transaction.commit();
		}
		finally {
			session.close();
		}
		log.trace( "finished" );
	}
	
	private void loadAllFromQueue(Session session) {
		try {
			Object take;
			do {
				take = source.take();
				if ( take != null ) {
					List<Serializable> listIds = (List<Serializable>) take;
					log.trace( "received list of ids {}", listIds );
					loadList( listIds, session );
				}
			}
			while ( take != null );
		}
		catch (InterruptedException e) {
			// just quit
		}
		finally {
			destination.producerStopping();
		}
	}

	/**
	 * Loads a list of entities of defined type using their identifiers.
	 * The loaded objects are then pushed to the next queue one by one.
	 * @param listIds the list of entity identifiers (of type
	 * @param session the session to be used
	 * @throws InterruptedException
	 */
	private void loadList(List<Serializable> listIds, Session session) throws InterruptedException {
		//TODO investigate if I should use ObjectLoaderHelper.initializeObjects instead
		Criteria criteria = session
			.createCriteria( type )
			.setCacheMode( cacheMode )
			.setLockMode( LockMode.NONE )
			.setCacheable( false )
			.setFlushMode( FlushMode.MANUAL )
			.add( Restrictions.in( "id", listIds ) );
		List<?> list = criteria.list();
		monitor.entitiesLoaded( list.size() );
		session.clear();
		for ( Object obj : list ) {
			destination.put( obj );
		}
	}

}
