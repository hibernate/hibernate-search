/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.backend;

import java.util.Properties;

import org.easymock.EasyMock;
import org.hibernate.search.backend.impl.LocalBackend;
import org.hibernate.search.backend.impl.blackhole.BlackHoleBackend;
import org.hibernate.search.backend.spi.Backend;
import org.hibernate.search.backend.spi.BackendQueueProcessor;
import org.hibernate.search.indexes.spi.IndexManager;
import org.hibernate.search.spi.WorkerBuildContext;
import org.junit.Test;

/**
 * @author Sanne Grinovero (C) 2011 Red Hat Inc.
 */
public class BackendQueueProcessorTest {

	@Test
	public void testCheckingForNullWork() {
		checkBackendBehaviour( LocalBackend.INSTANCE );
		checkBackendBehaviour( BlackHoleBackend.INSTANCE );
	}

	private void checkBackendBehaviour(Backend backend) {
		try {
			WorkerBuildContext context = EasyMock.createNiceMock( WorkerBuildContext.class );
			IndexManager indexManager = EasyMock.createNiceMock( IndexManager.class );
			backend.initialize( new Properties(), context );
			BackendQueueProcessor processor = backend.createQueueProcessor( indexManager, context );
			processor.applyWork( null, null );
		}
		catch (IllegalArgumentException e) {
			// this is ok, we just want to avoid other exceptions or NPEs
		}
	}
}
