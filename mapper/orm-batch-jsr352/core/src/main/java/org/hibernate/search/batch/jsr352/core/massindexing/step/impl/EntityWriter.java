/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.batch.jsr352.core.massindexing.step.impl;

import java.io.Serializable;
import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import javax.batch.api.BatchProperty;
import javax.batch.api.chunk.AbstractItemWriter;
import javax.batch.runtime.context.JobContext;
import javax.batch.runtime.context.StepContext;
import javax.inject.Inject;
import javax.persistence.EntityManagerFactory;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.search.batch.jsr352.core.logging.impl.Log;
import org.hibernate.search.batch.jsr352.core.massindexing.MassIndexingJobParameters;
import org.hibernate.search.batch.jsr352.core.massindexing.impl.JobContextData;
import org.hibernate.search.batch.jsr352.core.massindexing.util.impl.MassIndexingPartitionProperties;
import org.hibernate.search.engine.backend.work.execution.DocumentCommitStrategy;
import org.hibernate.search.engine.backend.work.execution.DocumentRefreshStrategy;
import org.hibernate.search.engine.backend.work.execution.OperationSubmitter;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.mapping.SearchMapping;
import org.hibernate.search.mapper.orm.spi.BatchMappingContext;
import org.hibernate.search.mapper.orm.work.SearchWorkspace;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeIdentifier;
import org.hibernate.search.mapper.pojo.work.spi.PojoIndexer;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.impl.Futures;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public class EntityWriter extends AbstractItemWriter {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	@Inject
	private JobContext jobContext;

	@Inject
	private StepContext stepContext;

	@Inject
	@BatchProperty(name = MassIndexingPartitionProperties.ENTITY_NAME)
	private String entityName;

	@Inject
	@BatchProperty(name = MassIndexingPartitionProperties.PARTITION_ID)
	private String partitionIdStr;

	@Inject
	@BatchProperty(name = MassIndexingJobParameters.TENANT_ID)
	private String tenantId;

	private EntityManagerFactory emf;
	private SearchMapping searchMapping;
	private BatchMappingContext mappingContext;
	private PojoRawTypeIdentifier<?> typeIdentifier;
	private SearchWorkspace workspace;

	private WriteMode writeMode;

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
		searchMapping = Search.mapping( emf );
		mappingContext = (BatchMappingContext) searchMapping;
		typeIdentifier = mappingContext.typeContextProvider().byEntityName().getOrFail( entityName ).typeIdentifier();
		workspace = searchMapping.scope( typeIdentifier.javaClass(), entityName ).workspace( tenantId );

		/*
		 * Always execute works as updates on the first checkpoint interval,
		 * because we may be recovering from a failure, and there's no way
		 * to accurately detect that situation.
		 * Indeed, JSR-352 only specify that checkpoint state will be
		 * saved *after* each chunk, so when we fail during the very first checkpoint,
		 * we have no way of detecting this failure.
		 */
		this.writeMode = WriteMode.UPDATE;
	}

	@Override
	public void writeItems(List<Object> entities) {
		try ( Session session = emf.unwrap( SessionFactory.class )
				.withOptions()
				.tenantIdentifier( tenantId )
				.openSession() ) {

			PojoIndexer indexer = mappingContext.sessionContext( session ).createIndexer();

			indexAndWaitForCompletion( entities, indexer );

			/*
			 * Flush after each write operation
			 * This ensures the writes have actually been persisted,
			 * which is necessary because the runtime will perform a checkpoint
			 * just after we return from this method.
			 */
			workspace.flush();
		}

		// update work count
		PartitionContextData partitionData = (PartitionContextData) stepContext.getTransientUserData();
		partitionData.documentAdded( entities.size() );

		/*
		 * We can switch to a faster mode, without checks, because we know the next items
		 * we'll write haven't been written to the index yet.
		 */
		this.writeMode = WriteMode.ADD;

		log.closingEntityWriter( partitionIdStr, entityName );
	}

	private void indexAndWaitForCompletion(List<Object> entities, PojoIndexer indexer) {
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
			return indexer.add( typeIdentifier, null, null, entity,
					// Commit and refresh are handled globally after all documents are indexed.
					DocumentCommitStrategy.NONE, DocumentRefreshStrategy.NONE, OperationSubmitter.BLOCKING
			);
		}

		return indexer.addOrUpdate( typeIdentifier, null, null, entity,
				// Commit and refresh are handled globally after all documents are indexed.
				DocumentCommitStrategy.NONE, DocumentRefreshStrategy.NONE, OperationSubmitter.BLOCKING
		);
	}

	private enum WriteMode {
		ADD,
		UPDATE;
	}
}
