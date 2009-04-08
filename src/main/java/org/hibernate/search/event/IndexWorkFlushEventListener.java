// $Id$
package org.hibernate.search.event;

import java.io.Serializable;
import java.util.concurrent.ConcurrentHashMap;

import javax.transaction.Status;
import javax.transaction.Synchronization;

import org.hibernate.AssertionFailure;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.event.EventSource;
import org.hibernate.event.FlushEvent;
import org.hibernate.event.FlushEventListener;
import org.hibernate.search.util.LoggerFactory;
import org.slf4j.Logger;

/**
 * FlushEventListener to make sure the indexes are updated right after the hibernate flush,
 * avoiding object loading during a flush. Not needed during transactions.
 * 
 * @author Sanne Grinovero
 */
public final class IndexWorkFlushEventListener implements FlushEventListener, Serializable {
	
	private static final Logger log = LoggerFactory.make();
	
	private final ConcurrentHashMap<Session, Synchronization> synchronizationPerTransaction
		= new ConcurrentHashMap<Session, Synchronization>();
	
	public IndexWorkFlushEventListener() {
	}

	public void onFlush(FlushEvent event) throws HibernateException {
		Session session = event.getSession();
		Synchronization synchronization = synchronizationPerTransaction.get( session );
		if ( synchronization != null ) {
			log.debug( "flush event causing index update out of transaction" );
			synchronizationPerTransaction.remove( session );
			synchronization.beforeCompletion();
			synchronization.afterCompletion( Status.STATUS_COMMITTED );
		}
	}

	public void addSynchronization(EventSource eventSource, Synchronization synchronization) {
		Synchronization previousSync = synchronizationPerTransaction.put( eventSource, synchronization );
		if ( previousSync != null ) {
			throw new AssertionFailure( "previous registered sync not discarded in IndexWorkFlushEventListener" );
		}
	}

	/*
	 * Might want to implement AutoFlushEventListener in future?
	public void onAutoFlush(AutoFlushEvent event) throws HibernateException {
		// Currently not needed as auto-flush is not happening
		// when out of transaction.
	}
	*/

}
