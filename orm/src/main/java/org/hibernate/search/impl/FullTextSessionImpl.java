/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
import org.hibernate.search.SearchFactory;
import org.hibernate.search.batchindexing.impl.DefaultMassIndexerFactory;
import org.hibernate.search.backend.TransactionContext;
import org.hibernate.search.backend.impl.EventSourceTransactionContext;
import org.hibernate.search.backend.spi.Work;
import org.hibernate.search.backend.spi.WorkType;
import org.hibernate.search.engine.integration.impl.ExtendedSearchIntegrator;
import org.hibernate.search.engine.service.spi.ServiceManager;
import org.hibernate.search.query.hibernate.impl.FullTextQueryImpl;
import org.hibernate.search.batchindexing.spi.MassIndexerFactory;
import org.hibernate.search.batchindexing.spi.MassIndexerWithTenant;
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
final class FullTextSessionImpl extends SessionDelegatorBaseImpl implements FullTextSession, SessionImplementor {

	private static final Log log = LoggerFactory.make();

	private transient ExtendedSearchIntegrator searchFactory;
	private transient SearchFactory searchFactoryAPI;

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

	@Override
	public <T> void purgeAll(Class<T> entityType) {
		purge( entityType, null );
	}

	@Override
	public void flushToIndexes() {
		ExtendedSearchIntegrator extendedIntegrator = getSearchIntegrator();
		extendedIntegrator.getWorker().flushWorks( transactionContext );
	}

	@Override
	public <T> void purge(Class<T> entityType, Serializable id) {
		if ( entityType == null ) {
			return;
		}

		Set<Class<?>> targetedClasses = getSearchIntegrator().getIndexedTypesPolymorphic(
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

	private void createAndPerformWork(Class<?> clazz, Serializable id, WorkType workType) {
		Work work = new Work( clazz, id, workType );
		getSearchIntegrator().getWorker().performWork( work, transactionContext );
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
		ExtendedSearchIntegrator extendedIntegrator = getSearchIntegrator();
		//not strictly necessary but a small optimization
		if ( extendedIntegrator.getIndexBinding( clazz ) == null ) {
			String msg = "Entity to index is not an @Indexed entity: " + entity.getClass().getName();
			throw new IllegalArgumentException( msg );
		}
		Serializable id = session.getIdentifier( entity );
		Work work = new Work( entity, id, WorkType.INDEX );
		extendedIntegrator.getWorker().performWork( work, transactionContext );

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
		MassIndexer massIndexer = massIndexerFactory.createMassIndexer( getSearchIntegrator(), getFactory(), types );
		if ( massIndexer instanceof MassIndexerWithTenant ) {
			( (MassIndexerWithTenant) massIndexer ).tenantIdentifier( getTenantIdentifier() );
		}
		return massIndexer;
	}

	@Override
	public SearchFactory getSearchFactory() {
		if ( searchFactoryAPI == null ) {
			searchFactoryAPI = new SearchFactoryImpl( getSearchIntegrator() );
		}
		return searchFactoryAPI;
	}

	private ExtendedSearchIntegrator getSearchIntegrator() {
		if ( searchFactory == null ) {
			searchFactory = ContextHelper.getSearchintegrator( session );
		}
		return searchFactory;
	}

	@Override
	public FullTextSharedSessionBuilder sessionWithOptions() {
		return new FullTextSharedSessionBuilderDelegator( super.sessionWithOptions() );
	}

	private MassIndexerFactory createMassIndexerFactory() {
		MassIndexerFactory factory;
		Properties properties = getSearchIntegrator().getConfigurationProperties();
		String factoryClassName = properties.getProperty( MassIndexerFactory.MASS_INDEXER_FACTORY_CLASSNAME );

		if ( factoryClassName != null ) {
			ExtendedSearchIntegrator extendedIntegrator = getSearchIntegrator();
			ServiceManager serviceManager = extendedIntegrator.getServiceManager();
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
