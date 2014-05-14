/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.jmx.impl;

import java.util.Properties;
import javax.naming.Context;
import javax.naming.InitialContext;

import org.hibernate.CacheMode;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.engine.service.classloading.spi.ClassLoaderService;
import org.hibernate.search.engine.service.classloading.spi.ClassLoadingException;
import org.hibernate.search.engine.service.spi.ServiceManager;
import org.hibernate.search.jmx.IndexControlMBean;
import org.hibernate.search.util.impl.JNDIHelper;

/**
 * Implementation of the {@code IndexControlMBean} JMX attributes and operations.
 *
 * @author Hardy Ferentschik
 */
public class IndexControl implements IndexControlMBean {

	private final Properties jndiProperties;
	private final String sessionFactoryJndiName;
	private final ServiceManager serviceManager;

	private int batchSize = 25;
	private int numberOfObjectLoadingThreads = 2;
	private int numberOfFetchingThreads = 4;

	public IndexControl(Properties properties, ServiceManager serviceManager) {
		this.sessionFactoryJndiName = properties.getProperty( "hibernate.session_factory_name" );
		this.jndiProperties = JNDIHelper.getJndiProperties( properties, JNDIHelper.HIBERNATE_JNDI_PREFIX );
		this.serviceManager = serviceManager;
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

		SessionFactory factory = getSessionFactory();
		Session session = factory.openSession();
		FullTextSession fulltextSession = Search.getFullTextSession( session );
		try {
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
		session.close();
	}

	@Override
	public void optimize(String entity) {
		Class<?> clazz = getEntityClass( entity );

		SessionFactory factory = getSessionFactory();
		Session session = factory.openSession();
		FullTextSession fullTextSession = Search.getFullTextSession( session );
		fullTextSession.beginTransaction();
		fullTextSession.getSearchFactory().optimize( clazz );
		fullTextSession.getTransaction().commit();
		session.close();
	}

	@Override
	public void purge(String entity) {
		Class<?> clazz = getEntityClass( entity );

		SessionFactory factory = getSessionFactory();
		Session session = factory.openSession();
		FullTextSession fullTextSession = Search.getFullTextSession( session );
		fullTextSession.beginTransaction();
		fullTextSession.purgeAll( clazz );
		fullTextSession.getTransaction().commit();
		session.close();
	}

	private Class<?> getEntityClass(String entity) {
		Class<?> clazz;
		ClassLoaderService classLoaderService = serviceManager.requestService( ClassLoaderService.class );
		try {
			clazz = classLoaderService.classForName( entity );
		}
		catch (ClassLoadingException e) {
			throw new IllegalArgumentException( entity + " not a indexed entity" );
		}
		finally {
			serviceManager.releaseService( ClassLoaderService.class );
		}
		return clazz;
	}

	private SessionFactory getSessionFactory() {
		try {
			Context initialContext;
			if ( jndiProperties.isEmpty() ) {
				initialContext = new InitialContext();
			}
			else {
				initialContext = new InitialContext( jndiProperties );
			}
			return (SessionFactory) initialContext.lookup( sessionFactoryJndiName );
		}
		catch (Exception e) {
			throw new UnsupportedOperationException(
					"In order for this operation to work the SessionFactory must be bound to JNDI"
			);
		}
	}
}
