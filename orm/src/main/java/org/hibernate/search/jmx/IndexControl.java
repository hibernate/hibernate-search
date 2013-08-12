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
package org.hibernate.search.jmx;

import java.util.Properties;
import javax.naming.Context;
import javax.naming.InitialContext;

import org.hibernate.CacheMode;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.SearchException;
import org.hibernate.search.util.impl.ClassLoaderHelper;
import org.hibernate.search.util.impl.JNDIHelper;


/**
 * Implementation of the {@code IndexControlMBean} JMX attributes and operations.
 *
 * @author Hardy Ferentschik
 */
public class IndexControl implements IndexControlMBean {

	private final Properties jndiProperties;
	private final String sessionFactoryJndiName;

	private int batchSize = 25;
	private int numberOfObjectLoadingThreads = 2;
	private int numberOfFetchingThreads = 4;

	public IndexControl(Properties props) {
		this.sessionFactoryJndiName = props.getProperty( "hibernate.session_factory_name" );
		this.jndiProperties = JNDIHelper.getJndiProperties( props, JNDIHelper.HIBERNATE_JNDI_PREFIX );
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
					.threadsForSubsequentFetching( numberOfFetchingThreads )
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
		try {
			clazz = ClassLoaderHelper.classForName( entity, IndexControl.class.getClassLoader() );
		}
		catch (ClassNotFoundException e) {
			throw new IllegalArgumentException( entity + "not a indexed entity" );
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
