//$Id$
package org.hibernate.search.backend.impl.lucene;

import java.util.List;
import java.util.Map;

import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.backend.impl.lucene.works.LuceneWorkVisitor;
import org.hibernate.search.engine.DocumentBuilder;
import org.hibernate.search.engine.SearchFactoryImplementor;
import org.hibernate.search.store.DirectoryProvider;
import org.hibernate.search.store.IndexShardingStrategy;

/**
 * Apply the operations to Lucene directories.
 *
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 * @author John Griffin
 * @author Sanne Grinovero
 */
class LuceneBackendQueueProcessor implements Runnable {
	
	private final List<LuceneWork> queue;
	private final SearchFactoryImplementor searchFactoryImplementor;
	private final Map<DirectoryProvider,LuceneWorkVisitor> visitorsMap;
	private static final DpSelectionVisitor providerSelectionVisitor = new DpSelectionVisitor(); 

	LuceneBackendQueueProcessor(List<LuceneWork> queue,
			SearchFactoryImplementor searchFactoryImplementor,
			Map<DirectoryProvider,LuceneWorkVisitor> visitorsMap) {
		this.queue = queue;
		this.searchFactoryImplementor = searchFactoryImplementor;
		this.visitorsMap = visitorsMap;
	}

	public void run() {
		QueueProcessors processors = new QueueProcessors( visitorsMap );
		// divide tasks in parts, adding to QueueProcessors by affected Directory.
		for ( LuceneWork work : queue ) {
			DocumentBuilder documentBuilder = searchFactoryImplementor.getDocumentBuilders().get( work.getEntityClass() );
			IndexShardingStrategy shardingStrategy = documentBuilder.getDirectoryProviderSelectionStrategy();
			work.getWorkDelegate( providerSelectionVisitor ).addAsPayLoadsToQueue( work, shardingStrategy, processors );
		}
		// TODO next cycle could be performed in parallel
		for ( PerDPQueueProcessor processor : processors.getQueueProcessors() ) {
			// perform the work on indexes
			processor.performWorks();
		}
	}

}
