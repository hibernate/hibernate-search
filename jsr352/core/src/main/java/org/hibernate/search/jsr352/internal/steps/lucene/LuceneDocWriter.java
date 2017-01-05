/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.jsr352.internal.steps.lucene;

import java.io.Serializable;
import java.util.List;

import javax.batch.api.BatchProperty;
import javax.batch.api.chunk.AbstractItemWriter;
import javax.batch.runtime.context.JobContext;
import javax.batch.runtime.context.StepContext;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;

import org.hibernate.search.backend.AddLuceneWork;
import org.hibernate.search.backend.FlushLuceneWork;
import org.hibernate.search.backend.impl.StreamingOperationExecutor;
import org.hibernate.search.backend.impl.StreamingOperationExecutorSelector;
import org.hibernate.search.engine.spi.EntityIndexBinding;
import org.hibernate.search.indexes.spi.IndexManager;
import org.hibernate.search.jpa.Search;
import org.hibernate.search.jsr352.internal.JobContextData;
import org.hibernate.search.jsr352.internal.se.JobSEEnvironment;
import org.hibernate.search.spi.SearchIntegrator;
import org.hibernate.search.store.IndexShardingStrategy;
import org.jboss.logging.Logger;

/**
 * Batch item writer writes a list of items into Lucene documents. Here, items mean the luceneWorks, given by the
 * processor. These items will be executed using StreamingOperationExecutor.
 *
 * @author Mincong Huang
 */
@Named
@SuppressWarnings("deprecation")
public class LuceneDocWriter extends AbstractItemWriter {

	private static final Logger LOGGER = Logger.getLogger( LuceneDocWriter.class );
	private static final boolean FORCE_ASYNC = true;
	private final JobContext jobContext;
	private final StepContext stepContext;

	@Inject
	@BatchProperty
	private String entityName;

	@Inject
	@BatchProperty
	private String isJavaSE;

	@PersistenceUnit(unitName = "h2")
	private EntityManagerFactory emf;

	private EntityManager em;
	private EntityIndexBinding entityIndexBinding;

	@Inject
	public LuceneDocWriter(JobContext jobContext, StepContext stepContext) {
		this.jobContext = jobContext;
		this.stepContext = stepContext;
	}

	/**
	 * The close method marks the end of use of the ItemWriter. This method is called when the job stops for any reason.
	 * In case of job interruption, the job might need to be restarted. That's why the step context data is persisted.
	 *
	 * @throws Exception is thrown for any errors.
	 */
	@Override
	public void close() throws Exception {
		LOGGER.debug( "close() called." );
		try {
			em.close();
		}
		catch (Exception e) {
			LOGGER.error( e );
		}
	}

	/**
	 * The open method prepares the writer to write items.
	 *
	 * @param checkpoint the last checkpoint
	 */
	@Override
	public void open(Serializable checkpoint) throws Exception {

		LOGGER.debug( "open(Seriliazable) called" );
		if ( Boolean.parseBoolean( isJavaSE ) ) {
			emf = JobSEEnvironment.getInstance().getEntityManagerFactory();
		}
		em = emf.createEntityManager();

		JobContextData jobData = (JobContextData) jobContext.getTransientUserData();
		Class<?> entityType = jobData.getIndexedType( entityName );
		entityIndexBinding = Search
				.getFullTextEntityManager( em )
				.getSearchFactory()
				.unwrap( SearchIntegrator.class )
				.getIndexBinding( entityType );
	}

	/**
	 * Execute {@code LuceneWork}
	 *
	 * @param items a list of items, where each item is a list of Lucene works.
	 * @throw Exception is thrown for any errors.
	 */
	@Override
	public void writeItems(List<Object> items) throws Exception {
		IndexShardingStrategy shardingStrategy = entityIndexBinding.getSelectionStrategy();

		for ( Object item : items ) {
			AddLuceneWork addWork = (AddLuceneWork) item;
			StreamingOperationExecutor executor = addWork.acceptIndexWorkVisitor(
					StreamingOperationExecutorSelector.INSTANCE, null );
			executor.performStreamOperation(
					addWork,
					shardingStrategy,
					null, // monitor,
					FORCE_ASYNC );
		}

		// flush after write operation
		IndexManager[] indexManagers = entityIndexBinding.getIndexManagers();
		for ( IndexManager im : indexManagers ) {
			im.performStreamOperation( FlushLuceneWork.INSTANCE, null, false );
		}

		// update work count
		PartitionContextData partitionData = (PartitionContextData) stepContext.getTransientUserData();
		partitionData.documentAdded( items.size() );
	}
}
