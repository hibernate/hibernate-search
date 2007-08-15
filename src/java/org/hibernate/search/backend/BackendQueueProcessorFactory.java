//$Id$
package org.hibernate.search.backend;

import java.util.Properties;
import java.util.List;

import org.hibernate.search.engine.SearchFactoryImplementor;

/**
 * Build stateful backend processor
 * Must have a no arg constructor
 * The factory typically prepare or pool the resources needed by the queue processor
 *
 * @author Emmanuel Bernard
 */
public interface BackendQueueProcessorFactory {
	void initialize(Properties props, SearchFactoryImplementor searchFactory);

	/**
	 * Return a runnable implementation responsible for processing the queue to a given backend
	 */

	Runnable getProcessor(List<LuceneWork> queue);
}
