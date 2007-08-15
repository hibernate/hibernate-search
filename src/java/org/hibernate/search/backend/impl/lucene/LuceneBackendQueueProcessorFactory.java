//$Id$
package org.hibernate.search.backend.impl.lucene;

import java.util.Properties;
import java.util.List;

import org.hibernate.search.backend.BackendQueueProcessorFactory;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.engine.SearchFactoryImplementor;

/**
 * @author Emmanuel Bernard
 */
public class LuceneBackendQueueProcessorFactory implements BackendQueueProcessorFactory {
	private SearchFactoryImplementor searchFactoryImplementor;

	public void initialize(Properties props, SearchFactoryImplementor searchFactoryImplementor) {
		this.searchFactoryImplementor = searchFactoryImplementor;
	}

	public Runnable getProcessor(List<LuceneWork> queue) {
		return new LuceneBackendQueueProcessor( queue, searchFactoryImplementor );
	}
}
