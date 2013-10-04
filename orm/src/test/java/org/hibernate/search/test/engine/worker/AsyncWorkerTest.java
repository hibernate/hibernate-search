/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat, Inc. and/or its affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
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
