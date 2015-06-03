/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.engine.worker;

import java.util.Map;

import org.hibernate.search.cfg.Environment;

/**
 * @author Emmanuel Bernard
 */
public class SyncWorkerTest extends WorkerTestCase {

	@Override
	public void configure(Map<String,Object> cfg) {
		cfg.put( Environment.WORKER_SCOPE, "transaction" );
		cfg.put( "hibernate.search.default.worker.execution", "sync" );
	}

}
