/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.jakarta.batch.core.massindexing.step.impl;

import java.io.Serializable;
import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import jakarta.batch.api.BatchProperty;
import jakarta.batch.api.chunk.AbstractItemWriter;
import jakarta.batch.runtime.context.JobContext;
import jakarta.batch.runtime.context.StepContext;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.LockModeType;

import org.hibernate.CacheMode;
import org.hibernate.FlushMode;
import org.hibernate.Session;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.search.engine.backend.work.execution.DocumentCommitStrategy;
import org.hibernate.search.engine.backend.work.execution.DocumentRefreshStrategy;
import org.hibernate.search.engine.backend.work.execution.OperationSubmitter;
import org.hibernate.search.engine.backend.work.execution.spi.UnsupportedOperationBehavior;
import org.hibernate.search.jakarta.batch.core.logging.impl.Log;
import org.hibernate.search.jakarta.batch.core.massindexing.MassIndexingJobParameters;
import org.hibernate.search.jakarta.batch.core.massindexing.impl.JobContextData;
import org.hibernate.search.jakarta.batch.core.massindexing.util.impl.EntityTypeDescriptor;
import org.hibernate.search.jakarta.batch.core.massindexing.util.impl.MassIndexingPartitionProperties;
import org.hibernate.search.jakarta.batch.core.massindexing.util.impl.PersistenceUtil;
import org.hibernate.search.jakarta.batch.core.massindexing.util.impl.SerializationUtil;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.mapping.SearchMapping;
import org.hibernate.search.mapper.orm.spi.BatchMappingContext;
import org.hibernate.search.mapper.orm.tenancy.spi.TenancyConfiguration;
import org.hibernate.search.mapper.pojo.work.spi.PojoIndexer;
import org.hibernate.search.mapper.pojo.work.spi.PojoScopeWorkspace;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.impl.Futures;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public class EntityWriter extends AbstractItemWriter {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );
	private static final String ID_PARAMETER_NAME = "ids";

	@Inject
	private JobContext jobContext;

	@Inject
	private StepContext stepContext;

	@Inject
	@BatchProperty(name = MassIndexingJobParameters.CHECKPOINT_INTERVAL)
	private String serializedCheckpointInterval;

	@Inject
	@BatchProperty(name = MassIndexingJobParameters.CACHE_MODE)
	private String serializedCacheMode;

	@Inject
	@BatchProperty(name = MassIndexingJobParameters.ENTITY_FETCH_SIZE)
	private String serializedEntityFetchSize;

	@Inject
	@BatchProperty(name = MassIndexingPartitionProperties.ENTITY_NAME)
	private String entityName;

	@Inject
	@BatchProperty(name = MassIndexingPartitionProperties.PARTITION_ID)
	private String partitionIdStr;

	@Inject
	@BatchProperty(name = MassIndexingJobParameters.TENANT_ID)
	private String tenantId;

	private CacheMode cacheMode;
	private int entityFetchSize;

	private EntityManagerFactory emf;
	private BatchMappingContext mappingContext;
	private EntityTypeDescriptor<?, ?> type;
	private PojoScopeWorkspace workspace;

	private WriteMode writeMode;
	private TenancyConfiguration tenancyConfiguration;

	/**
	 * The open method prepares the writer to write items.
	 *
	 * @param checkpoint the last checkpoint
	 *
	 * @throws SearchException if the entityName does not match any indexed class type in the job context data.
	 */
	@Override
	public void open(Serializable checkpoint) {
		log.openingEntityWriter( partitionIdStr, entityName );
		JobContextData jobContextData = (JobContextData) jobContext.getTransientUserData();

		emf = jobContextData.getEntityManagerFactory();
		tenancyConfiguration = jobContextData.getTenancyConfiguration();
		SearchMapping searchMapping = Search.mapping( emf );
		mappingContext = (BatchMappingContext) searchMapping;
		type = jobContextData.getEntityTypeDescriptor( entityName );
		workspace = mappingContext.scope( type.javaClass(), entityName ).pojoWorkspace( tenantId );

		cacheMode = SerializationUtil.parseCacheModeParameter(
				MassIndexingJobParameters.CACHE_MODE, serializedCacheMode, MassIndexingJobParameters.Defaults.CACHE_MODE
		);
		int checkpointInterval = SerializationUtil.parseIntegerParameter(
				MassIndexingJobParameters.CHECKPOINT_INTERVAL, serializedCheckpointInterval
		);
		Integer entityFetchSizeRaw = SerializationUtil.parseIntegerParameterOptional(
				MassIndexingJobParameters.ENTITY_FETCH_SIZE, serializedEntityFetchSize, null );
		entityFetchSize = MassIndexingJobParameters.Defaults.entityFetchSize( entityFetchSizeRaw, checkpointInterval );

		/*
		 * Always execute works as updates on the first checkpoint interval,
		 * because we may be recovering from a failure, and there's no way
		 * to accurately detect that situation.
		 * Indeed, Jakarta Batch only specify that checkpoint state will be
		 * saved *after* each chunk, so when we fail during the very first checkpoint,
		 * we have no way of detecting this failure.
		 */
		this.writeMode = WriteMode.UPDATE;
	}

	@Override
	public void writeItems(List<Object> entityIds) {
		try ( Session session = PersistenceUtil.openSession( emf, tenancyConfiguration.convert( tenantId ) ) ) {
			SessionImplementor sessionImplementor = session.unwrap( SessionImplementor.class );

			PojoIndexer indexer = mappingContext.sessionContext( session ).createIndexer();

			int i = 0;
			while ( i < entityIds.size() ) {
				int fromIndex = i;
				i += entityFetchSize;
				int toIndex = Math.min( i, entityIds.size() );

				List<?> entities = loadEntities( sessionImplementor, entityIds.subList( fromIndex, toIndex ) );

				indexAndWaitForCompletion( entities, indexer );
			}
		}

		/*
		 * Flush after each write operation
		 * This ensures the writes have actually been persisted,
		 * which is necessary because the runtime will perform a checkpoint
		 * just after we return from this method.
		 */
		Futures.unwrappedExceptionJoin( workspace.flush( OperationSubmitter.blocking(),
				// If not supported, we're on Amazon OpenSearch Serverless,
				// and in this case purge writes are safe even without a flush.
				UnsupportedOperationBehavior.IGNORE ) );

		// update work count
		PartitionContextData partitionData = (PartitionContextData) stepContext.getTransientUserData();
		partitionData.documentAdded( entityIds.size() );

		/*
		 * We can switch to a faster mode, without checks, because we know the next items
		 * we'll write haven't been written to the index yet.
		 */
		this.writeMode = WriteMode.ADD;
	}

	@Override
	public void close() throws Exception {
		log.closingEntityWriter( partitionIdStr, entityName );
	}

	private List<?> loadEntities(SessionImplementor session, List<Object> entityIds) {
		return type.createLoadingQuery( session, ID_PARAMETER_NAME )
				.setParameter( ID_PARAMETER_NAME, entityIds )
				.setReadOnly( true )
				.setCacheable( false )
				.setLockMode( LockModeType.NONE )
				.setCacheMode( cacheMode )
				.setHibernateFlushMode( FlushMode.MANUAL )
				.setFetchSize( entityFetchSize )
				.list();
	}

	private void indexAndWaitForCompletion(List<?> entities, PojoIndexer indexer) {
		if ( entities == null || entities.isEmpty() ) {
			return;
		}

		CompletableFuture<?>[] indexingFutures = new CompletableFuture<?>[entities.size()];
		for ( int i = 0; i < entities.size(); i++ ) {
			indexingFutures[i] = writeItem( indexer, entities.get( i ) );
		}

		try {
			Futures.unwrappedExceptionGet( CompletableFuture.allOf( indexingFutures ) );
		}
		catch (InterruptedException e) {
			// mark current thread interrupted and raise the exception to propagate the error up
			Thread.currentThread().interrupt();
			throw new IllegalStateException( "Writer thread was interrupted", e );
		}
	}

	private CompletableFuture<?> writeItem(PojoIndexer indexer, Object entity) {
		log.processEntity( entity );

		if ( WriteMode.ADD.equals( writeMode ) ) {
			return indexer.add( type.typeIdentifier(), null, null, entity,
					// Commit and refresh are handled globally after all documents are indexed.
					DocumentCommitStrategy.NONE, DocumentRefreshStrategy.NONE, OperationSubmitter.blocking()
			);
		}

		return indexer.addOrUpdate( type.typeIdentifier(), null, null, entity,
				// Commit and refresh are handled globally after all documents are indexed.
				DocumentCommitStrategy.NONE, DocumentRefreshStrategy.NONE, OperationSubmitter.blocking()
		);
	}

	private enum WriteMode {
		ADD,
		UPDATE;
	}
}
