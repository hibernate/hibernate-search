/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.genericjpa.impl;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.transaction.TransactionManager;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.search.Query;

import org.hibernate.search.engine.integration.impl.ExtendedSearchIntegrator;
import org.hibernate.search.engine.metadata.impl.MetadataProvider;
import org.hibernate.search.genericjpa.JPASearchFactoryController;
import org.hibernate.search.genericjpa.batchindexing.MassIndexer;
import org.hibernate.search.genericjpa.batchindexing.impl.MassIndexerImpl;
import org.hibernate.search.genericjpa.db.events.UpdateConsumer;
import org.hibernate.search.genericjpa.db.events.impl.AsyncUpdateSource;
import org.hibernate.search.genericjpa.db.events.index.impl.IndexUpdater;
import org.hibernate.search.genericjpa.entity.EntityManagerEntityProvider;
import org.hibernate.search.genericjpa.entity.EntityProvider;
import org.hibernate.search.genericjpa.entity.impl.BasicEntityProvider;
import org.hibernate.search.genericjpa.entity.impl.JPAReusableEntityProvider;
import org.hibernate.search.genericjpa.events.impl.SynchronizedUpdateSource;
import org.hibernate.search.genericjpa.exception.AssertionFailure;
import org.hibernate.search.genericjpa.exception.SearchException;
import org.hibernate.search.genericjpa.factory.StandaloneSearchConfiguration;
import org.hibernate.search.genericjpa.factory.StandaloneSearchFactory;
import org.hibernate.search.genericjpa.factory.impl.StandaloneSearchFactoryImpl;
import org.hibernate.search.genericjpa.jpa.util.impl.JPAEntityManagerFactoryWrapper;
import org.hibernate.search.genericjpa.metadata.impl.MetadataRehasher;
import org.hibernate.search.genericjpa.metadata.impl.MetadataUtil;
import org.hibernate.search.genericjpa.metadata.impl.RehashedTypeMetadata;
import org.hibernate.search.genericjpa.query.HSearchQuery;
import org.hibernate.search.genericjpa.transaction.TransactionContext;
import org.hibernate.search.indexes.IndexReaderAccessor;
import org.hibernate.search.jpa.FullTextEntityManager;
import org.hibernate.search.metadata.IndexedTypeDescriptor;
import org.hibernate.search.query.dsl.QueryContextBuilder;
import org.hibernate.search.spi.SearchIntegrator;
import org.hibernate.search.spi.SearchIntegratorBuilder;
import org.hibernate.search.stat.Statistics;

/**
 * Base class to create SearchFactories in a JPA environment.
 *
 * @author Martin Braun
 */
public final class JPASearchFactoryAdapter
		implements StandaloneSearchFactory, UpdateConsumer, JPASearchFactoryController {

	private static final Logger LOGGER = Logger.getLogger( JPASearchFactoryAdapter.class.getName() );

	private final Set<UpdateConsumer> updateConsumers = new HashSet<>();
	private final Lock lock = new ReentrantLock();

	private int updateDelay = 500;
	private int batchSizeForUpdates = 5;
	private AsyncUpdateSourceProvider asyncUpdateSourceProvider;
	private AsyncUpdateSource asyncUpdateSource;

	private SynchronizedUpdateSourceProvider synchronizedUpdateSourceProvider;
	private SynchronizedUpdateSource synchronizedUpdateSource;

	private IndexUpdater indexUpdater;
	private Map<Class<?>, EntityManagerEntityProvider> customUpdateEntityProviders;

	private String name;
	private EntityManagerFactory emf;
	private Properties properties;
	private List<Class<?>> indexRootTypes;
	private List<Class<?>> jpaRootTypes;
	private boolean useJTATransaction;
	private StandaloneSearchFactory searchFactory;
	private Set<Class<?>> indexRelevantEntities;
	private Map<Class<?>, String> idProperties;
	private Map<Class<?>, RehashedTypeMetadata> rehashedTypeMetadataForIndexRoot;
	private Map<Class<?>, List<Class<?>>> containedInIndexOf;
	private ExtendedSearchIntegrator searchIntegrator;

	private TransactionManager transactionManager;

	@Override
	public void updateEvent(List<UpdateEventInfo> updateInfo) {
		this.lock.lock();
		try {
			for ( UpdateConsumer updateConsumer : this.updateConsumers ) {
				try {
					updateConsumer.updateEvent( updateInfo );
				}
				catch (Exception e) {
					LOGGER.log( Level.WARNING, "Exception during notification of UpdateConsumers", e );
				}
			}
		}
		finally {
			this.lock.unlock();
		}
	}

	public StandaloneSearchFactory getSearchFactory() {
		return this.searchFactory;
	}

	public EntityProvider entityProvider(EntityManager em) {
		return new BasicEntityProvider( em, this.idProperties );
	}

	public final void init() {
		StandaloneSearchConfiguration config;
		if ( this.properties != null ) {
			LOGGER.info( "using config: " + this.properties );
			config = new StandaloneSearchConfiguration( this.properties );
		}
		else {
			config = new StandaloneSearchConfiguration();
		}

		MetadataProvider metadataProvider = MetadataUtil.getDummyMetadataProvider( config );
		MetadataRehasher rehasher = new MetadataRehasher();

		List<RehashedTypeMetadata> rehashedTypeMetadatas = new ArrayList<>();
		this.rehashedTypeMetadataForIndexRoot = new HashMap<>();
		for ( Class<?> indexRootType : this.getIndexRootTypes() ) {
			RehashedTypeMetadata rehashed = rehasher.rehash( metadataProvider.getTypeMetadataFor( indexRootType ) );
			rehashedTypeMetadatas.add( rehashed );
			rehashedTypeMetadataForIndexRoot.put( indexRootType, rehashed );
		}

		this.indexRelevantEntities = Collections.unmodifiableSet(
				MetadataUtil.calculateIndexRelevantEntities(
						rehashedTypeMetadatas
				)
		);
		this.idProperties = MetadataUtil.calculateIdProperties( rehashedTypeMetadatas );
		this.containedInIndexOf = MetadataUtil.calculateInIndexOf( rehashedTypeMetadatas );

		SearchIntegratorBuilder builder = new SearchIntegratorBuilder();
		this.indexRelevantEntities.forEach(
				config::addClass
		);
		// we have to build an integrator here (but we don't need it afterwards)
		builder.configuration( config ).buildSearchIntegrator();
		this.indexRelevantEntities.forEach(
				builder::addClass
		);
		SearchIntegrator impl = builder.buildSearchIntegrator();
		this.searchIntegrator = impl.unwrap( ExtendedSearchIntegrator.class );
		this.searchFactory = new StandaloneSearchFactoryImpl( this.searchIntegrator );

		JPAReusableEntityProvider entityProvider = new JPAReusableEntityProvider(
				this.emf,
				this.idProperties,
				this.transactionManager,
				this.customUpdateEntityProviders
		);

		this.asyncUpdateSource = this.asyncUpdateSourceProvider.getUpdateSource(
				this.updateDelay,
				TimeUnit.MILLISECONDS,
				this.batchSizeForUpdates,
				this.properties,
				new JPAEntityManagerFactoryWrapper( this.emf, this.transactionManager )
		);
		if ( this.asyncUpdateSource != null ) {
			this.indexUpdater = new IndexUpdater(
					this.rehashedTypeMetadataForIndexRoot, this.containedInIndexOf, entityProvider,
					impl.unwrap( ExtendedSearchIntegrator.class )
			);
			//TODO: we could allow this, but then we would need to change
			//the way we get the entityProvider. it's safest to keep it like this
			if ( this.emf == null ) {
				throw new AssertionFailure( "emf may not be null when using an AsyncUpdateSource" );
			}

			this.asyncUpdateSource.setUpdateConsumers(
					Arrays.asList(
							this.indexUpdater::updateEvent, this
					)
			);
			this.asyncUpdateSource.start();
		}
		this.synchronizedUpdateSource = this.synchronizedUpdateSourceProvider.getUpdateSource(
				impl.unwrap( ExtendedSearchIntegrator.class ),
				this.rehashedTypeMetadataForIndexRoot,
				this.containedInIndexOf,
				this.properties,
				this.emf,
				this.transactionManager,
				this.indexRelevantEntities
		);
		if ( this.synchronizedUpdateSource != null ) {
			if ( this.asyncUpdateSource != null ) {
				LOGGER.warning( "using both async updating AND synchronized updating, updates will get handled twice!" );
			}
			this.synchronizedUpdateSource.setUpdateConsumers( Collections.singletonList( this ) );
		}
	}

	public TransactionManager getTransactionManager() {
		return this.transactionManager;
	}

	public JPASearchFactoryAdapter setTransactionManager(TransactionManager transactionManager) {
		this.transactionManager = transactionManager;
		return this;
	}

	public String getName() {
		return this.name;
	}

	public JPASearchFactoryAdapter setName(String name) {
		this.name = name;
		return this;
	}

	public EntityManagerFactory getEmf() {
		return this.emf;
	}

	public JPASearchFactoryAdapter setEmf(EntityManagerFactory emf) {
		this.emf = emf;
		return this;
	}

	public Map<Class<?>, EntityManagerEntityProvider> getCustomUpdateEntityProviders() {
		return customUpdateEntityProviders;
	}

	public JPASearchFactoryAdapter setCustomUpdateEntityProviders(Map<Class<?>, EntityManagerEntityProvider> customUpdateEntityProviders) {
		this.customUpdateEntityProviders = customUpdateEntityProviders;
		return this;
	}

	public boolean isUseJTATransaction() {
		return this.useJTATransaction;
	}

	public JPASearchFactoryAdapter setUseJTATransaction(boolean useJTATransaction) {
		this.useJTATransaction = useJTATransaction;
		return this;
	}

	public List<Class<?>> getIndexRootTypes() {
		return this.indexRootTypes;
	}

	public JPASearchFactoryAdapter setIndexRootTypes(List<Class<?>> indexRootTypes) {
		this.indexRootTypes = indexRootTypes;
		return this;
	}

	public List<Class<?>> getJpaRootTypes() {
		return jpaRootTypes;
	}

	public JPASearchFactoryAdapter setJpaRootTypes(List<Class<?>> jpaRootTypes) {
		this.jpaRootTypes = jpaRootTypes;
		return this;
	}

	public Properties getProperties() {
		return this.properties;
	}

	public JPASearchFactoryAdapter setProperties(Map properties) {
		this.properties = new Properties();
		this.properties.putAll( properties );
		return this;
	}

	public AsyncUpdateSourceProvider getAsyncUpdateSourceProvider() {
		return this.asyncUpdateSourceProvider;
	}

	public JPASearchFactoryAdapter setAsyncUpdateSourceProvider(AsyncUpdateSourceProvider asyncUpdateSourceProvider) {
		this.asyncUpdateSourceProvider = asyncUpdateSourceProvider;
		return this;
	}

	public SynchronizedUpdateSourceProvider getSynchronizedUpdateSourceProvider() {
		return synchronizedUpdateSourceProvider;
	}

	public JPASearchFactoryAdapter setSynchronizedUpdateSourceProvider(SynchronizedUpdateSourceProvider synchronizedUpdateSourceProvider) {
		this.synchronizedUpdateSourceProvider = synchronizedUpdateSourceProvider;
		return this;
	}

	public int getBatchSizeForUpdates() {
		return this.batchSizeForUpdates;
	}

	public JPASearchFactoryAdapter setBatchSizeForUpdates(int batchSizeForUpdates) {
		this.batchSizeForUpdates = batchSizeForUpdates;
		return this;
	}

	public int getUpdateDelay() {
		return this.updateDelay;
	}

	public JPASearchFactoryAdapter setUpdateDelay(int updateDelay) {
		this.updateDelay = updateDelay;
		return this;
	}

	@Override
	public void pauseUpdating(boolean pause) {
		if ( this.asyncUpdateSource != null ) {
			this.asyncUpdateSource.pause( pause );
		}
	}

	@Override
	public FullTextEntityManager getFullTextEntityManager(EntityManager em) {
		// em may be null for when we don't need an EntityManager
		if ( em != null && em instanceof FullTextEntityManager ) {
			return (FullTextEntityManager) em;
		}
		else {
			return ImplementationFactory.createFullTextEntityManager( em, this );
		}
	}

	public MassIndexer createMassIndexer(List<Class<?>> indexRootTypes) {
		if ( this.emf == null ) {
			throw new SearchException( "can only create a MassIndexer with a JPA EntityManagerFactory present!" );
		}
		return new MassIndexerImpl( this.emf, this.searchIntegrator, indexRootTypes, this.transactionManager );
	}

	public MassIndexer createMassIndexer() {
		return this.createMassIndexer( this.jpaRootTypes );
	}

	public ExtendedSearchIntegrator getSearchIntegrator() {
		return this.searchIntegrator;
	}

	@Override
	public void index(Iterable<?> entities, TransactionContext tc) {
		this.searchFactory.index( entities, tc );
	}

	@Override
	public void close() {
		try {
			if ( this.asyncUpdateSource != null ) {
				this.asyncUpdateSource.stop();
			}
			if ( this.synchronizedUpdateSource != null ) {
				this.synchronizedUpdateSource.close();
			}
			if ( this.indexUpdater != null ) {
				this.indexUpdater.close();
			}
			this.searchFactory.close();
		}
		finally {
			SearchFactoryRegistry.unsetup( name, this );
		}
	}

	@Override
	public void update(Iterable<?> entities, TransactionContext tc) {
		this.searchFactory.update( entities, tc );
	}

	@Override
	public IndexReaderAccessor getIndexReaderAccessor() {
		return this.searchFactory.getIndexReaderAccessor();
	}

	@Override
	public QueryContextBuilder buildQueryBuilder() {
		return this.searchFactory.buildQueryBuilder();
	}

	@Override
	public void optimize() {
		this.searchFactory.optimize();
	}

	@Override
	public void optimize(Class<?> entity) {
		this.searchFactory.optimize( entity );
	}

	@Override
	public Statistics getStatistics() {
		return this.searchFactory.getStatistics();
	}

	@Override
	public void delete(Iterable<?> entities, TransactionContext tc) {
		this.searchFactory.delete( entities, tc );
	}

	@Override
	public void purgeAll(Class<?> entityClass) {
		this.searchFactory.purgeAll( entityClass );
	}

	@Override
	public Analyzer getAnalyzer(String name) {
		return this.searchFactory.getAnalyzer( name );
	}

	@Override
	public Analyzer getAnalyzer(Class<?> clazz) {
		return this.searchFactory.getAnalyzer( clazz );
	}

	@Override
	public void purgeAll(Class<?> entityClass, TransactionContext tc) {
		this.searchFactory.purgeAll( entityClass, tc );
	}

	@Override
	public HSearchQuery createQuery(Query query, Class<?>... targetedEntities) {
		return this.searchFactory.createQuery( query, targetedEntities );
	}

	@Override
	public void purge(Class<?> entityClass, Serializable id, TransactionContext tc) {
		this.searchFactory.purge( entityClass, id, tc );
	}

	@Override
	public void purge(Iterable<?> entities, TransactionContext tc) {
		this.searchFactory.purge( entities, tc );
	}

	@Override
	public void purgeByTerm(Class<?> entityClass, String field, Integer val, TransactionContext tc) {
		searchFactory.purgeByTerm( entityClass, field, val, tc );
	}

	@Override
	public void purgeByTerm(Class<?> entityClass, String field, Long val, TransactionContext tc) {
		searchFactory.purgeByTerm( entityClass, field, val, tc );
	}

	@Override
	public void purgeByTerm(Class<?> entityClass, String field, Float val, TransactionContext tc) {
		searchFactory.purgeByTerm( entityClass, field, val, tc );
	}

	@Override
	public void purgeByTerm(Class<?> entityClass, String field, Double val, TransactionContext tc) {
		searchFactory.purgeByTerm( entityClass, field, val, tc );
	}

	@Override
	public void purgeByTerm(Class<?> entityClass, String field, String val, TransactionContext tc) {
		searchFactory.purgeByTerm( entityClass, field, val, tc );
	}

	@Deprecated
	@Override
	public void purge(Class<?> entityClass, Query query, TransactionContext tc) {
		this.searchFactory.purge( entityClass, query, tc );
	}

	@Override
	public void flushToIndexes(TransactionContext tc) {
		this.searchFactory.flushToIndexes( tc );
	}

	@Override
	public IndexedTypeDescriptor getIndexedTypeDescriptor(Class<?> entityType) {
		return this.searchFactory.getIndexedTypeDescriptor( entityType );
	}

	@Override
	public Set<Class<?>> getIndexedTypes() {
		return this.searchFactory.getIndexedTypes();
	}

	@Override
	public <T> T unwrap(Class<T> cls) {
		return this.searchFactory.unwrap( cls );
	}

	@Override
	public void addUpdateConsumer(UpdateConsumer updateConsumer) {
		this.lock.lock();
		try {
			this.updateConsumers.add( updateConsumer );
		}
		finally {
			this.lock.unlock();
		}
	}

	@Override
	public void removeUpdateConsumer(UpdateConsumer updateConsumer) {
		this.lock.lock();
		try {
			this.updateConsumers.remove( updateConsumer );
		}
		finally {
			this.lock.unlock();
		}
	}

}
