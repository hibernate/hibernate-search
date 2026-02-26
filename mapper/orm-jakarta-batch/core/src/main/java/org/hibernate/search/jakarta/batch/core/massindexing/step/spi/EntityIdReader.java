/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.jakarta.batch.core.massindexing.step.spi;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

import jakarta.batch.api.BatchProperty;
import jakarta.batch.api.chunk.AbstractItemReader;
import jakarta.batch.runtime.context.JobContext;
import jakarta.batch.runtime.context.StepContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.persistence.EntityManagerFactory;

import org.hibernate.StatelessSession;
import org.hibernate.search.jakarta.batch.core.context.jpa.spi.EntityManagerFactoryRegistry;
import org.hibernate.search.jakarta.batch.core.inject.scope.spi.HibernateSearchPartitionScoped;
import org.hibernate.search.jakarta.batch.core.logging.impl.JakartaBatchLog;
import org.hibernate.search.jakarta.batch.core.massindexing.MassIndexingJobParameters;
import org.hibernate.search.jakarta.batch.core.massindexing.impl.JobContextData;
import org.hibernate.search.jakarta.batch.core.massindexing.step.impl.HibernateSearchPartitionMapper;
import org.hibernate.search.jakarta.batch.core.massindexing.step.impl.PartitionContextData;
import org.hibernate.search.jakarta.batch.core.massindexing.util.impl.BatchCoreEntityTypeDescriptor;
import org.hibernate.search.jakarta.batch.core.massindexing.util.impl.JobContextUtil;
import org.hibernate.search.jakarta.batch.core.massindexing.util.impl.MassIndexingPartitionProperties;
import org.hibernate.search.jakarta.batch.core.massindexing.util.impl.PartitionBound;
import org.hibernate.search.jakarta.batch.core.massindexing.util.impl.PersistenceUtil;
import org.hibernate.search.jakarta.batch.core.massindexing.util.impl.SerializationUtil;
import org.hibernate.search.mapper.orm.loading.batch.HibernateOrmBatchIdentifierLoader;
import org.hibernate.search.mapper.orm.loading.batch.HibernateOrmBatchIdentifierLoadingOptions;
import org.hibernate.search.mapper.orm.loading.batch.HibernateOrmBatchReindexCondition;
import org.hibernate.search.mapper.orm.tenancy.spi.TenancyConfiguration;
import org.hibernate.search.util.common.impl.Closer;

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
	private BatchCoreEntityTypeDescriptor<?, ?> type;
	private TenancyConfiguration tenancyConfiguration;

	private int idFetchSize;
	private Integer maxResults;
	private HibernateOrmBatchReindexCondition reindexOnly;
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
		JakartaBatchLog.INSTANCE.openingReader( serializedPartitionId, entityName );

		JobContextData jobData = getOrCreateJobContextData();

		emf = jobData.getEntityManagerFactory();
		type = jobData.getEntityTypeDescriptor( entityName );
		tenancyConfiguration = jobData.getTenancyConfiguration();

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
		JakartaBatchLog.INSTANCE.closingReader( serializedPartitionId, entityName );
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
		JakartaBatchLog.INSTANCE.readingEntityId();

		Object id = chunkState.next();
		if ( id == null ) {
			JakartaBatchLog.INSTANCE.noMoreResults();
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
		JakartaBatchLog.INSTANCE.checkpointReached( entityName, checkpointInfo );
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
		private HibernateOrmBatchIdentifierLoader identifierLoader;

		private CheckpointInfo lastCheckpointInfo;
		private int processedEntityCount = 0;
		private Object lastProcessedEntityId;

		public ChunkState(Serializable checkpointInfo) {
			this.lastCheckpointInfo = (CheckpointInfo) checkpointInfo;
		}

		/**
		 * Get the next element for the current chunk.
		 *
		 * @return The next element for this chunk.
		 */
		public Object next() {
			if ( identifierLoader == null ) {
				start();
			}
			if ( !identifierLoader.hasNext() ) {
				return null;
			}
			Object id = identifierLoader.next();
			lastProcessedEntityId = id;
			++processedEntityCount;
			return id;
		}

		private <E> HibernateOrmBatchIdentifierLoader createIdentifierLoader(BatchCoreEntityTypeDescriptor<E, ?> type,
				HibernateOrmBatchIdentifierLoadingOptions options) {
			return type.batchLoadingStrategy().createIdentifierLoader( type, options );
		}

		/**
		 * End a chunk.
		 *
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
				if ( identifierLoader != null ) {
					closer.push( HibernateOrmBatchIdentifierLoader::close, identifierLoader );
					identifierLoader = null;
				}
				if ( session != null ) {
					closer.push( StatelessSession::close, session );
					session = null;
				}
			}
		}

		private void start() {
			session = PersistenceUtil.openStatelessSession( emf, tenancyConfiguration.convert( tenantId ) );
			try {
				boolean hasNoPreviousCheckpoint = lastCheckpointInfo == null;
				HibernateOrmBatchIdentifierLoadingOptions options = new LoadingOptions( session,
						idFetchSize, actualMaxResults(), upperBound,
						hasNoPreviousCheckpoint ? lowerBound : lastCheckpointInfo.getLastProcessedEntityId(),
						hasNoPreviousCheckpoint, reindexOnly
				);
				identifierLoader = createIdentifierLoader( type, options );
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

		private Integer actualMaxResults() {
			if ( maxResults != null ) {
				int remaining;
				if ( lastCheckpointInfo != null ) {
					remaining = maxResults - lastCheckpointInfo.getProcessedEntityCount();
				}
				else {
					remaining = maxResults;
				}
				return remaining;
			}
			return null;
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

	private static class LoadingOptions implements HibernateOrmBatchIdentifierLoadingOptions {
		private final HibernateOrmBatchReindexCondition reindexOnly;
		private final Map<Class<?>, Object> contextData;
		private final int fetchSize;
		private final Integer maxResults;
		private final Object upperBound;
		private final Object lowerBound;
		private final boolean lowerBoundInclusive;


		LoadingOptions(StatelessSession session,
				int fetchSize, Integer maxResults, Object upperBound, Object lowerBound,
				boolean lowerBoundInclusive, HibernateOrmBatchReindexCondition reindexOnly) {
			this.fetchSize = fetchSize;
			this.maxResults = maxResults;
			this.upperBound = upperBound;
			this.lowerBound = lowerBound;
			this.reindexOnly = reindexOnly;
			this.lowerBoundInclusive = lowerBoundInclusive;

			this.contextData = new HashMap<>();

			this.contextData.put( StatelessSession.class, session );
		}

		@Override
		public int fetchSize() {
			return fetchSize;
		}

		@Override
		public OptionalInt maxResults() {
			return maxResults == null ? OptionalInt.empty() : OptionalInt.of( maxResults );
		}

		@Override
		public OptionalInt offset() {
			return OptionalInt.empty();
		}

		@Override
		public Optional<HibernateOrmBatchReindexCondition> reindexOnlyCondition() {
			return Optional.ofNullable( reindexOnly );
		}

		@Override
		public Optional<Object> upperBound() {
			return Optional.ofNullable( upperBound );
		}

		@Override
		public boolean upperBoundInclusive() {
			return false;
		}

		@Override
		public Optional<Object> lowerBound() {
			return Optional.ofNullable( lowerBound );
		}

		@Override
		public boolean lowerBoundInclusive() {
			return lowerBoundInclusive;
		}

		@SuppressWarnings("unchecked")
		@Override
		public <T> T context(Class<T> contextType) {
			return (T) contextData.get( contextType );
		}
	}
}
