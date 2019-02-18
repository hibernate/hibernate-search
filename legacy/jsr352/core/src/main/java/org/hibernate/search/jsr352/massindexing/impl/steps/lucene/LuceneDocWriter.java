/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.jsr352.massindexing.impl.steps.lucene;

import java.io.Serializable;
import java.util.List;
import java.util.Set;
import javax.batch.api.BatchProperty;
import javax.batch.api.chunk.AbstractItemWriter;
import javax.batch.runtime.context.JobContext;
import javax.batch.runtime.context.StepContext;
import javax.inject.Inject;

import org.hibernate.search.backend.AddLuceneWork;
import org.hibernate.search.backend.FlushLuceneWork;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.backend.UpdateLuceneWork;
import org.hibernate.search.backend.impl.StreamingOperationExecutor;
import org.hibernate.search.backend.impl.StreamingOperationExecutorSelector;
import org.hibernate.search.engine.spi.EntityIndexBinding;
import org.hibernate.search.exception.AssertionFailure;
import org.hibernate.search.indexes.spi.IndexManager;
import org.hibernate.search.indexes.spi.IndexManagerSelector;
import org.hibernate.search.jsr352.logging.impl.Log;
import org.hibernate.search.jsr352.massindexing.impl.JobContextData;
import org.hibernate.search.jsr352.massindexing.impl.util.MassIndexingPartitionProperties;
import org.hibernate.search.spi.IndexedTypeIdentifier;
import org.hibernate.search.spi.impl.PojoIndexedTypeIdentifier;

import org.hibernate.search.util.logging.impl.LoggerFactory;
import java.lang.invoke.MethodHandles;

/**
 * Batch item writer writes a list of items into Lucene documents. Here, items mean the luceneWorks, given by the
 * processor. These items will be executed using StreamingOperationExecutor.
 *
 * @author Mincong Huang
 */
@SuppressWarnings("deprecation")
public class LuceneDocWriter extends AbstractItemWriter {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );
	private static final boolean FORCE_ASYNC = true;

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

	private IndexManagerSelector indexManagerSelector;

	private WriteMode writeMode;

	/**
	 * The open method prepares the writer to write items.
	 *
	 * @param checkpoint the last checkpoint
	 */
	@Override
	public void open(Serializable checkpoint) throws Exception {
		log.openingDocWriter( partitionIdStr, entityName );

		/*
		 * Always execute works as updates on the first checkpoint interval,
		 * because we may be recovering from a failure, and there's no way
		 * to accurately detect that situation.
		 * Indeed, JSR-352 only specify that checkpoint state will be
		 * saved *after* each chunk, so when we fail during the very first checkpoint,
		 * we have no way of detecting this failure.
		 */
		this.writeMode = WriteMode.UPDATE;

		JobContextData jobData = (JobContextData) jobContext.getTransientUserData();

		Class<?> entityType = jobData.getEntityType( entityName );
		IndexedTypeIdentifier typeIdentifier = new PojoIndexedTypeIdentifier( entityType );
		EntityIndexBinding entityIndexBinding = jobData.getSearchIntegrator()
				.getIndexBinding( typeIdentifier );
		indexManagerSelector = entityIndexBinding.getIndexManagerSelector();
	}

	/**
	 * Execute {@code LuceneWork}
	 *
	 * @param items a list of LuceneWork, either AddLuceneWork or UpdateLuceneWork.
	 *
	 * @throws Exception is thrown for any errors.
	 */
	@Override
	public void writeItems(List<Object> items) throws Exception {
		for ( Object item : items ) {
			writeItem( item );
		}

		/*
		 * Flush after each write operation
		 * This ensures the writes have actually been persisted,
		 * which is necessary because the runtime will perform a checkpoint
		 * just after we return from this method.
		 */
		Set<IndexManager> indexManagers = indexManagerSelector.all();
		for ( IndexManager im : indexManagers ) {
			im.performStreamOperation( FlushLuceneWork.INSTANCE, null, false );
		}

		// update work count
		PartitionContextData partitionData = (PartitionContextData) stepContext.getTransientUserData();
		partitionData.documentAdded( items.size() );

		/*
		 * We can switch to a faster mode, without checks, because we know the next items
		 * we'll write haven't been written to the index yet.
		 */
		this.writeMode = WriteMode.ADD;
	}

	private void writeItem(Object item) {
		LuceneWork work = extractWork( item );
		StreamingOperationExecutor executor = work.acceptIndexWorkVisitor(
				StreamingOperationExecutorSelector.INSTANCE, null );
		executor.performStreamOperation(
				work,
				indexManagerSelector,
				null, // monitor,
				FORCE_ASYNC );
	}

	private LuceneWork extractWork(Object item) {
		AddLuceneWork addWork = (AddLuceneWork) item;
		switch ( writeMode ) {
			case ADD:
				return (AddLuceneWork) item;
			case UPDATE:
				return new UpdateLuceneWork(
						addWork.getId(), addWork.getIdInString(), addWork.getEntityType(),
						addWork.getDocument(), addWork.getFieldToAnalyzerMap()
				);
			default:
				throw new AssertionFailure( "Invalid WriteMode: " + writeMode );
		}
	}

	private enum WriteMode {
		ADD,
		UPDATE;
	}
}
