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
import java.util.List;

import org.hibernate.CacheMode;
import org.hibernate.Criteria;
import org.hibernate.FlushMode;
import org.hibernate.LockMode;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.criterion.CriteriaSpecification;
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
public class IdentifierConsumerEntityProducer implements SessionAwareRunnable {
	
	private static final Logger log = LoggerFactory.make();

	private final ProducerConsumerQueue<List<Serializable>> source;
	private final ProducerConsumerQueue<List<?>> destination;
	private final SessionFactory sessionFactory;
	private final CacheMode cacheMode;
	private final Class<?> type;
	private final MassIndexerProgressMonitor monitor;

	public IdentifierConsumerEntityProducer(
			ProducerConsumerQueue<List<Serializable>> fromIdentifierListToEntities,
			ProducerConsumerQueue<List<?>> fromEntityToAddwork,
			MassIndexerProgressMonitor monitor,
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

	public void run(Session upperSession) {
		log.trace( "started" );
		Session session = upperSession;
		if ( upperSession == null ) {
			session = sessionFactory.openSession();
		}
		session.setFlushMode( FlushMode.MANUAL );
		session.setCacheMode( cacheMode );
		session.setDefaultReadOnly( true );
		try {
			Transaction transaction = Helper.getTransactionAndMarkForJoin( session );
			transaction.begin();
			loadAllFromQueue( session );
			transaction.commit();
		}
		catch (Throwable e) {
			log.error( "error during batch indexing: ", e );
		}
		finally {
			if (upperSession == null) {
				session.close();
			}
		}
		log.trace( "finished" );
	}
	
	private void loadAllFromQueue(Session session) {
		try {
			Object take;
			do {
				take = source.take();
				if ( take != null ) {
					@SuppressWarnings("unchecked")
					List<Serializable> listIds = (List<Serializable>) take;
					log.trace( "received list of ids {}", listIds );
					loadList( listIds, session );
				}
			}
			while ( take != null );
		}
		catch (InterruptedException e) {
			// just quit
			Thread.currentThread().interrupt();
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
			.setResultTransformer( CriteriaSpecification.DISTINCT_ROOT_ENTITY )
			.add( Restrictions.in( "id", listIds ) );
		List<?> list = criteria.list();
		monitor.entitiesLoaded( list.size() );
		session.clear();
		destination.put( list );
	}

}
