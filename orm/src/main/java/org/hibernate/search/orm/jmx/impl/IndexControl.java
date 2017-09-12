/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.orm.jmx.impl;

import org.hibernate.CacheMode;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.engine.integration.impl.ExtendedSearchIntegrator;
import org.hibernate.search.engine.service.classloading.spi.ClassLoaderService;
import org.hibernate.search.engine.service.classloading.spi.ClassLoadingException;
import org.hibernate.search.jmx.IndexControlMBean;
import org.hibernate.search.spi.IndexedTypeIdentifier;

/**
 * Implementation of the {@code IndexControlMBean} JMX attributes and operations.
 *
 * @author Hardy Ferentschik
 */
public class IndexControl implements IndexControlMBean {

	private final SessionFactory hibernateSessionFactory;
	private final ExtendedSearchIntegrator extendedIntegrator;

	private int batchSize = 25;
	private int numberOfObjectLoadingThreads = 2;
	private int numberOfFetchingThreads = 4;

	public IndexControl(ExtendedSearchIntegrator extendedIntegrator, SessionFactory factory) {
		this.extendedIntegrator = extendedIntegrator;
		this.hibernateSessionFactory = factory;
	}

	@Override
	public void setBatchSize(int batchSize) {
		this.batchSize = batchSize;
	}

	@Override
	public int getBatchSize() {
		return batchSize;
	}

	@Override
	public void setNumberOfObjectLoadingThreads(int numberOfThreads) {
		this.numberOfObjectLoadingThreads = numberOfThreads;
	}

	@Override
	public int getNumberOfObjectLoadingThreads() {
		return numberOfObjectLoadingThreads;
	}

	@Override
	public void setNumberOfFetchingThreads(int numberOfThreads) {
		this.numberOfFetchingThreads = numberOfThreads;
	}

	@Override
	public int getNumberOfFetchingThreads() {
		return numberOfFetchingThreads;
	}

	@Override
	public void index(String entity) {
		Class<?> clazz = getEntityClass( entity );

		try ( Session session = hibernateSessionFactory.openSession() ) {
			FullTextSession fulltextSession = Search.getFullTextSession( session );
			fulltextSession.createIndexer( clazz )
					.batchSizeToLoadObjects( batchSize )
					.cacheMode( CacheMode.NORMAL )
					.threadsToLoadObjects( numberOfObjectLoadingThreads )
					.startAndWait();
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new SearchException( "Unable to complete indexing" );
		}
	}

	@Override
	public void optimize(String entity) {
		IndexedTypeIdentifier typeIdentifier = extendedIntegrator.getIndexBindings().keyFromName( entity );
		extendedIntegrator.optimize( typeIdentifier );
	}

	@Override
	public void purge(String entity) {
		Class<?> clazz = getEntityClass( entity );
		try ( Session session = hibernateSessionFactory.openSession() ) {
			FullTextSession fullTextSession = Search.getFullTextSession( session );
			Transaction transaction = ( (SessionImplementor) session ).accessTransaction();
			final boolean controlTransactions = ! transaction.isActive();
			if ( controlTransactions ) {
				transaction.begin();
			}
			try {
				fullTextSession.purgeAll( clazz );
			}
			finally {
				if ( controlTransactions ) {
					transaction.commit();
				}
			}
		}
	}

	private Class<?> getEntityClass(String entity) {
		Class<?> clazz;
		ClassLoaderService classLoaderService = extendedIntegrator.getServiceManager().getClassLoaderService();
		try {
			clazz = classLoaderService.classForName( entity );
		}
		catch (ClassLoadingException e) {
			throw new IllegalArgumentException( entity + " not a indexed entity" );
		}
		return clazz;
	}

}
