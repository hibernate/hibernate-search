package org.hibernate.search.backend.impl.lucene;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.backend.impl.lucene.works.LuceneWorkVisitor;
import org.hibernate.search.store.DirectoryProvider;

/**
 * Container used to split work by DirectoryProviders.
 * @author Sanne Grinovero
 */
class QueueProcessors {
	
	private final Map<DirectoryProvider, LuceneWorkVisitor> visitorsMap;
	private final Map<DirectoryProvider, PerDPQueueProcessor> dpProcessors = new HashMap<DirectoryProvider, PerDPQueueProcessor>();
	
	QueueProcessors(Map<DirectoryProvider, LuceneWorkVisitor> visitorsMap) {
		this.visitorsMap = visitorsMap;
	}

	void addWorkToDpProcessor(DirectoryProvider dp, LuceneWork work) {
		if ( ! dpProcessors.containsKey( dp ) ) {
			dpProcessors.put( dp, new PerDPQueueProcessor( visitorsMap.get( dp ) ) );
		}
		PerDPQueueProcessor processor = dpProcessors.get( dp );
		processor.addWork ( work );
	}
	
	Collection<PerDPQueueProcessor> getQueueProcessors(){
		return dpProcessors.values();
	}

}
