/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.engine.worker;

import org.jboss.byteman.contrib.bmunit.BMRule;
import org.jboss.byteman.contrib.bmunit.BMUnitRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.hibernate.cfg.Configuration;

/**
 * @author Emmanuel Bernard
 */
@RunWith(BMUnitRunner.class)
public class AsyncWorkerTest extends WorkerTestCase {

	@Override
	@Test
	@BMRule(targetClass = "org.hibernate.search.backend.impl.lucene.LuceneBackendQueueProcessor",
			targetMethod = "applyWork",
			helper = "org.hibernate.search.test.util.BytemanHelper",
			action = "assertBooleanValue($0.sync, false)", // asserting that we are in async mode
			name = "testConcurrency")
	public void testConcurrency() throws Exception {
		super.testConcurrency();
	}

	@Override
	protected void configure(Configuration cfg) {
		super.configure( cfg );
		cfg.setProperty( "hibernate.search.worker.scope", "transaction" );
		cfg.setProperty( "hibernate.search.default.worker.execution", "async" );
		cfg.setProperty( "hibernate.search.default.worker.thread_pool.size", "1" );
		cfg.setProperty( "hibernate.search.default.worker.buffer_queue.max", "10" );
	}

	@Override
	protected boolean isWorkerSync() {
		return false;
	}
}
