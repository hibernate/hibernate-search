/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.engine.worker;

import java.util.Map;

/**
 * @author Emmanuel Bernard
 */
public class AsyncWorkerTest extends WorkerTestCase {

	@Override
	public void configure(Map<String,Object> cfg) {
		cfg.put( "hibernate.search.worker.scope", "transaction" );
		cfg.put( "hibernate.search.default.worker.execution", "async" );
		cfg.put( "hibernate.search.default.worker.thread_pool.size", "1" );
		cfg.put( "hibernate.search.default.worker.buffer_queue.max", "10" );
	}

	@Override
	protected boolean isWorkerSync() {
		return false;
	}
}
