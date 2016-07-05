package org.hibernate.search.jsr352.internal;

import java.util.HashSet;
import java.util.Set;

import javax.batch.api.BatchProperty;
import javax.batch.api.listener.AbstractJobListener;
import javax.batch.runtime.context.JobContext;
import javax.inject.Inject;
import javax.inject.Named;

import org.hibernate.search.jpa.Search;

@Named
public class BatchJobContextSetupListener extends AbstractJobListener {

	private final JobContext jobContext;
	private final IndexingContext indexingContext;

	@Inject @BatchProperty
	private String rootEntities;

	@Inject
	public BatchJobContextSetupListener(JobContext jobContext, IndexingContext indexingContext) {
		this.jobContext = jobContext;
		this.indexingContext = indexingContext;
	}

	@Override
	public void beforeJob() throws Exception {
		Set<Class<?>> entitiesToIndex = new HashSet<>();

		String[] entityTypeNamesToIndex = rootEntities.split( "," );
		Set<Class<?>> indexedTypes = Search.getFullTextEntityManager( indexingContext.getEntityManager() )
			.getSearchFactory()
			.getIndexedTypes();

		for ( String entityType : entityTypeNamesToIndex ) {
			for ( Class<?> indexedType : indexedTypes ) {
				if ( indexedType.getName().equals( entityType.trim() ) ) {
					entitiesToIndex.add( indexedType );
					continue;
				}
			}
		}

		jobContext.setTransientUserData( new BatchContextData( entitiesToIndex ) );
	}
}
