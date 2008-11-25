//$Id$
package org.hibernate.search.backend.impl.lucene;

import java.util.List;
import java.util.Map;

import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.engine.DocumentBuilderIndexedEntity;
import org.hibernate.search.engine.SearchFactoryImplementor;
import org.hibernate.search.store.DirectoryProvider;
import org.hibernate.search.store.IndexShardingStrategy;
import org.hibernate.search.util.LoggerFactory;
import org.slf4j.Logger;

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
	private final Map<DirectoryProvider,PerDPResources> resourcesMap;
	private final boolean sync;
	
	private static final DpSelectionVisitor providerSelectionVisitor = new DpSelectionVisitor();
	private static final Logger log = LoggerFactory.make();

	LuceneBackendQueueProcessor(List<LuceneWork> queue,
			SearchFactoryImplementor searchFactoryImplementor,
			Map<DirectoryProvider,PerDPResources> resourcesMap,
			boolean syncMode) {
		this.sync = syncMode;
		this.queue = queue;
		this.searchFactoryImplementor = searchFactoryImplementor;
		this.resourcesMap = resourcesMap;
	}

	public void run() {
		QueueProcessors processors = new QueueProcessors( resourcesMap );
		// divide the queue in tasks, adding to QueueProcessors by affected Directory.
		for ( LuceneWork work : queue ) {
			final Class<?> entityType = work.getEntityClass();
			DocumentBuilderIndexedEntity<?> documentBuilder = searchFactoryImplementor.getDocumentBuilderIndexedEntity( entityType );
			IndexShardingStrategy shardingStrategy = documentBuilder.getDirectoryProviderSelectionStrategy();
			work.getWorkDelegate( providerSelectionVisitor ).addAsPayLoadsToQueue( work, shardingStrategy, processors );
		}
		try {
			//this Runnable splits tasks in more runnables and then runs them:
			processors.runAll( sync );
		} catch (InterruptedException e) {
			log.error( "Index update task has been interrupted", e );
		}
	}
	
}
