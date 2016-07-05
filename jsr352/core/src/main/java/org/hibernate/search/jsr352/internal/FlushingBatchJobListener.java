package org.hibernate.search.jsr352.internal;

import java.util.Collection;
import java.util.HashMap;

import javax.batch.api.listener.AbstractJobListener;
import javax.batch.runtime.context.JobContext;
import javax.inject.Inject;
import javax.inject.Named;

import org.hibernate.search.backend.FlushLuceneWork;
import org.hibernate.search.engine.spi.EntityIndexBinding;
import org.hibernate.search.indexes.spi.IndexManager;
import org.hibernate.search.jpa.Search;
import org.hibernate.search.spi.SearchIntegrator;

/**
 * Flushes all pending work to index managers after the batch job has run.
 *
 * @author Gunnar Morling
 */
@Named
public class FlushingBatchJobListener extends AbstractJobListener {

	private final JobContext jobContext;

	private final IndexingContext indexingContext;

	@Inject
	public FlushingBatchJobListener(JobContext jobContext, IndexingContext indexingContext) {
		this.jobContext = jobContext;
		this.indexingContext = indexingContext;
	}

	@Override
	public void afterJob() throws Exception {
		flush( ( ( BatchContextData )jobContext.getTransientUserData() ).getEntityTypesToIndex() );
	}

	private void flush(Iterable<Class<?>> entityTypes) {
		Collection<IndexManager> uniqueIndexManagers = uniqueIndexManagerForTypes( entityTypes );
		for ( IndexManager indexManager : uniqueIndexManagers ) {
			indexManager.performStreamOperation( FlushLuceneWork.INSTANCE, null, false );
		}
	}

	private Collection<IndexManager> uniqueIndexManagerForTypes(Iterable<Class<?>> entityTypes) {
		HashMap<String, IndexManager> uniqueBackends = new HashMap<>();

		for ( Class<?> type : entityTypes ) {
			EntityIndexBinding indexBindingForEntity = Search.getFullTextEntityManager( indexingContext.getEntityManager() )
					.getSearchFactory()
					.unwrap( SearchIntegrator.class )
					.getIndexBinding( type );

			if ( indexBindingForEntity != null ) {
				IndexManager[] indexManagers = indexBindingForEntity.getIndexManagers();
				for ( IndexManager im : indexManagers ) {
					uniqueBackends.put( im.getIndexName(), im );
				}
			}
		}

		return uniqueBackends.values();
	}
}
