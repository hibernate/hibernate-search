package org.hibernate.search.jsr352.internal;

import javax.batch.api.BatchProperty;
import javax.batch.api.chunk.listener.AbstractItemReadListener;
import javax.batch.runtime.context.JobContext;
import javax.batch.runtime.context.StepContext;
import javax.inject.Inject;
import javax.inject.Named;

import org.hibernate.search.engine.spi.EntityIndexBinding;
import org.hibernate.search.jpa.Search;
import org.hibernate.search.spi.SearchIntegrator;
import org.hibernate.search.store.IndexShardingStrategy;
import org.jboss.logging.Logger;

/**
 * Sets up the {@link EntityIndexingStepData} for one entity indexing step.
 *
 * @author Gunnar Morling
 */
@Named
public class EntityIndexingStepContextSetupListener extends AbstractItemReadListener {

	private static final Logger LOGGER = Logger.getLogger( BatchItemReader.class );

	private final JobContext jobContext;
	private final IndexingContext indexingContext;
	private final StepContext stepContext;

	@Inject @BatchProperty
	private String entityType;


	@Inject
	public EntityIndexingStepContextSetupListener(JobContext jobContext, IndexingContext indexingContext, StepContext stepContext) {
		this.jobContext = jobContext;
		this.indexingContext = indexingContext;
		this.stepContext = stepContext;
	}

	@Override
	public void beforeRead() throws Exception {
		Class<?> entityClazz = ( (BatchContextData) jobContext.getTransientUserData() ).getIndexedType( entityType );

		LOGGER.debugf( "#beforeRead(...): entityClazz = %s", entityClazz );

		EntityIndexBinding entityIndexBinding = Search.getFullTextEntityManager( indexingContext.getEntityManager() )
				.getSearchFactory()
				.unwrap( SearchIntegrator.class )
				.getIndexBinding( entityClazz );

		IndexShardingStrategy shardingStrategy = entityIndexBinding.getSelectionStrategy();

		EntityIndexingStepData stepContextData = new EntityIndexingStepData( entityClazz, shardingStrategy );
		stepContext.setTransientUserData( stepContextData );
	}
}
