/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.backend;

import org.junit.Test;

import org.hibernate.search.backend.impl.blackhole.BlackHoleBackendQueueProcessor;
import org.hibernate.search.backend.impl.lucene.LuceneBackendQueueProcessor;
import org.hibernate.search.backend.spi.BackendQueueProcessor;

/**
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2011 Red Hat Inc.
 */
public class BackendQueueProcessorTest {

	@Test
	public void testCheckingForNullWork() {
		checkBackendBehaviour( new LuceneBackendQueueProcessor() );
		checkBackendBehaviour( new BlackHoleBackendQueueProcessor() );
	}

	private void checkBackendBehaviour(BackendQueueProcessor backend) {
		try {
			backend.applyWork( null, null );
		}
		catch (IllegalArgumentException e) {
			// this is ok, we just want to avoid other exceptions or NPEs
		}
	}
}
