/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.jakarta.batch.core.massindexing.step.spi;

import java.io.IOException;
import java.io.Serializable;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;

import jakarta.batch.api.BatchProperty;
import jakarta.batch.api.chunk.AbstractItemReader;
import jakarta.batch.runtime.context.JobContext;
import jakarta.batch.runtime.context.StepContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.LockModeType;

import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.StatelessSession;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.query.SelectionQuery;
import org.hibernate.search.jakarta.batch.core.context.jpa.spi.EntityManagerFactoryRegistry;
import org.hibernate.search.jakarta.batch.core.inject.scope.spi.HibernateSearchPartitionScoped;
import org.hibernate.search.jakarta.batch.core.logging.impl.Log;
import org.hibernate.search.jakarta.batch.core.massindexing.MassIndexingJobParameters;
import org.hibernate.search.jakarta.batch.core.massindexing.impl.JobContextData;
import org.hibernate.search.jakarta.batch.core.massindexing.step.impl.HibernateSearchPartitionMapper;
import org.hibernate.search.jakarta.batch.core.massindexing.step.impl.PartitionContextData;
import org.hibernate.search.jakarta.batch.core.massindexing.util.impl.EntityTypeDescriptor;
import org.hibernate.search.jakarta.batch.core.massindexing.util.impl.JobContextUtil;
import org.hibernate.search.jakarta.batch.core.massindexing.util.impl.MassIndexingPartitionProperties;
import org.hibernate.search.jakarta.batch.core.massindexing.util.impl.PartitionBound;
import org.hibernate.search.jakarta.batch.core.massindexing.util.impl.PersistenceUtil;
import org.hibernate.search.jakarta.batch.core.massindexing.util.impl.SerializationUtil;
import org.hibernate.search.mapper.orm.loading.spi.ConditionalExpression;
import org.hibernate.search.util.common.impl.Closer;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

/**
 * Reads entity identifiers from the database.
 * <p>
 * This reader builds a scroll and outputs IDs from that scroll.
 * <p>
 * Each reader pertains to only one entity type.
 * <p>
 * The reading range is restricted by the {@link PartitionBound}, which always represents a left-closed interval.
 * See {@link HibernateSearchPartitionMapper} for more information about these bounds.
 *
 * @author Mincong Huang
 */
// Same hack as in JobContextSetupListener.
@Named(value = "org.hibernate.search.jsr352.massindexing.impl.steps.lucene.EntityReader")
@HibernateSearchPartitionScoped
public class EntityIdReader extends AbstractItemReader {

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
	@BatchProperty(name = MassIndexingPartitionProperties.ENTITY_NAME)
	private String entityName;

	@Inject
	@BatchProperty(name = MassIndexingJobParameters.ID_FETCH_SIZE)
	private String serializedIdFetchSize;

	@Inject
	@BatchProperty(name = MassIndexingJobParameters.REINDEX_ONLY_HQL)
	private String reindexOnlyHql;

	@Inject
	@BatchProperty(name = MassIndexingJobParameters.REINDEX_ONLY_PARAMETERS)
	private String serializedReindexOnlyParameters;

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

	private EntityManagerFactory emf;
	private EntityTypeDescriptor<?, ?> type;

	private int idFetchSize;
	private Integer maxResults;
	private ConditionalExpression reindexOnly;
	private Object upperBound;
	private Object lowerBound;

	private ChunkState chunkState;

	public EntityIdReader() {
	}

	public EntityIdReader(String entityName,
			String serializedIdFetchSize,
			String reindexOnlyHql,
			String serializedReindexOnlyParameters,
			String serializedMaxResultsPerEntity,
			String partitionIdStr,
			String serializedLowerBound,
			String serializedUpperBound,
			JobContext jobContext,
			StepContext stepContext) {
		this.entityName = entityName;
		this.serializedIdFetchSize = serializedIdFetchSize;
		this.reindexOnlyHql = reindexOnlyHql;
		this.serializedReindexOnlyParameters = serializedReindexOnlyParameters;
		this.serializedMaxResultsPerEntity = serializedMaxResultsPerEntity;
		this.serializedPartitionId = partitionIdStr;
		this.serializedLowerBound = serializedLowerBound;
		this.serializedUpperBound = serializedUpperBound;
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

		JobContextData jobData = getOrCreateJobContextData();

		emf = jobData.getEntityManagerFactory();
		type = jobData.getEntityTypeDescriptor( entityName );

		idFetchSize = SerializationUtil.parseIntegerParameterOptional(
				MassIndexingJobParameters.ID_FETCH_SIZE, serializedIdFetchSize,
				MassIndexingJobParameters.Defaults.ID_FETCH_SIZE
		);
		reindexOnly = SerializationUtil.parseReindexOnlyParameters( reindexOnlyHql, serializedReindexOnlyParameters );
		maxResults = SerializationUtil.parseIntegerParameterOptional(
				MassIndexingJobParameters.MAX_RESULTS_PER_ENTITY, serializedMaxResultsPerEntity, null
		);

		upperBound = SerializationUtil.deserialize( serializedUpperBound );
		lowerBound = SerializationUtil.deserialize( serializedLowerBound );

		chunkState = new ChunkState( checkpointInfo );

		PartitionContextData partitionData;
		boolean isRestarted = checkpointInfo != null;
		if ( isRestarted ) {
			partitionData = (PartitionContextData) stepContext.getPersistentUserData();
		}
		else {
			final int partitionId =
					SerializationUtil.parseIntegerParameter( MassIndexingPartitionProperties.PARTITION_ID,
							serializedPartitionId );
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
	 * Read item from database using JPA. Each read, there will be only one identifier fetched.
	 */
	@Override
	public Object readItem() {
		log.readingEntityId();

		Object id = chunkState.next();
		if ( id == null ) {
			log.noMoreResults();
		}
		return id;
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

	private class ChunkState implements AutoCloseable {
		private StatelessSession session;
		private ScrollableResults<?> scroll;

		private CheckpointInfo lastCheckpointInfo;
		private int processedEntityCount = 0;
		private Object lastProcessedEntityId;

		public ChunkState(Serializable checkpointInfo) {
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
			if ( !scroll.next() ) {
				return null;
			}
			Object id = scroll.get();
			lastProcessedEntityId = id;
			++processedEntityCount;
			return id;
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
			lastCheckpointInfo = new CheckpointInfo(
					(Serializable) lastProcessedEntityIdInPartition,
					processedEntityCountInPartition
			);
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
					closer.push( StatelessSession::close, session );
					session = null;
				}
			}
		}

		private void start() {
			session = PersistenceUtil.openStatelessSession( emf, tenantId );
			try {
				scroll = createScroll( type, session );
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

		private <E, I> ScrollableResults<I> createScroll(EntityTypeDescriptor<E, I> type, StatelessSession session) {
			List<ConditionalExpression> conditions = new ArrayList<>();
			if ( reindexOnly != null ) {
				conditions.add( reindexOnly );
			}
			if ( upperBound != null ) {
				conditions.add( type.idOrder().idLesser( "HIBERNATE_SEARCH_PARTITION_UPPER_BOUND_", upperBound ) );
			}
			if ( lastCheckpointInfo != null ) {
				conditions.add( type.idOrder().idGreater( "HIBERNATE_SEARCH_LAST_CHECKPOINT_",
						lastCheckpointInfo.getLastProcessedEntityId() ) );
			}
			else if ( lowerBound != null ) {
				conditions.add( type.idOrder().idGreaterOrEqual( "HIBERNATE_SEARCH_PARTITION_LOWER_BOUND_", lowerBound ) );
			}

			SelectionQuery<I> query = type.createIdentifiersQuery( (SharedSessionContractImplementor) session, conditions );

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
					.setFetchSize( idFetchSize )
					.scroll( ScrollMode.FORWARD_ONLY );
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
