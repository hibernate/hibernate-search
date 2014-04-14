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
package org.hibernate.search.impl;

import java.io.Serializable;
import java.util.Properties;
import java.util.Set;

import org.hibernate.engine.query.spi.ParameterMetadata;
import org.hibernate.engine.spi.SessionDelegatorBaseImpl;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.event.spi.EventSource;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.FullTextSharedSessionBuilder;
import org.hibernate.search.MassIndexer;
import org.hibernate.search.batchindexing.impl.DefaultMassIndexerFactory;
import org.hibernate.search.engine.SearchFactory;
import org.hibernate.search.backend.TransactionContext;
import org.hibernate.search.backend.impl.EventSourceTransactionContext;
import org.hibernate.search.backend.spi.Work;
import org.hibernate.search.backend.spi.WorkType;
import org.hibernate.search.engine.service.spi.ServiceManager;
import org.hibernate.search.engine.spi.SearchFactoryImplementor;
import org.hibernate.search.query.hibernate.impl.FullTextQueryImpl;
import org.hibernate.search.batchindexing.spi.MassIndexerFactory;
import org.hibernate.search.util.impl.ClassLoaderHelper;
import org.hibernate.search.hcore.util.impl.ContextHelper;
import org.hibernate.search.hcore.util.impl.HibernateHelper;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * Lucene full text search aware session.
 *
 * @author Emmanuel Bernard
 * @author John Griffin
 * @author Hardy Ferentschik
 */
public class FullTextSessionImpl extends SessionDelegatorBaseImpl implements FullTextSession, SessionImplementor {
	private static final Log log = LoggerFactory.make();

	private transient SearchFactoryImplementor searchFactory;
	private final TransactionContext transactionContext;

	public FullTextSessionImpl(org.hibernate.Session session) {
		super( (SessionImplementor) session, session );
		if ( session == null ) {
			throw log.getNullSessionPassedToFullTextSessionCreationException();
		}
		this.transactionContext = new EventSourceTransactionContext( (EventSource) session );
	}

	/**
	 * Execute a Lucene query and retrieve managed objects of type entities (or their indexed subclasses)
	 * If entities is empty, include all indexed entities
	 *
	 * @param entities must be immutable for the lifetime of the query object
	 */
	@Override
	public FullTextQuery createFullTextQuery(org.apache.lucene.search.Query luceneQuery, Class<?>... entities) {
		return new FullTextQueryImpl(
				luceneQuery,
				entities,
				sessionImplementor,
				new ParameterMetadata( null, null )
		);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public <T> void purgeAll(Class<T> entityType) {
		purge( entityType, null );
	}

	@Override
	public void flushToIndexes() {
		SearchFactoryImplementor searchFactoryImplementor = getSearchFactoryImplementor();
		searchFactoryImplementor.getWorker().flushWorks( transactionContext );
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public <T> void purge(Class<T> entityType, Serializable id) {
		if ( entityType == null ) {
			return;
		}

		Set<Class<?>> targetedClasses = getSearchFactoryImplementor().getIndexedTypesPolymorphic(
				new Class[] {
						entityType
				}
		);
		if ( targetedClasses.isEmpty() ) {
			String msg = entityType.getName() + " is not an indexed entity or a subclass of an indexed entity";
			throw new IllegalArgumentException( msg );
		}

		for ( Class<?> clazz : targetedClasses ) {
			if ( id == null ) {
				createAndPerformWork( clazz, null, WorkType.PURGE_ALL );
			}
			else {
				createAndPerformWork( clazz, id, WorkType.PURGE );
			}
		}
	}

	private <T> void createAndPerformWork(Class<T> clazz, Serializable id, WorkType workType) {
		Work<T> work;
		work = new Work<T>( clazz, id, workType );
		getSearchFactoryImplementor().getWorker().performWork( work, transactionContext );
	}

	/**
	 * (Re-)index an entity.
	 * The entity must be associated with the session and non indexable entities are ignored.
	 *
	 * @param entity The entity to index - must not be <code>null</code>.
	 *
	 * @throws IllegalArgumentException if entity is null or not an @Indexed entity
	 */
	@Override
	public <T> void index(T entity) {
		if ( entity == null ) {
			throw new IllegalArgumentException( "Entity to index should not be null" );
		}

		Class<?> clazz = HibernateHelper.getClass( entity );
		//TODO cache that at the FTSession level
		SearchFactoryImplementor searchFactoryImplementor = getSearchFactoryImplementor();
		//not strictly necessary but a small optimization
		if ( searchFactoryImplementor.getIndexBinding( clazz ) == null ) {
			String msg = "Entity to index is not an @Indexed entity: " + entity.getClass().getName();
			throw new IllegalArgumentException( msg );
		}
		Serializable id = session.getIdentifier( entity );
		Work<T> work = new Work<T>( entity, id, WorkType.INDEX );
		searchFactoryImplementor.getWorker().performWork( work, transactionContext );

		//TODO
		//need to add elements in a queue kept at the Session level
		//the queue will be processed by a Lucene(Auto)FlushEventListener
		//note that we could keep this queue somewhere in the event listener in the mean time but that requires
		//a synchronized hashmap holding this queue on a per session basis plus some session house keeping (yuk)
		//another solution would be to subclass SessionImpl instead of having this LuceneSession delegation model
		//this is an open discussion
	}

	@Override
	public MassIndexer createIndexer(Class<?>... types) {
		MassIndexerFactory massIndexerFactory = createMassIndexerFactory();
		return massIndexerFactory.createMassIndexer( getSearchFactoryImplementor(), getFactory(), types );
	}

	@Override
	public SearchFactory getSearchFactory() {
		return getSearchFactoryImplementor();
	}

	private SearchFactoryImplementor getSearchFactoryImplementor() {
		if ( searchFactory == null ) {
			searchFactory = ContextHelper.getSearchFactory( session );
		}
		return searchFactory;
	}

	@Override
	public FullTextSharedSessionBuilder sessionWithOptions() {
		return new FullTextSharedSessionBuilderDelegator( super.sessionWithOptions() );
	}

	private MassIndexerFactory createMassIndexerFactory() {
		MassIndexerFactory factory;
		Properties properties = getSearchFactoryImplementor().getConfigurationProperties();
		String factoryClassName = properties.getProperty( MassIndexerFactory.MASS_INDEXER_FACTORY_CLASSNAME );

		if ( factoryClassName != null ) {
			SearchFactoryImplementor searchFactoryImplementor = getSearchFactoryImplementor();
			ServiceManager serviceManager = searchFactoryImplementor.getServiceManager();
			factory = ClassLoaderHelper.instanceFromName(
					MassIndexerFactory.class, factoryClassName, "Mass indexer factory", serviceManager
			);
		}
		else {
			factory = new DefaultMassIndexerFactory();
		}
		factory.initialize( properties );
		return factory;
	}

}
