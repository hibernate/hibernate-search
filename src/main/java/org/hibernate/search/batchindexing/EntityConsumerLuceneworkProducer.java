package org.hibernate.search.batchindexing;

import java.io.Serializable;
import java.util.Map;

import org.hibernate.CacheMode;
import org.hibernate.FlushMode;
import org.hibernate.Hibernate;
import org.hibernate.LockMode;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.search.backend.AddLuceneWork;
import org.hibernate.search.bridge.TwoWayFieldBridge;
import org.hibernate.search.engine.DocumentBuilderIndexedEntity;
import org.hibernate.search.engine.SearchFactoryImplementor;
import org.hibernate.search.util.LoggerFactory;
import org.slf4j.Logger;

/**
 * Component of batch-indexing pipeline, using chained producer-consumers.
 * This Runnable will consume entities taken one-by-one from the queue
 * and produce for each entity an AddLuceneWork to the output queue.
 * 
 * @author Sanne Grinovero
 */
public class EntityConsumerLuceneworkProducer implements Runnable {
	
	private static final Logger log = LoggerFactory.make();
	
	private final ProducerConsumerQueue source;
	private final ProducerConsumerQueue destination;
	private final SessionFactory sessionFactory;
	private final Map<Class<?>, DocumentBuilderIndexedEntity<?>> documentBuilders;
	private final IndexerProgressMonitor monitor;
	
	private static final int CLEAR_PERIOD = 50;
	private final CacheMode cacheMode;
	
	public EntityConsumerLuceneworkProducer(
			ProducerConsumerQueue entitySource,
			ProducerConsumerQueue fromAddworkToIndex,
			IndexerProgressMonitor monitor,
			SessionFactory sessionFactory,
			SearchFactoryImplementor searchFactory, CacheMode cacheMode) {
		this.source = entitySource;
		this.destination = fromAddworkToIndex;
		this.monitor = monitor;
		this.sessionFactory = sessionFactory;
		this.cacheMode = cacheMode;
		this.documentBuilders = searchFactory.getDocumentBuildersIndexedEntities();
	}

	public void run() {
		Session session = sessionFactory.openSession();
		session.setFlushMode( FlushMode.MANUAL );
		session.setCacheMode( cacheMode );
		try {
			Transaction transaction = session.beginTransaction();
			indexAllQueue( session );
			transaction.commit();
		}
		finally {
			session.close();
		}
		log.debug( "finished" );
	}

	private void indexAllQueue(Session session) {
		try {
			for ( int cycle=0; true; cycle++ ) {
				Object take = source.take();
				if ( take == null ) {
					break;
				}
				else {
					log.trace( "received an object {}", take );
					//trick to attach the objects to session:
					session.lock( take, LockMode.NONE );
					index( take, session );
					monitor.documentsBuilt( 1 );
					session.evict( take );
					if ( cycle == CLEAR_PERIOD ) {
						cycle = 0;
						session.clear();
					}
				}
			}
		}
		catch (InterruptedException e) {
			// just quit
		}
		finally {
			destination.producerStopping();
		}
	}

	@SuppressWarnings("unchecked")
	private void index( Object entity, Session session ) throws InterruptedException {
		Serializable id = session.getIdentifier( entity );
		Class clazz = Hibernate.getClass( entity );
		DocumentBuilderIndexedEntity docBuilder = documentBuilders.get( clazz );
		TwoWayFieldBridge idBridge = docBuilder.getIdBridge();
		String idInString = idBridge.objectToString( id );
		//depending on the complexity of the object graph going to be indexed it's possible
		//that we hit the database several times during work construction.
		AddLuceneWork addWork = docBuilder.createAddWork( clazz, entity, id, idInString, true );
		destination.put( addWork );
	}
	
}
