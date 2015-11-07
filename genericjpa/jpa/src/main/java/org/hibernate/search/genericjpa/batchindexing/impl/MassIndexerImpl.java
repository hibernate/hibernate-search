/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.genericjpa.batchindexing.impl;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.metamodel.EntityType;
import javax.persistence.metamodel.Metamodel;
import javax.persistence.metamodel.SingularAttribute;
import javax.transaction.TransactionManager;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hibernate.search.backend.OptimizeLuceneWork;
import org.hibernate.search.backend.PurgeAllLuceneWork;
import org.hibernate.search.backend.impl.batch.DefaultBatchBackend;
import org.hibernate.search.backend.spi.BatchBackend;
import org.hibernate.search.engine.integration.impl.ExtendedSearchIntegrator;
import org.hibernate.search.genericjpa.batchindexing.MassIndexer;
import org.hibernate.search.genericjpa.batchindexing.MassIndexerProgressMonitor;
import org.hibernate.search.genericjpa.db.events.UpdateConsumer;
import org.hibernate.search.genericjpa.entity.impl.BasicEntityProvider;
import org.hibernate.search.genericjpa.entity.EntityProvider;
import org.hibernate.search.genericjpa.entity.impl.TransactionWrappedEntityManagerEntityProvider;
import org.hibernate.search.genericjpa.exception.AssertionFailure;
import org.hibernate.search.genericjpa.exception.SearchException;
import org.hibernate.search.genericjpa.util.NamingThreadFactory;

/**
 * @author Martin Braun
 */
public class MassIndexerImpl implements MassIndexer {

	private static final Logger LOGGER = Logger.getLogger( MassIndexerImpl.class.getName() );
	private final ExtendedSearchIntegrator searchIntegrator;
	private final List<Class<?>> rootTypes;
	private final TransactionManager transactionManager;
	private final EntityManagerFactory emf;
	private final ConcurrentHashMap<Class<?>, AtomicInteger> idProgress = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<Class<?>, AtomicInteger> objectLoadedProgress = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<Class<?>, AtomicInteger> documentBuiltProgress = new ConcurrentHashMap<>();
	private final AtomicInteger documentsAdded = new AtomicInteger();
	/**
	 * used to wait for finishing the indexing process
	 */
	private final Map<Class<?>, NumberCondition> finishConditions = new HashMap<>();
	private final ConcurrentLinkedQueue<Future<?>> idProducerFutures = new ConcurrentLinkedQueue<>();
	/**
	 * this latch is used to wait for the cleanup thread to finish.
	 */
	private final CountDownLatch cleanUpLatch = new CountDownLatch( 1 );
	/**
	 * lock to guard the cancelled variable. the cancel method of our future has the write lock while all the others are
	 * "readers". -> cancel is is more important.
	 */
	private final ReadWriteLock cancelGuard = new ReentrantReadWriteLock();
	private BatchBackend batchBackend;
	private ExecutorService executorServiceForIds;
	private ExecutorService executorServiceForObjects;
	private boolean purgeAllOnStart = true;
	private boolean optimizeAfterPurge = true;
	private boolean optimizeOnFinish = true;
	private int batchSizeToLoadIds = 100;
	private int batchSizeToLoadObjects = 10;
	private int threadsToLoadIds = 1;
	private int threadsToLoadObjects = 4;
	private Integer idProducerTransactionTimeout = null;
	private boolean started = false;
	private Map<Class<?>, String> idProperties;
	private Future<Void> future;
	private ConcurrentLinkedQueue<BasicEntityProvider> freeEntityProviders = new ConcurrentLinkedQueue<>();
	private ConcurrentLinkedQueue<BasicEntityProvider> entityProviders = new ConcurrentLinkedQueue<>();
	private MassIndexerProgressMonitor progressMonitor;
	/**
	 * this is needed so we don't flood the executors for object handling. we store the amount of currently submitted
	 * ObjectHandlerTasks in here
	 */
	private NumberCondition objectHandlerTaskCondition;
	private boolean cancelled = false;

	private EntityProvider userSpecifiedEntityProvider;

	public MassIndexerImpl(
			EntityManagerFactory emf,
			ExtendedSearchIntegrator searchIntegrator,
			List<Class<?>> rootTypes,
			TransactionManager transactionManager) {
		this.emf = emf;
		this.searchIntegrator = searchIntegrator;
		this.rootTypes = rootTypes;
		this.transactionManager = transactionManager;
	}

	@Override
	public MassIndexer purgeAllOnStart(boolean purgeAllOnStart) {
		this.purgeAllOnStart = purgeAllOnStart;
		return this;
	}

	@Override
	public MassIndexer optimizeAfterPurge(boolean optimizeAfterPurge) {
		this.optimizeAfterPurge = optimizeAfterPurge;
		return this;
	}

	@Override
	public MassIndexer optimizeOnFinish(boolean optimizeOnFinish) {
		this.optimizeOnFinish = optimizeOnFinish;
		return this;
	}

	@Override
	public MassIndexer batchSizeToLoadIds(int batchSizeToLoadIds) {
		if ( batchSizeToLoadIds <= 0 ) {
			throw new IllegalArgumentException( "batchSizeToLoadIds must be greater or equal to 1!" );
		}
		this.batchSizeToLoadIds = batchSizeToLoadIds;
		return this;
	}

	@Override
	public MassIndexer batchSizeToLoadObjects(int batchSizeToLoadObjects) {
		if ( batchSizeToLoadObjects <= 0 ) {
			throw new IllegalArgumentException( "batchSizeToLoadObjects must be greater or equal to 1!" );
		}
		this.batchSizeToLoadObjects = batchSizeToLoadObjects;
		return this;
	}

	@Override
	public MassIndexer threadsToLoadIds(int threadsToLoadIds) {
		if ( threadsToLoadIds <= 0 ) {
			throw new IllegalArgumentException( "threadsToLoadIds must be greater or equal to 1" );
		}
		this.threadsToLoadIds = threadsToLoadIds;
		return this;
	}

	@Override
	public MassIndexer threadsToLoadObjects(int threadsToLoadObjects) {
		if ( threadsToLoadObjects <= 0 ) {
			throw new IllegalArgumentException( "threadsToLoadObjects must be greater or equal to 1" );
		}
		this.threadsToLoadObjects = threadsToLoadObjects;
		return this;
	}

	@Override
	public Future<?> start() {
		if ( this.started ) {
			throw new AssertionFailure( "already started this instance of MassIndexer once!" );
		}
		this.started = true;

		this.setupBatchBackend();

		this.executorServiceForIds = Executors.newFixedThreadPool(
				this.threadsToLoadIds, new NamingThreadFactory(
						"MassIndexer Id Loader Thread"
				)
		);
		this.executorServiceForObjects = Executors
				.newFixedThreadPool(
						this.threadsToLoadObjects, new NamingThreadFactory(
								"MassIndexer Object Loader Thread"
						)
				);

		this.objectHandlerTaskCondition = new NumberCondition( this.threadsToLoadObjects * 4 );

		this.idProperties = this.getIdProperties( this.rootTypes );

		// start all the IdProducers
		this.startIdProducers();

		// create the future object
		this.future = this.getFuture();

		// start the cleanup thread needed to do all the jobs required after the batch indexing is done
		this.startCleanUpThread();
		return this.future;
	}

	@Override
	public void startAndWait() throws InterruptedException {
		try {
			this.start().get();
		}
		catch (ExecutionException e) {
			throw new SearchException( e );
		}
	}

	public void updateEvent(List<UpdateConsumer.UpdateEventInfo> updateInfo) {
		try {
			// check if we should wait with submitting
			this.objectHandlerTaskCondition.check();
		}
		catch (InterruptedException e) {
			//throw this forward. this should be catched by the IdProducer
			throw new RuntimeException( e );
		}
		Lock lock = MassIndexerImpl.this.cancelGuard.readLock();
		lock.lock();
		try {
			if ( this.cancelled ) {
				return;
			}
			Class<?> entityClass = updateInfo.get( 0 ).getEntityClass();
			ObjectHandlerTask task = new ObjectHandlerTask(
					this.batchBackend,
					entityClass,
					this.searchIntegrator.getIndexBinding( entityClass ),
					this::getEntityProvider,
					this::disposeEntityManager,
					this.emf.getPersistenceUnitUtil(),
					this.finishConditions.get( entityClass ),
					this::onException
			);
			task.batch( updateInfo );
			task.documentBuiltProgressMonitor( this::documentBuiltProgress );
			task.objectLoadedProgressMonitor( this::objectLoadedProgress );
			this.objectHandlerTaskCondition.up( 1 );
			this.executorServiceForObjects.submit( task );
		}
		finally {
			lock.unlock();
		}
	}

	@Override
	public MassIndexer progressMonitor(MassIndexerProgressMonitor progressMonitor) {
		this.progressMonitor = progressMonitor;
		return this;
	}

	@Override
	public MassIndexer entityProvider(EntityProvider entityProvider) {
		this.userSpecifiedEntityProvider = entityProvider;
		return this;
	}

	@Override
	public MassIndexer idProducerTransactionTimeout(int seconds) {
		if ( seconds <= 0 ) {
			throw new IllegalArgumentException( "idProducerTransactionTimeout must be greater than 0" );
		}
		this.idProducerTransactionTimeout = seconds;
		return this;
	}

	private void startIdProducers() {
		for ( Class<?> rootClass : this.rootTypes ) {
			try {
				if ( this.purgeAllOnStart ) {
					this.batchBackend.enqueueAsyncWork( new PurgeAllLuceneWork( rootClass ) );
					if ( this.optimizeAfterPurge ) {
						this.batchBackend.enqueueAsyncWork( new OptimizeLuceneWork( rootClass ) );
					}
					this.batchBackend.flush( new HashSet<>( this.rootTypes ) );
				}
			}
			catch (Exception e) {
				throw new SearchException( e );
			}

			this.finishConditions.put( rootClass, new NumberCondition( 0, 0, false ) );
			IdProducerTask idProducer = new IdProducerTask(
					rootClass,
					this.idProperties.get( rootClass ),
					this.emf,
					this.transactionManager,
					this.batchSizeToLoadIds,
					this.batchSizeToLoadObjects,
					this::updateEvent,
					this::onException,
					this.finishConditions.get( rootClass )
			);
			idProducer.progressMonitor( this::idProgress );
			idProducer.transactionTimeout( this.idProducerTransactionTimeout );
			this.idProducerFutures.add( this.executorServiceForIds.submit( idProducer ) );
		}
	}

	private void setupBatchBackend() {
		this.batchBackend = new DefaultBatchBackend(
				this.searchIntegrator, new org.hibernate.search.batchindexing.MassIndexerProgressMonitor() {

			@Override
			public void documentsAdded(long increment) {
				// hacky: whatever...
				int count = MassIndexerImpl.this.documentsAdded.addAndGet( (int) increment );
				if ( MassIndexerImpl.this.progressMonitor != null ) {
					MassIndexerImpl.this.progressMonitor.documentsAdded( count );
				}
			}

			@Override
			public void indexingCompleted() {

			}

			@Override
			public void entitiesLoaded(int size) {

			}

			@Override
			public void documentsBuilt(int number) {

			}

			@Override
			public void addToTotalCount(long count) {

			}

		}
		);
	}

	private void startCleanUpThread() {
		new Thread( "MassIndexer Cleanup/Finisher Thread" ) {

			@Override
			public void run() {
				try {
					MassIndexerImpl.this.awaitJobsFinish();
					if ( MassIndexerImpl.this.optimizeOnFinish ) {
						LOGGER.info( "optimizing on finish" );
						for ( Class<?> rootEntity : MassIndexerImpl.this.rootTypes ) {
							try {
								MassIndexerImpl.this.batchBackend.enqueueAsyncWork(
										new OptimizeLuceneWork(
												rootEntity
										)
								);
							}
							catch (InterruptedException e) {
								LOGGER.log( Level.WARNING, "interrupted while optimizing on finish!", e );
								throw e;
							}
						}
					}
				}
				catch (InterruptedException e) {
					throw new SearchException( "Error during massindexing!", e );
				}
				finally {
					// flush all the works that are left in the queue EVEN if we get interrupted
					MassIndexerImpl.this.batchBackend.flush( new HashSet<>( MassIndexerImpl.this.rootTypes ) );

					//we also have to finish up
					MassIndexerImpl.this.closeExecutorServices();
					MassIndexerImpl.this.closeAllOpenEntityManagers();
					MassIndexerImpl.this.cleanUpLatch.countDown();
				}
			}

		}.start();
	}

	private Future<Void> getFuture() {
		return new Future<Void>() {

			@Override
			public boolean cancel(boolean mayInterruptIfRunning) {
				boolean ret = false;

				ret |= MassIndexerImpl.this.cancelIdProducers( mayInterruptIfRunning );

				MassIndexerImpl.this.setCancelled();

				// FIXME: wait for all the running threads to finish up.
				// add this logic here, but don't add it in the onException method
				// or this will result in a deadlock

				MassIndexerImpl.this.disableFinishConditions();

				// but we have to wait for the cleanup thread to finish up
				try {
					MassIndexerImpl.this.cleanUpLatch.await();
				}
				catch (InterruptedException e) {
					throw new SearchException( "couldn't wait for optimizeOnFinish", e );
				}
				return ret;

			}

			@Override
			public boolean isCancelled() {
				boolean ret = false;
				for ( Future<?> future : MassIndexerImpl.this.idProducerFutures ) {
					ret |= future.isCancelled();
				}
				return ret;
			}

			@Override
			public boolean isDone() {
				boolean ret = false;
				for ( Future<?> future : MassIndexerImpl.this.idProducerFutures ) {
					ret |= future.isDone();
				}
				return ret || this.isCancelled() || MassIndexerImpl.this.isFinished();
			}

			@Override
			public Void get() throws InterruptedException, ExecutionException {
				MassIndexerImpl.this.awaitJobsFinish();
				MassIndexerImpl.this.cleanUpLatch.await();
				return null;
			}

			@Override
			public Void get(long timeout, TimeUnit unit)
					throws InterruptedException, ExecutionException, TimeoutException {
				for ( NumberCondition condition : MassIndexerImpl.this.finishConditions.values() ) {
					// FIXME: not quite right...
					if ( !condition.check( timeout, unit ) ) {
						throw new TimeoutException();
					}
				}
				// FIXME: not quite right...
				MassIndexerImpl.this.cleanUpLatch.await( timeout, unit );
				return null;
			}

		};
	}

	private void awaitJobsFinish() throws InterruptedException {
		for ( NumberCondition condition : MassIndexerImpl.this.finishConditions.values() ) {
			condition.check();
		}
	}

	private void idProgress(Class<?> entityType, Integer count) {
		int newCount = this.idProgress.computeIfAbsent(
				entityType, (type) -> new AtomicInteger( 0 )
		).addAndGet( count );
		if ( this.progressMonitor != null ) {
			this.progressMonitor.idsLoaded( entityType, newCount );
		}
	}

	private void objectLoadedProgress(Class<?> entityType, Integer count) {
		int newCount = this.objectLoadedProgress.computeIfAbsent(
				entityType, (type) -> new AtomicInteger( 0 )
		).addAndGet( count );
		if ( this.progressMonitor != null ) {
			this.progressMonitor.objectsLoaded( entityType, newCount );
		}
	}

	private void documentBuiltProgress(Class<?> entityType, Integer count) {
		int newCount = this.documentBuiltProgress.computeIfAbsent(
				entityType, (type) -> new AtomicInteger( 0 )
		).addAndGet( count );
		if ( this.progressMonitor != null ) {
			this.progressMonitor.documentsBuilt( entityType, newCount );
		}
	}

	private boolean isFinished() {
		boolean ret = true;
		for ( NumberCondition numberCondition : this.finishConditions.values() ) {
			try {
				ret &= numberCondition.check( 1, TimeUnit.NANOSECONDS );
			}
			catch (InterruptedException e) {
				throw new SearchException( e );
			}
		}
		return ret;
	}

	private EntityProvider getEntityProvider() {
		if ( this.userSpecifiedEntityProvider == null ) {
			BasicEntityProvider emProvider = this.freeEntityProviders.poll();
			if ( emProvider == null ) {
				EntityManager em = this.emf.createEntityManager();
				try {
					emProvider = new TransactionWrappedEntityManagerEntityProvider(
							em,
							this.idProperties,
							this.transactionManager
					);
				}
				catch (Exception e) {
					em.close();
					throw e;
				}
				this.entityProviders.add( emProvider );
			}
			return emProvider;
		} else {
			return this.userSpecifiedEntityProvider;
		}
	}

	private void disposeEntityManager(ObjectHandlerTask task, EntityProvider provider) {
		if ( this.userSpecifiedEntityProvider == null ) {
			((TransactionWrappedEntityManagerEntityProvider) provider).clearEm();
			this.freeEntityProviders.add( (BasicEntityProvider) provider );
		}
		this.objectHandlerTaskCondition.down( 1 );
	}


	private boolean cancelIdProducers(boolean mayInterruptIfRunning) {
		boolean ret = false;
		for ( Future<?> future : MassIndexerImpl.this.idProducerFutures ) {
			ret |= future.cancel( mayInterruptIfRunning );
		}

		MassIndexerImpl.this.objectHandlerTaskCondition.disable();
		return ret;
	}

	/**
	 * careful, this is used in ObjectHandlerTasks as well, don't produce deadlocks!
	 */
	private void disableFinishConditions() {
		// blow the signal to stop everything
		MassIndexerImpl.this.finishConditions.values()
				.forEach( NumberCondition::disable );
	}

	private void setCancelled() {
		Lock lock = MassIndexerImpl.this.cancelGuard.writeLock();
		lock.lock();
		try {
			MassIndexerImpl.this.cancelled = true;
		}
		finally {
			lock.unlock();
		}
	}

	/**
	 * this method is called whenever an Exception occurs in either the IdProducerTask or the ObjectHandlerTask
	 */
	private void onException(Exception e) {
		LOGGER.log( Level.WARNING, "Exception during indexing", e );
		//this doesn't wait for anything, so we can call this even if we are coming from a IdProducer-Thread
		this.cancelIdProducers( true );
		//we should stop producing new ObjectHandlerTasks
		this.setCancelled();
		//welp, since the ObjectHandler Threads are borked anyways we can just disable all waiting for them.
		this.disableFinishConditions();
	}

	private void closeExecutorServices() {
		this.executorServiceForIds.shutdown();
		this.executorServiceForObjects.shutdown();
	}

	private void closeAllOpenEntityManagers() {
		while ( this.entityProviders.size() > 0 ) {
			try {
				this.entityProviders.remove().close();
			}
			catch (IOException e) {
				LOGGER.log( Level.WARNING, "Exception while closing EntityManagers", e );
			}
		}
	}

	private Map<Class<?>, String> getIdProperties(List<Class<?>> entityClasses) {
		Map<Class<?>, String> ret = new HashMap<>( entityClasses.size() );
		for ( Class<?> entityClass : entityClasses ) {
			ret.put( entityClass, this.getIdProperty( entityClass ) );
		}
		return ret;
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	private String getIdProperty(Class<?> entityClass) {
		String idProperty = null;
		Metamodel metamodel = this.emf.getMetamodel();
		EntityType entity = metamodel.entity( entityClass );
		Set<SingularAttribute> singularAttributes = entity.getSingularAttributes();
		for ( SingularAttribute singularAttribute : singularAttributes ) {
			if ( singularAttribute.isId() ) {
				idProperty = singularAttribute.getName();
				break;
			}
		}
		if ( idProperty == null ) {
			throw new SearchException( "id field not found for: " + entityClass );
		}
		return idProperty;
	}

}
