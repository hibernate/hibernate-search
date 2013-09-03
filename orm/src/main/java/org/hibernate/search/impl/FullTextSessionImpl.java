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
import java.sql.Connection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.CacheMode;
import org.hibernate.Criteria;
import org.hibernate.Filter;
import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.IdentifierLoadAccess;
import org.hibernate.Interceptor;
import org.hibernate.LobHelper;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.NaturalIdLoadAccess;
import org.hibernate.Query;
import org.hibernate.ReplicationMode;
import org.hibernate.SQLQuery;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.SessionFactory;
import org.hibernate.SimpleNaturalIdLoadAccess;
import org.hibernate.Transaction;
import org.hibernate.TypeHelper;
import org.hibernate.UnknownProfileException;
import org.hibernate.Session;
import org.hibernate.cache.spi.CacheKey;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.jdbc.spi.JdbcConnectionAccess;
import org.hibernate.engine.query.spi.sql.NativeSQLQuerySpecification;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.NonFlushedChanges;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.QueryParameters;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.query.spi.ParameterMetadata;
import org.hibernate.engine.transaction.spi.TransactionCoordinator;
import org.hibernate.event.spi.EventSource;
import org.hibernate.jdbc.ReturningWork;
import org.hibernate.loader.custom.CustomQuery;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.FullTextSharedSessionBuilder;
import org.hibernate.search.MassIndexer;
import org.hibernate.search.SearchFactory;
import org.hibernate.search.backend.TransactionContext;
import org.hibernate.search.backend.spi.Work;
import org.hibernate.search.backend.spi.WorkType;
import org.hibernate.search.backend.impl.EventSourceTransactionContext;
import org.hibernate.search.engine.ServiceManager;
import org.hibernate.search.engine.spi.SearchFactoryImplementor;
import org.hibernate.search.hcore.impl.MassIndexerFactoryProvider;
import org.hibernate.search.query.hibernate.impl.FullTextQueryImpl;
import org.hibernate.search.spi.MassIndexerFactory;
import org.hibernate.search.util.impl.ContextHelper;
import org.hibernate.search.util.impl.HibernateHelper;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;
import org.hibernate.stat.SessionStatistics;
import org.hibernate.type.Type;

/**
 * Lucene full text search aware session.
 *
 * @author Emmanuel Bernard
 * @author John Griffin
 * @author Hardy Ferentschik
 */
public class FullTextSessionImpl implements FullTextSession, SessionImplementor {
	private static final Log log = LoggerFactory.make();

	private final Session session;
	private final SessionImplementor sessionImplementor;
	private transient SearchFactoryImplementor searchFactory;
	private final TransactionContext transactionContext;


	public FullTextSessionImpl(org.hibernate.Session session) {
		if ( session == null ) {
			throw log.getNullSessionPassedToFullTextSessionCreationException();
		}
		this.session = session;
		this.transactionContext = new EventSourceTransactionContext( (EventSource) session );
		this.sessionImplementor = (SessionImplementor) session;
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
		//We shouldn't expose the ServiceManager in phases other than startup or teardown, that's why the cast is required.
		//Exceptionally, this very specific case is fine. TODO: cleanup this mess.
		MutableSearchFactory msf = (MutableSearchFactory) getSearchFactoryImplementor();
		ServiceManager serviceManager = msf.getServiceManager();
		MassIndexerFactory service = serviceManager.requestService( MassIndexerFactoryProvider.class, null );
		return service.createMassIndexer( getSearchFactoryImplementor(), getSessionFactory(), types );
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
	public String getTenantIdentifier() {
		return session.getTenantIdentifier();
	}

	@Override
	public Transaction beginTransaction() throws HibernateException {
		return session.beginTransaction();
	}

	@Override
	public void cancelQuery() throws HibernateException {
		session.cancelQuery();
	}

	@Override
	public void clear() {
		//FIXME should session clear work with the lucene queue
		session.clear();
	}

	@Override
	public Connection close() throws HibernateException {
		return session.close();
	}

	@Override
	public boolean contains(Object object) {
		return session.contains( object );
	}

	@Override
	public Criteria createCriteria(String entityName) {
		return session.createCriteria( entityName );
	}

	@Override
	public Criteria createCriteria(String entityName, String alias) {
		return session.createCriteria( entityName, alias );
	}

	@Override
	public Criteria createCriteria(Class persistentClass) {
		return session.createCriteria( persistentClass );
	}

	@Override
	public Criteria createCriteria(Class persistentClass, String alias) {
		return session.createCriteria( persistentClass, alias );
	}

	@Override
	public Query createFilter(Object collection, String queryString) throws HibernateException {
		return session.createFilter( collection, queryString );
	}

	@Override
	public Query createQuery(String queryString) throws HibernateException {
		return session.createQuery( queryString );
	}

	@Override
	public SQLQuery createSQLQuery(String queryString) throws HibernateException {
		return session.createSQLQuery( queryString );
	}

	@Override
	public void delete(String entityName, Object object) throws HibernateException {
		session.delete( entityName, object );
	}

	@Override
	public void delete(Object object) throws HibernateException {
		session.delete( object );
	}

	@Override
	public void disableFilter(String filterName) {
		session.disableFilter( filterName );
	}

	@Override
	public Connection disconnect() throws HibernateException {
		return session.disconnect();
	}

	@Override
	public Filter enableFilter(String filterName) {
		return session.enableFilter( filterName );
	}

	@Override
	public void evict(Object object) throws HibernateException {
		session.evict( object );
	}

	@Override
	public FullTextSharedSessionBuilder sessionWithOptions() {
		return new FullTextSharedSessionBuilderDelegator( session.sessionWithOptions() );
	}

	@Override
	public void flush() throws HibernateException {
		session.flush();
	}

	@Override
	public Object get(Class clazz, Serializable id) throws HibernateException {
		return session.get( clazz, id );
	}

	@Override
	public Object get(Class clazz, Serializable id, LockMode lockMode) throws HibernateException {
		return session.get( clazz, id, lockMode );
	}

	@Override
	public Object get(Class clazz, Serializable id, LockOptions lockOptions) throws HibernateException {
		return session.get( clazz, id, lockOptions );
	}

	@Override
	public Object get(String entityName, Serializable id) throws HibernateException {
		return session.get( entityName, id );
	}

	@Override
	public Object get(String entityName, Serializable id, LockMode lockMode) throws HibernateException {
		return session.get( entityName, id, lockMode );
	}

	@Override
	public Object get(String entityName, Serializable id, LockOptions lockOptions) throws HibernateException {
		return session.get( entityName, id, lockOptions );
	}

	@Override
	public CacheMode getCacheMode() {
		return session.getCacheMode();
	}

	@Override
	public LockMode getCurrentLockMode(Object object) throws HibernateException {
		return session.getCurrentLockMode( object );
	}

	@Override
	public Filter getEnabledFilter(String filterName) {
		return session.getEnabledFilter( filterName );
	}

	@Override
	public JdbcConnectionAccess getJdbcConnectionAccess() {
		return sessionImplementor.getJdbcConnectionAccess();
	}

	@Override
	public EntityKey generateEntityKey(Serializable id, EntityPersister persister) {
		return sessionImplementor.generateEntityKey( id, persister );
	}

	@Override
	public CacheKey generateCacheKey(Serializable id, Type type, String entityOrRoleName) {
		return sessionImplementor.generateCacheKey( id, type, entityOrRoleName );
	}

	@Override
	public Interceptor getInterceptor() {
		return sessionImplementor.getInterceptor();
	}

	@Override
	public void setAutoClear(boolean enabled) {
		sessionImplementor.setAutoClear( enabled );
	}

	@Override
	public void disableTransactionAutoJoin() {
		sessionImplementor.disableTransactionAutoJoin();
	}

	@Override
	public boolean isTransactionInProgress() {
		return sessionImplementor.isTransactionInProgress();
	}

	@Override
	public void initializeCollection(PersistentCollection collection, boolean writing) throws HibernateException {
		sessionImplementor.initializeCollection( collection, writing );
	}

	@Override
	public Object internalLoad(String entityName, Serializable id, boolean eager, boolean nullable)
			throws HibernateException {
		return sessionImplementor.internalLoad( entityName, id, eager, nullable );
	}

	@Override
	public Object immediateLoad(String entityName, Serializable id) throws HibernateException {
		return sessionImplementor.immediateLoad( entityName, id );
	}

	@Override
	public long getTimestamp() {
		return sessionImplementor.getTimestamp();
	}

	@Override
	public SessionFactoryImplementor getFactory() {
		return sessionImplementor.getFactory();
	}

	@Override
	public List list(String query, QueryParameters queryParameters) throws HibernateException {
		return sessionImplementor.list( query, queryParameters );
	}

	@Override
	public Iterator iterate(String query, QueryParameters queryParameters) throws HibernateException {
		return sessionImplementor.iterate( query, queryParameters );
	}

	@Override
	public ScrollableResults scroll(String query, QueryParameters queryParameters) throws HibernateException {
		return sessionImplementor.scroll( query, queryParameters );
	}

	@Override
	public ScrollableResults scroll(Criteria criteria, ScrollMode scrollMode) {
		return sessionImplementor.scroll( criteria, scrollMode );
	}

	@Override
	public List list(Criteria criteria) {
		return sessionImplementor.list( criteria );
	}

	@Override
	public List listFilter(Object collection, String filter, QueryParameters queryParameters)
			throws HibernateException {
		return sessionImplementor.listFilter( collection, filter, queryParameters );
	}

	@Override
	public Iterator iterateFilter(Object collection, String filter, QueryParameters queryParameters)
			throws HibernateException {
		return sessionImplementor.iterateFilter( collection, filter, queryParameters );
	}

	@Override
	public EntityPersister getEntityPersister(String entityName, Object object) throws HibernateException {
		return sessionImplementor.getEntityPersister( entityName, object );
	}

	@Override
	public Object getEntityUsingInterceptor(EntityKey key) throws HibernateException {
		return sessionImplementor.getEntityUsingInterceptor( key );
	}

	@Override
	public Serializable getContextEntityIdentifier(Object object) {
		return sessionImplementor.getContextEntityIdentifier( object );
	}

	@Override
	public String bestGuessEntityName(Object object) {
		return sessionImplementor.bestGuessEntityName( object );
	}

	@Override
	public String guessEntityName(Object entity) throws HibernateException {
		return sessionImplementor.guessEntityName( entity );
	}

	@Override
	public Object instantiate(String entityName, Serializable id) throws HibernateException {
		return sessionImplementor.instantiate( entityName, id );
	}

	@Override
	public List listCustomQuery(CustomQuery customQuery, QueryParameters queryParameters) throws HibernateException {
		return sessionImplementor.listCustomQuery( customQuery, queryParameters );
	}

	@Override
	public ScrollableResults scrollCustomQuery(CustomQuery customQuery, QueryParameters queryParameters)
			throws HibernateException {
		return sessionImplementor.scrollCustomQuery( customQuery, queryParameters );
	}

	@Override
	public List list(NativeSQLQuerySpecification spec, QueryParameters queryParameters) throws HibernateException {
		return sessionImplementor.list( spec, queryParameters );
	}

	@Override
	public ScrollableResults scroll(NativeSQLQuerySpecification spec, QueryParameters queryParameters)
			throws HibernateException {
		return sessionImplementor.scroll( spec, queryParameters );
	}

	@Override
	public Object getFilterParameterValue(String filterParameterName) {
		return sessionImplementor.getFilterParameterValue( filterParameterName );
	}

	@Override
	public Type getFilterParameterType(String filterParameterName) {
		return sessionImplementor.getFilterParameterType( filterParameterName );
	}

	@Override
	public Map getEnabledFilters() {
		return sessionImplementor.getEnabledFilters();
	}

	@Override
	public int getDontFlushFromFind() {
		return sessionImplementor.getDontFlushFromFind();
	}

	@Override
	public PersistenceContext getPersistenceContext() {
		return sessionImplementor.getPersistenceContext();
	}

	@Override
	public int executeUpdate(String query, QueryParameters queryParameters) throws HibernateException {
		return sessionImplementor.executeUpdate( query, queryParameters );
	}

	@Override
	public int executeNativeUpdate(NativeSQLQuerySpecification specification, QueryParameters queryParameters)
			throws HibernateException {
		return sessionImplementor.executeNativeUpdate( specification, queryParameters );
	}

	@Override
	public NonFlushedChanges getNonFlushedChanges() throws HibernateException {
		return sessionImplementor.getNonFlushedChanges();
	}

	@Override
	public void applyNonFlushedChanges(NonFlushedChanges nonFlushedChanges) throws HibernateException {
		sessionImplementor.applyNonFlushedChanges( nonFlushedChanges );
	}

	@Override
	public String getEntityName(Object object) throws HibernateException {
		return session.getEntityName( object );
	}

	@Override
	public FlushMode getFlushMode() {
		return session.getFlushMode();
	}

	@Override
	public Serializable getIdentifier(Object object) throws HibernateException {
		return session.getIdentifier( object );
	}

	@Override
	public Query getNamedQuery(String queryName) throws HibernateException {
		return session.getNamedQuery( queryName );
	}

	@Override
	public Query getNamedSQLQuery(String name) {
		return sessionImplementor.getNamedSQLQuery( name );
	}

	@Override
	public boolean isEventSource() {
		return sessionImplementor.isEventSource();
	}

	@Override
	public void afterScrollOperation() {
		sessionImplementor.afterScrollOperation();
	}

	@Override
	public void setFetchProfile(String name) {
		sessionImplementor.setFetchProfile( name );
	}

	@Override
	public TransactionCoordinator getTransactionCoordinator() {
		return sessionImplementor.getTransactionCoordinator();
	}

	@Override
	public String getFetchProfile() {
		return sessionImplementor.getFetchProfile();
	}

	@Override
	public boolean isClosed() {
		return sessionImplementor.isClosed();
	}

	@Override
	public LoadQueryInfluencers getLoadQueryInfluencers() {
		return sessionImplementor.getLoadQueryInfluencers();
	}

	@Override
	public SessionFactory getSessionFactory() {
		return session.getSessionFactory();
	}

	public SessionStatistics getStatistics() {
		return session.getStatistics();
	}

	public boolean isReadOnly(Object entityOrProxy) {
		return session.isReadOnly( entityOrProxy );
	}

	public Transaction getTransaction() {
		return session.getTransaction();
	}

	public boolean isConnected() {
		return session.isConnected();
	}

	public boolean isDirty() throws HibernateException {
		return session.isDirty();
	}

	public boolean isDefaultReadOnly() {
		return session.isDefaultReadOnly();
	}

	public boolean isOpen() {
		return session.isOpen();
	}

	public Object load(String entityName, Serializable id) throws HibernateException {
		return session.load( entityName, id );
	}

	public Object load(String entityName, Serializable id, LockMode lockMode) throws HibernateException {
		return session.load( entityName, id, lockMode );
	}

	public Object load(String entityName, Serializable id, LockOptions lockOptions) throws HibernateException {
		return session.load( entityName, id, lockOptions );
	}

	public void load(Object object, Serializable id) throws HibernateException {
		session.load( object, id );
	}

	public Object load(Class theClass, Serializable id) throws HibernateException {
		return session.load( theClass, id );
	}

	public Object load(Class theClass, Serializable id, LockMode lockMode) throws HibernateException {
		return session.load( theClass, id, lockMode );
	}

	public Object load(Class theClass, Serializable id, LockOptions lockOptions) throws HibernateException {
		return session.load( theClass, id, lockOptions );
	}

	public void lock(String entityName, Object object, LockMode lockMode) throws HibernateException {
		session.lock( entityName, object, lockMode );
	}

	public LockRequest buildLockRequest(LockOptions lockOptions) {
		return session.buildLockRequest( lockOptions );
	}

	public void lock(Object object, LockMode lockMode) throws HibernateException {
		session.lock( object, lockMode );
	}

	public Object merge(String entityName, Object object) throws HibernateException {
		return session.merge( entityName, object );
	}

	public Object merge(Object object) throws HibernateException {
		return session.merge( object );
	}

	public void persist(String entityName, Object object) throws HibernateException {
		session.persist( entityName, object );
	}

	public void persist(Object object) throws HibernateException {
		session.persist( object );
	}

	public void reconnect(Connection connection) throws HibernateException {
		session.reconnect( connection );
	}

	public void refresh(Object object) throws HibernateException {
		session.refresh( object );
	}

	@Override
	public void refresh(String entityName, Object object) throws HibernateException {
		session.refresh( entityName, object );
	}

	public void refresh(Object object, LockMode lockMode) throws HibernateException {
		session.refresh( object, lockMode );
	}

	public void refresh(Object object, LockOptions lockOptions) throws HibernateException {
		session.refresh( object, lockOptions );
	}

	@Override
	public void refresh(String entityName, Object object, LockOptions lockOptions) throws HibernateException {
		session.refresh( entityName, object, lockOptions );
	}

	public void replicate(String entityName, Object object, ReplicationMode replicationMode) throws HibernateException {
		session.replicate( entityName, object, replicationMode );
	}

	public void replicate(Object object, ReplicationMode replicationMode) throws HibernateException {
		session.replicate( object, replicationMode );
	}

	public Serializable save(String entityName, Object object) throws HibernateException {
		return session.save( entityName, object );
	}

	public Serializable save(Object object) throws HibernateException {
		return session.save( object );
	}

	public void saveOrUpdate(String entityName, Object object) throws HibernateException {
		session.saveOrUpdate( entityName, object );
	}

	public void saveOrUpdate(Object object) throws HibernateException {
		session.saveOrUpdate( object );
	}

	public void setCacheMode(CacheMode cacheMode) {
		session.setCacheMode( cacheMode );
	}

	public void setDefaultReadOnly(boolean readOnly) {
		session.setDefaultReadOnly( readOnly );
	}

	public void setFlushMode(FlushMode flushMode) {
		session.setFlushMode( flushMode );
	}

	@Override
	public Connection connection() {
		return sessionImplementor.connection();
	}

	public void setReadOnly(Object entity, boolean readOnly) {
		session.setReadOnly( entity, readOnly );
	}

	public void doWork(org.hibernate.jdbc.Work work) throws HibernateException {
		session.doWork( work );
	}

	@Override
	public <T> T doReturningWork(ReturningWork<T> work) throws HibernateException {
		return session.doReturningWork( work );
	}

	public void update(String entityName, Object object) throws HibernateException {
		session.update( entityName, object );
	}

	public void update(Object object) throws HibernateException {
		session.update( object );
	}

	public boolean isFetchProfileEnabled(String name) throws UnknownProfileException {
		return session.isFetchProfileEnabled( name );
	}

	public void enableFetchProfile(String name) throws UnknownProfileException {
		session.enableFetchProfile( name );
	}

	public void disableFetchProfile(String name) throws UnknownProfileException {
		session.disableFetchProfile( name );
	}

	public TypeHelper getTypeHelper() {
		return session.getTypeHelper();
	}

	public LobHelper getLobHelper() {
		return session.getLobHelper();
	}

	@Override
	public <T> T execute(Callback<T> callback) {
		return sessionImplementor.execute( callback );
	}

	@Override
	public IdentifierLoadAccess byId(String entityName) {
		return session.byId( entityName );
	}

	@Override
	public IdentifierLoadAccess byId(Class entityClass) {
		return session.byId( entityClass );
	}

	@Override
	public NaturalIdLoadAccess byNaturalId(String entityName) {
		return session.byNaturalId( entityName );
	}

	@Override
	public NaturalIdLoadAccess byNaturalId(Class entityClass) {
		return session.byNaturalId( entityClass );
	}

	@Override
	public SimpleNaturalIdLoadAccess bySimpleNaturalId(String entityName) {
		return session.bySimpleNaturalId( entityName );
	}

	@Override
	public SimpleNaturalIdLoadAccess bySimpleNaturalId(Class entityClass) {
		return session.bySimpleNaturalId( entityClass );
	}

}
