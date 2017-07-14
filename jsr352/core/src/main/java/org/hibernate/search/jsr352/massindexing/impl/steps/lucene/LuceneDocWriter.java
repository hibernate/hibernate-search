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

import org.hibernate.search.backend.FlushLuceneWork;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.backend.impl.StreamingOperationExecutor;
import org.hibernate.search.backend.impl.StreamingOperationExecutorSelector;
import org.hibernate.search.engine.spi.EntityIndexBinding;
import org.hibernate.search.indexes.spi.IndexManager;
import org.hibernate.search.indexes.spi.IndexManagerSelector;
import org.hibernate.search.jsr352.logging.impl.Log;
import org.hibernate.search.jsr352.massindexing.impl.JobContextData;
import org.hibernate.search.jsr352.massindexing.impl.util.MassIndexingPartitionProperties;
import org.hibernate.search.spi.IndexedTypeIdentifier;
import org.hibernate.search.spi.impl.PojoIndexedTypeIdentifier;

import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * Batch item writer writes a list of items into Lucene documents. Here, items mean the luceneWorks, given by the
 * processor. These items will be executed using StreamingOperationExecutor.
 *
 * @author Mincong Huang
 */
@SuppressWarnings("deprecation")
public class LuceneDocWriter extends AbstractItemWriter {

	private static final Log log = LoggerFactory.make( Log.class );
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

	/**
	 * The open method prepares the writer to write items.
	 *
	 * @param checkpoint the last checkpoint
	 */
	@Override
	public void open(Serializable checkpoint) throws Exception {
		log.openingDocWriter( partitionIdStr, entityName );

		JobContextData jobData = (JobContextData) jobContext.getTransientUserData();

		Class<?> entityType = jobData.getIndexedType( entityName );
		IndexedTypeIdentifier typeIdentifier = new PojoIndexedTypeIdentifier( entityType );
		EntityIndexBinding entityIndexBinding = jobData.getSearchIntegrator()
				.getIndexBinding( typeIdentifier );
		indexManagerSelector = entityIndexBinding.getIndexManagerSelector();
	}

	/**
	 * Execute {@code LuceneWork}
	 *
	 * @param items a list of items, where each item is a list of Lucene works.
	 *
	 * @throws Exception is thrown for any errors.
	 */
	@Override
	public void writeItems(List<Object> items) throws Exception {
		for ( Object item : items ) {
			LuceneWork work = (LuceneWork) item;
			StreamingOperationExecutor executor = work.acceptIndexWorkVisitor(
					StreamingOperationExecutorSelector.INSTANCE, null );
			executor.performStreamOperation(
					work,
					indexManagerSelector,
					null, // monitor,
					FORCE_ASYNC );
		}

		// flush after write operation
		Set<IndexManager> indexManagers = indexManagerSelector.all();
		for ( IndexManager im : indexManagers ) {
			im.performStreamOperation( FlushLuceneWork.INSTANCE, null, false );
		}

		// update work count
		PartitionContextData partitionData = (PartitionContextData) stepContext.getTransientUserData();
		partitionData.documentAdded( items.size() );
	}
}
