//$Id$
package org.hibernate.search.test.worker;

import org.hibernate.search.store.RAMDirectoryProvider;
import org.hibernate.search.Environment;
import org.hibernate.cfg.Configuration;
import org.apache.lucene.analysis.StopAnalyzer;

/**
 * @author Emmanuel Bernard
 */
public class AsyncWorkerTest extends WorkerTestCase {

	protected void configure(Configuration cfg) {
		super.configure( cfg );
		cfg.setProperty( "hibernate.search.default.directory_provider", RAMDirectoryProvider.class.getName() );
		cfg.setProperty( Environment.ANALYZER_CLASS, StopAnalyzer.class.getName() );
		cfg.setProperty( Environment.WORKER_SCOPE, "transaction" );
		cfg.setProperty( Environment.WORKER_EXECUTION, "async" );
		cfg.setProperty( Environment.WORKER_PREFIX + "thread_pool.size", "1" );
		cfg.setProperty( Environment.WORKER_PREFIX + "buffer_queue.max", "10" );
	}

}
