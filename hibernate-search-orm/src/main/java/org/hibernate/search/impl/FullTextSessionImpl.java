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
import org.hibernate.SharedSessionBuilder;
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
import org.hibernate.internal.CriteriaImpl;
import org.hibernate.jdbc.ReturningWork;
import org.hibernate.loader.custom.CustomQuery;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.MassIndexer;
import org.hibernate.search.SearchFactory;
import org.hibernate.search.backend.TransactionContext;
import org.hibernate.search.backend.spi.Work;
import org.hibernate.search.backend.spi.WorkType;
import org.hibernate.search.backend.impl.EventSourceTransactionContext;
import org.hibernate.search.engine.spi.SearchFactoryImplementor;
import org.hibernate.search.query.hibernate.impl.FullTextQueryImpl;
import org.hibernate.search.util.impl.ContextHelper;
import org.hibernate.search.util.impl.HibernateHelper;
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
	private final Session session;
	private final SessionImplementor sessionImplementor;
	private transient SearchFactoryImplementor searchFactory;
	private final TransactionContext transactionContext;


	public FullTextSessionImpl(org.hibernate.Session session) {
		if ( session == null ) {
			throw new IllegalArgumentException( "Unable to create a FullTextSession from an null Session object" );
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
	public <T> void purgeAll(Class<T> entityType) {
		purge( entityType, null );
	}

	public void flushToIndexes() {
		SearchFactoryImplementor searchFactoryImplementor = getSearchFactoryImplementor();
		searchFactoryImplementor.getWorker().flushWorks( transactionContext );
	}

	/**
	 * {@inheritDoc}
	 */
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
	public <T> void index(T entity) {
		if ( entity == null ) {
			throw new IllegalArgumentException( "Entity to index should not be null" );
		}

		Class<?> clazz = HibernateHelper.getClass( entity );
		//TODO cache that at the FTSession level
		SearchFactoryImplementor searchFactoryImplementor = getSearchFactoryImplementor();
		//not strictly necessary but a small optimization
		if ( searchFactoryImplementor.getIndexBindingForEntity( clazz ) == null ) {
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

	public MassIndexer createIndexer(Class<?>... types) {
		if ( types.length == 0 ) {
			return new MassIndexerImpl( getSearchFactoryImplementor(), getSessionFactory(), Object.class );
		}
		else {
			return new MassIndexerImpl( getSearchFactoryImplementor(), getSessionFactory(), types );
		}
	}

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

	public Transaction beginTransaction() throws HibernateException {
		return session.beginTransaction();
	}

	public void cancelQuery() throws HibernateException {
		session.cancelQuery();
	}

	public void clear() {
		//FIXME should session clear work with the lucene queue
		session.clear();
	}

	public Connection close() throws HibernateException {
		return session.close();
	}

	public boolean contains(Object object) {
		return session.contains( object );
	}

	public Criteria createCriteria(String entityName) {
		return session.createCriteria( entityName );
	}

	public Criteria createCriteria(String entityName, String alias) {
		return session.createCriteria( entityName, alias );
	}

	public Criteria createCriteria(Class persistentClass) {
		return session.createCriteria( persistentClass );
	}

	public Criteria createCriteria(Class persistentClass, String alias) {
		return session.createCriteria( persistentClass, alias );
	}

	public Query createFilter(Object collection, String queryString) throws HibernateException {
		return session.createFilter( collection, queryString );
	}

	public Query createQuery(String queryString) throws HibernateException {
		return session.createQuery( queryString );
	}

	public SQLQuery createSQLQuery(String queryString) throws HibernateException {
		return session.createSQLQuery( queryString );
	}

	public void delete(String entityName, Object object) throws HibernateException {
		session.delete( entityName, object );
	}

	public void delete(Object object) throws HibernateException {
		session.delete( object );
	}

	public void disableFilter(String filterName) {
		session.disableFilter( filterName );
	}

	public Connection disconnect() throws HibernateException {
		return session.disconnect();
	}

	public Filter enableFilter(String filterName) {
		return session.enableFilter( filterName );
	}

	public void evict(Object object) throws HibernateException {
		session.evict( object );
	}

	@Override
	public SharedSessionBuilder sessionWithOptions() {
		return new FullTextSharedSessionBuilderDelegator( session.sessionWithOptions() );
	}

	public void flush() throws HibernateException {
		session.flush();
	}

	public Object get(Class clazz, Serializable id) throws HibernateException {
		return session.get( clazz, id );
	}

	public Object get(Class clazz, Serializable id, LockMode lockMode) throws HibernateException {
		return session.get( clazz, id, lockMode );
	}

	public Object get(Class clazz, Serializable id, LockOptions lockOptions) throws HibernateException {
		return session.get( clazz, id, lockOptions );
	}

	public Object get(String entityName, Serializable id) throws HibernateException {
		return session.get( entityName, id );
	}

	public Object get(String entityName, Serializable id, LockMode lockMode) throws HibernateException {
		return session.get( entityName, id, lockMode );
	}

	public Object get(String entityName, Serializable id, LockOptions lockOptions) throws HibernateException {
		return session.get( entityName, id, lockOptions );
	}

	public CacheMode getCacheMode() {
		return session.getCacheMode();
	}

	public LockMode getCurrentLockMode(Object object) throws HibernateException {
		return session.getCurrentLockMode( object );
	}

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

	public Interceptor getInterceptor() {
		return sessionImplementor.getInterceptor();
	}

	public void setAutoClear(boolean enabled) {
		sessionImplementor.setAutoClear( enabled );
	}

	@Override
	public void disableTransactionAutoJoin() {
		sessionImplementor.disableTransactionAutoJoin();
	}

	public boolean isTransactionInProgress() {
		return sessionImplementor.isTransactionInProgress();
	}

	public void initializeCollection(PersistentCollection collection, boolean writing) throws HibernateException {
		sessionImplementor.initializeCollection( collection, writing );
	}

	public Object internalLoad(String entityName, Serializable id, boolean eager, boolean nullable)
			throws HibernateException {
		return sessionImplementor.internalLoad( entityName, id, eager, nullable );
	}

	public Object immediateLoad(String entityName, Serializable id) throws HibernateException {
		return sessionImplementor.immediateLoad( entityName, id );
	}

	public long getTimestamp() {
		return sessionImplementor.getTimestamp();
	}

	public SessionFactoryImplementor getFactory() {
		return sessionImplementor.getFactory();
	}

	public List list(String query, QueryParameters queryParameters) throws HibernateException {
		return sessionImplementor.list( query, queryParameters );
	}

	public Iterator iterate(String query, QueryParameters queryParameters) throws HibernateException {
		return sessionImplementor.iterate( query, queryParameters );
	}

	public ScrollableResults scroll(String query, QueryParameters queryParameters) throws HibernateException {
		return sessionImplementor.scroll( query, queryParameters );
	}

	@Override
	public ScrollableResults scroll(CriteriaImpl criteria, ScrollMode scrollMode) {
		return sessionImplementor.scroll( criteria, scrollMode );
	}

	@Override
	public List list(CriteriaImpl criteria) {
		return sessionImplementor.list( criteria );
	}

	public List listFilter(Object collection, String filter, QueryParameters queryParameters)
			throws HibernateException {
		return sessionImplementor.listFilter( collection, filter, queryParameters );
	}

	public Iterator iterateFilter(Object collection, String filter, QueryParameters queryParameters)
			throws HibernateException {
		return sessionImplementor.iterateFilter( collection, filter, queryParameters );
	}

	public EntityPersister getEntityPersister(String entityName, Object object) throws HibernateException {
		return sessionImplementor.getEntityPersister( entityName, object );
	}

	public Object getEntityUsingInterceptor(EntityKey key) throws HibernateException {
		return sessionImplementor.getEntityUsingInterceptor( key );
	}

	public Serializable getContextEntityIdentifier(Object object) {
		return sessionImplementor.getContextEntityIdentifier( object );
	}

	public String bestGuessEntityName(Object object) {
		return sessionImplementor.bestGuessEntityName( object );
	}

	public String guessEntityName(Object entity) throws HibernateException {
		return sessionImplementor.guessEntityName( entity );
	}

	public Object instantiate(String entityName, Serializable id) throws HibernateException {
		return sessionImplementor.instantiate( entityName, id );
	}

	public List listCustomQuery(CustomQuery customQuery, QueryParameters queryParameters) throws HibernateException {
		return sessionImplementor.listCustomQuery( customQuery, queryParameters );
	}

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

	public Object getFilterParameterValue(String filterParameterName) {
		return sessionImplementor.getFilterParameterValue( filterParameterName );
	}

	public Type getFilterParameterType(String filterParameterName) {
		return sessionImplementor.getFilterParameterType( filterParameterName );
	}

	public Map getEnabledFilters() {
		return sessionImplementor.getEnabledFilters();
	}

	public int getDontFlushFromFind() {
		return sessionImplementor.getDontFlushFromFind();
	}

	public PersistenceContext getPersistenceContext() {
		return sessionImplementor.getPersistenceContext();
	}

	public int executeUpdate(String query, QueryParameters queryParameters) throws HibernateException {
		return sessionImplementor.executeUpdate( query, queryParameters );
	}

	@Override
	public int executeNativeUpdate(NativeSQLQuerySpecification specification, QueryParameters queryParameters)
			throws HibernateException {
		return sessionImplementor.executeNativeUpdate( specification, queryParameters );
	}

	public NonFlushedChanges getNonFlushedChanges() throws HibernateException {
		return sessionImplementor.getNonFlushedChanges();
	}

	public void applyNonFlushedChanges(NonFlushedChanges nonFlushedChanges) throws HibernateException {
		sessionImplementor.applyNonFlushedChanges( nonFlushedChanges );
	}

	public String getEntityName(Object object) throws HibernateException {
		return session.getEntityName( object );
	}

	public FlushMode getFlushMode() {
		return session.getFlushMode();
	}

	public Serializable getIdentifier(Object object) throws HibernateException {
		return session.getIdentifier( object );
	}

	public Query getNamedQuery(String queryName) throws HibernateException {
		return session.getNamedQuery( queryName );
	}

	public Query getNamedSQLQuery(String name) {
		return sessionImplementor.getNamedSQLQuery( name );
	}

	public boolean isEventSource() {
		return sessionImplementor.isEventSource();
	}

	public void afterScrollOperation() {
		sessionImplementor.afterScrollOperation();
	}

	public void setFetchProfile(String name) {
		sessionImplementor.setFetchProfile( name );
	}

	@Override
	public TransactionCoordinator getTransactionCoordinator() {
		return sessionImplementor.getTransactionCoordinator();
	}

	public String getFetchProfile() {
		return sessionImplementor.getFetchProfile();
	}

	public boolean isClosed() {
		return sessionImplementor.isClosed();
	}

	public LoadQueryInfluencers getLoadQueryInfluencers() {
		return sessionImplementor.getLoadQueryInfluencers();
	}

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
