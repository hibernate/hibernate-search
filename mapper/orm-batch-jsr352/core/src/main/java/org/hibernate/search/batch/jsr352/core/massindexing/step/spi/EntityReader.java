/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.batch.jsr352.core.massindexing.step.spi;

import static org.hibernate.search.batch.jsr352.core.massindexing.MassIndexingJobParameters.CACHE_MODE;
import static org.hibernate.search.batch.jsr352.core.massindexing.MassIndexingJobParameters.CHECKPOINT_INTERVAL;
import static org.hibernate.search.batch.jsr352.core.massindexing.MassIndexingJobParameters.ENTITY_FETCH_SIZE;
import static org.hibernate.search.batch.jsr352.core.massindexing.MassIndexingJobParameters.MAX_RESULTS_PER_ENTITY;
import static org.hibernate.search.batch.jsr352.core.massindexing.MassIndexingJobParameters.SESSION_CLEAR_INTERVAL;
import static org.hibernate.search.batch.jsr352.core.massindexing.util.impl.MassIndexingPartitionProperties.PARTITION_ID;

import java.io.IOException;
import java.io.Serializable;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;

import jakarta.batch.api.BatchProperty;
import jakarta.batch.api.chunk.AbstractItemReader;
import jakarta.batch.runtime.context.JobContext;
import jakarta.batch.runtime.context.StepContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.LockModeType;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import org.hibernate.CacheMode;
import org.hibernate.FlushMode;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.hibernate.search.batch.jsr352.core.context.jpa.spi.EntityManagerFactoryRegistry;
import org.hibernate.search.batch.jsr352.core.inject.scope.spi.HibernateSearchPartitionScoped;
import org.hibernate.search.batch.jsr352.core.logging.impl.Log;
import org.hibernate.search.batch.jsr352.core.massindexing.MassIndexingJobParameters;
import org.hibernate.search.batch.jsr352.core.massindexing.MassIndexingJobParameters.Defaults;
import org.hibernate.search.batch.jsr352.core.massindexing.impl.JobContextData;
import org.hibernate.search.batch.jsr352.core.massindexing.step.impl.HibernateSearchPartitionMapper;
import org.hibernate.search.batch.jsr352.core.massindexing.step.impl.IndexScope;
import org.hibernate.search.batch.jsr352.core.massindexing.step.impl.PartitionContextData;
import org.hibernate.search.batch.jsr352.core.massindexing.util.impl.EntityTypeDescriptor;
import org.hibernate.search.batch.jsr352.core.massindexing.util.impl.IdOrder;
import org.hibernate.search.batch.jsr352.core.massindexing.util.impl.JobContextUtil;
import org.hibernate.search.batch.jsr352.core.massindexing.util.impl.MassIndexingPartitionProperties;
import org.hibernate.search.batch.jsr352.core.massindexing.util.impl.PartitionBound;
import org.hibernate.search.batch.jsr352.core.massindexing.util.impl.PersistenceUtil;
import org.hibernate.search.batch.jsr352.core.massindexing.util.impl.SerializationUtil;
import org.hibernate.search.util.common.impl.Closer;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

/**
 * Entity reader reads entities from database. During the open of the read stream, this reader builds a scrollable
 * result. Then, it scrolls from one entity to another at each reading. An entity reader reaches its end when there’s no
 * more item to read. Each reader contains only one entity type.
 * <p>
 * The reading range is restricted by the {@link PartitionBound}, which always represents as a left-closed interval.
 * See {@link HibernateSearchPartitionMapper} for more information about these bounds.
 *
 * @author Mincong Huang
 */
// Same hack as in JobContextSetupListener.
@Named(value = "org.hibernate.search.jsr352.massindexing.impl.steps.lucene.EntityReader")
@HibernateSearchPartitionScoped
public class EntityReader extends AbstractItemReader {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	@Inject
	private JobContext jobContext;

	@Inject
	@BatchProperty(name = MassIndexingJobParameters.ENTITY_MANAGER_FACTORY_NAMESPACE)
	private String entityManagerFactoryNamespace;

	@Inject
	@BatchProperty(name = MassIndexingJobParameters.ENTITY_MANAGER_FACTORY_REFERENCE)
	private String entityManagerFactoryReference;

	@Inject
	@BatchProperty(name = MassIndexingJobParameters.ENTITY_TYPES)
	private String serializedEntityTypes;

	@Inject
	private EntityManagerFactoryRegistry emfRegistry;

	@Inject
	private StepContext stepContext;

	@Inject
	@BatchProperty(name = MassIndexingJobParameters.CACHE_MODE)
	private String serializedCacheMode;

	@Inject
	@BatchProperty(name = MassIndexingPartitionProperties.ENTITY_NAME)
	private String entityName;

	@Inject
	@BatchProperty(name = MassIndexingJobParameters.ENTITY_FETCH_SIZE)
	private String serializedEntityFetchSize;

	@Inject
	@BatchProperty(name = MassIndexingJobParameters.CHECKPOINT_INTERVAL)
	private String serializedCheckpointInterval;

	@Inject
	@BatchProperty(name = MassIndexingJobParameters.SESSION_CLEAR_INTERVAL)
	private String serializedSessionClearInterval;

	@Inject
	@BatchProperty(name = MassIndexingJobParameters.CUSTOM_QUERY_HQL)
	private String customQueryHql;

	@Inject
	@BatchProperty(name = MassIndexingJobParameters.MAX_RESULTS_PER_ENTITY)
	private String serializedMaxResultsPerEntity;

	@Inject
	@BatchProperty(name = MassIndexingJobParameters.TENANT_ID)
	private String tenantId;

	@Inject
	@BatchProperty(name = MassIndexingPartitionProperties.PARTITION_ID)
	private String serializedPartitionId;

	@Inject
	@BatchProperty(name = MassIndexingPartitionProperties.LOWER_BOUND)
	private String serializedLowerBound;

	@Inject
	@BatchProperty(name = MassIndexingPartitionProperties.UPPER_BOUND)
	private String serializedUpperBound;

	@Inject
	@BatchProperty(name = MassIndexingPartitionProperties.INDEX_SCOPE)
	private String indexScopeName;

	private JobContextData jobData;
	private EntityManagerFactory emf;

	private ChunkState chunkState;

	public EntityReader() {
	}

	public EntityReader(String serializedCacheMode,
			String entityName,
			String serializedEntityFetchSize,
			String serializedCheckpointInterval,
			String serializedSessionClearInterval,
			String hql,
			String serializedMaxResultsPerEntity,
			String partitionIdStr,
			String serializedLowerBound,
			String serializedUpperBound,
			String indexScopeName,
			JobContext jobContext,
			StepContext stepContext) {
		this.serializedCacheMode = serializedCacheMode;
		this.entityName = entityName;
		this.serializedEntityFetchSize = serializedEntityFetchSize;
		this.serializedCheckpointInterval = serializedCheckpointInterval;
		this.serializedSessionClearInterval = serializedSessionClearInterval;
		this.customQueryHql = hql;
		this.serializedMaxResultsPerEntity = serializedMaxResultsPerEntity;
		this.serializedPartitionId = partitionIdStr;
		this.serializedLowerBound = serializedLowerBound;
		this.serializedUpperBound = serializedUpperBound;
		this.indexScopeName = indexScopeName;
		this.jobContext = jobContext;
		this.stepContext = stepContext;
	}

	/**
	 * Initialize the environment. If checkpoint does not exist, then it should be the first open. If checkpoint exists,
	 * then it isn't the first open, re-use the input object "checkpoint" as the last ID already read.
	 *
	 * @param checkpointInfo The last checkpoint info persisted in the batch runtime, previously given by checkpointInfo().
	 * If this is the first start, then the checkpoint will be null.
	 */
	@Override
	public void open(Serializable checkpointInfo) throws IOException, ClassNotFoundException {
		log.openingReader( serializedPartitionId, entityName );

		final int partitionId = SerializationUtil.parseIntegerParameter( PARTITION_ID, serializedPartitionId );
		boolean isRestarted = checkpointInfo != null;

		jobData = getOrCreateJobContextData();

		emf = jobData.getEntityManagerFactory();

		PartitionContextData partitionData;
		IndexScope indexScope = IndexScope.valueOf( indexScopeName );
		CacheMode cacheMode = SerializationUtil.parseCacheModeParameter(
				CACHE_MODE, serializedCacheMode, Defaults.CACHE_MODE
		);
		int checkpointInterval = SerializationUtil.parseIntegerParameter(
				CHECKPOINT_INTERVAL, serializedCheckpointInterval
		);
		Integer sessionClearIntervalRaw = SerializationUtil.parseIntegerParameterOptional(
				SESSION_CLEAR_INTERVAL, serializedSessionClearInterval, null );
		int sessionClearInterval = Defaults.sessionClearInterval( sessionClearIntervalRaw, checkpointInterval );
		int entityFetchSize = SerializationUtil.parseIntegerParameterOptional(
				ENTITY_FETCH_SIZE, serializedEntityFetchSize, sessionClearInterval
		);
		Integer maxResults = SerializationUtil.parseIntegerParameterOptional(
				MAX_RESULTS_PER_ENTITY, serializedMaxResultsPerEntity, null
		);
		FetchingStrategy fetchingStrategy;
		switch ( indexScope ) {
			case HQL:
				fetchingStrategy = createHqlFetchingStrategy( cacheMode, entityFetchSize, maxResults );
				break;

			case FULL_ENTITY:
				fetchingStrategy = createCriteriaFetchingStrategy( cacheMode, entityFetchSize, maxResults );
				break;

			default:
				// This should never happen.
				throw new IllegalStateException( "Unknown value from enum: " + IndexScope.class );
		}

		chunkState = new ChunkState( emf, tenantId, fetchingStrategy, sessionClearInterval, checkpointInfo );

		if ( isRestarted ) {
			partitionData = (PartitionContextData) stepContext.getPersistentUserData();
		}
		else {
			partitionData = new PartitionContextData( partitionId, entityName );
		}

		stepContext.setTransientUserData( partitionData );
	}

	/**
	 * Close operation(s) before the class destruction.
	 */
	@Override
	public void close() {
		log.closingReader( serializedPartitionId, entityName );
		if ( chunkState != null ) {
			chunkState.close();
		}
		// reset the chunk work count to avoid over-count in item collector
		PartitionContextData partitionData = (PartitionContextData) stepContext.getTransientUserData();
		stepContext.setPersistentUserData( partitionData );
	}

	/**
	 * Read item from database using JPA. Each read, there will be only one entity fetched.
	 */
	@Override
	public Object readItem() {
		log.readingEntity();

		Object entity = chunkState.next();
		if ( entity == null ) {
			log.noMoreResults();
		}
		return entity;
	}

	/**
	 * The checkpointInfo method returns the current checkpoint data for this reader. It is called before a chunk
	 * checkpoint is committed.
	 *
	 * @return the checkpoint info
	 */
	@Override
	public Serializable checkpointInfo() {
		Serializable checkpointInfo = chunkState.end();
		log.checkpointReached( entityName, checkpointInfo );
		return checkpointInfo;
	}

	/*
	 * The spec states that a new job context is created for each partition,
	 * meaning that the entity reader may not be able to re-use the job context
	 * data set up in the JobContextSetupListener (actually we can, but only with JBeret).
	 * Thus we take care to re-create the data if necessary.
	 *
	 * See https://github.com/WASdev/standards.jsr352.jbatch/issues/50
	 */
	private JobContextData getOrCreateJobContextData() {
		return JobContextUtil.getOrCreateData(
				jobContext, emfRegistry, entityManagerFactoryNamespace, entityManagerFactoryReference,
				serializedEntityTypes
		);
	}

	/**
	 * Create a {@link FetchingStrategy} that creates scrolls from HQL
	 * and uses the last item's index in the result list as a checkpoint ID.
	 * <p>
	 * We use the item index as a checkpoint ID only because it's the only solution,
	 * given that we cannot modify the HQL to dynamically add a constraint based on the last
	 * treated element.
	 */
	private FetchingStrategy createHqlFetchingStrategy(
			CacheMode cacheMode, int entityFetchSize, Integer maxResults) {
		String hql = customQueryHql;

		return (session, lastCheckpointInfo) -> {
			Query<?> query = session.createQuery( hql, Object.class );

			if ( lastCheckpointInfo != null ) {
				query.setFirstResult( lastCheckpointInfo.getProcessedEntityCount() );
			}

			if ( maxResults != null ) {
				int remaining;
				if ( lastCheckpointInfo != null ) {
					remaining = maxResults - lastCheckpointInfo.getProcessedEntityCount();
				}
				else {
					remaining = maxResults;
				}
				query.setMaxResults( remaining );
			}

			return query.setReadOnly( true )
					.setCacheable( false )
					.setLockMode( LockModeType.NONE )
					.setHibernateFlushMode( FlushMode.MANUAL )
					.setCacheMode( cacheMode )
					.setFetchSize( entityFetchSize )
					// The FORWARD_ONLY mode is not enough for PostgreSQL when using setFirstResult
					.scroll( ScrollMode.SCROLL_SENSITIVE );
		};
	}

	/**
	 * Create a {@link FetchingStrategy} that creates scrolls from criteria
	 * and uses the last returned entity's ID as a checkpoint ID.
	 */
	private FetchingStrategy createCriteriaFetchingStrategy(
			CacheMode cacheMode, int entityFetchSize, Integer maxResults)
			throws IOException, ClassNotFoundException {
		Class<?> entityType = jobData.getEntityType( entityName );
		Object upperBound = SerializationUtil.deserialize( serializedUpperBound );
		Object lowerBound = SerializationUtil.deserialize( serializedLowerBound );

		EntityTypeDescriptor typeDescriptor = jobData.getEntityTypeDescriptor( entityType );
		IdOrder idOrder = typeDescriptor.getIdOrder();

		return (session, lastCheckpointInfo) -> {
			CriteriaBuilder builder = session.getCriteriaBuilder();
			CriteriaQuery<?> criteria = builder.createQuery( entityType );
			Root<?> root = criteria.from( entityType );

			// build orders for this entity
			idOrder.addAscOrder( builder, criteria, root );

			ArrayList<Predicate> predicates = new ArrayList<>( 2 );

			// build criteria using bounds
			if ( upperBound != null ) {
				predicates.add( idOrder.idLesser( builder, root, upperBound ) );
			}
			if ( lastCheckpointInfo != null ) {
				predicates.add( idOrder.idGreater( builder, root, lastCheckpointInfo.getLastProcessedEntityId() ) );
			}
			else if ( lowerBound != null ) {
				predicates.add( idOrder.idGreaterOrEqual( builder, root, lowerBound ) );
			}

			if ( !predicates.isEmpty() ) {
				criteria.where( predicates.toArray( new Predicate[predicates.size()] ) );
			}

			Query<?> query = session.createQuery( criteria );

			if ( maxResults != null ) {
				int remaining;
				if ( lastCheckpointInfo != null ) {
					remaining = maxResults - lastCheckpointInfo.getProcessedEntityCount();
				}
				else {
					remaining = maxResults;
				}
				query.setMaxResults( remaining );
			}

			return query
					.setReadOnly( true )
					.setCacheable( false )
					.setLockMode( LockModeType.NONE )
					.setCacheMode( cacheMode )
					.setHibernateFlushMode( FlushMode.MANUAL )
					.setFetchSize( entityFetchSize )
					.scroll( ScrollMode.FORWARD_ONLY );
		};
	}

	private interface FetchingStrategy {

		ScrollableResults<?> createScroll(Session session, CheckpointInfo lastCheckpointInfo);

	}

	private static class ChunkState implements AutoCloseable {
		private final EntityManagerFactory emf;
		private final String tenantId;
		private final FetchingStrategy fetchingStrategy;
		private final int clearInterval;

		private Session session;
		private ScrollableResults<?> scroll;

		private CheckpointInfo lastCheckpointInfo;
		private int processedEntityCount = 0;
		private Object lastProcessedEntityId;

		public ChunkState(
				EntityManagerFactory emf, String tenantId, FetchingStrategy fetchingStrategy, int clearInterval,
				Serializable checkpointInfo) {
			this.emf = emf;
			this.tenantId = tenantId;
			this.fetchingStrategy = fetchingStrategy;
			this.clearInterval = clearInterval;
			this.lastCheckpointInfo = (CheckpointInfo) checkpointInfo;
		}

		/**
		 * Get the next element for the current chunk.
		 * @return The next element for this chunk.
		 */
		public Object next() {
			if ( scroll == null ) {
				start();
			}
			// Mind the "else": we don't clear a session we just created.
			else if ( processedEntityCount % clearInterval == 0 ) {
				/*
				 * This must be executed before we extract the entity,
				 * because the returned entity must be attached to the session.
				 */
				session.clear();
			}
			if ( !scroll.next() ) {
				return null;
			}
			Object entity = scroll.get();
			lastProcessedEntityId = session.getIdentifier( entity );
			++processedEntityCount;
			return entity;
		}

		/**
		 * End a chunk.
		 * @return The checkpoint info for the chunk that just ended.
		 */
		public Serializable end() {
			close();
			int processedEntityCountInPartition = processedEntityCount;
			if ( lastCheckpointInfo != null ) {
				processedEntityCountInPartition += lastCheckpointInfo.getProcessedEntityCount();
			}
			Object lastProcessedEntityIdInPartition = lastProcessedEntityId;
			if ( lastCheckpointInfo != null && lastProcessedEntityIdInPartition == null ) {
				lastProcessedEntityIdInPartition = lastCheckpointInfo.getLastProcessedEntityId();
			}
			processedEntityCount = 0;
			lastProcessedEntityId = null;
			lastCheckpointInfo = new CheckpointInfo( (Serializable) lastProcessedEntityIdInPartition, processedEntityCountInPartition );
			return lastCheckpointInfo;
		}

		@Override
		public void close() {
			try ( Closer<RuntimeException> closer = new Closer<>() ) {
				if ( scroll != null ) {
					closer.push( ScrollableResults::close, scroll );
					scroll = null;
				}
				if ( session != null ) {
					closer.push( Session::close, session );
					session = null;
				}
			}
		}

		private void start() {
			session = PersistenceUtil.openSession( emf, tenantId );
			try {
				scroll = fetchingStrategy.createScroll( session, lastCheckpointInfo );
			}
			catch (Throwable t) {
				try {
					session.close();
				}
				catch (Throwable t2) {
					t.addSuppressed( t2 );
				}
				throw t;
			}
		}

	}

	private static class CheckpointInfo implements Serializable {
		private final Serializable lastProcessedEntityId;
		private final int processedEntityCount;

		public CheckpointInfo(Serializable lastProcessedEntityId, int processedEntityCount) {
			this.lastProcessedEntityId = lastProcessedEntityId;
			this.processedEntityCount = processedEntityCount;
		}

		public Serializable getLastProcessedEntityId() {
			return lastProcessedEntityId;
		}

		public int getProcessedEntityCount() {
			return processedEntityCount;
		}

		@Override
		public String toString() {
			return new StringBuilder( "[" )
					.append( "lastProcessedEntityId = " ).append( lastProcessedEntityId )
					.append( ", processedEntityCount = " ).append( processedEntityCount )
					.append( "]" )
					.toString();
		}
	}

}
