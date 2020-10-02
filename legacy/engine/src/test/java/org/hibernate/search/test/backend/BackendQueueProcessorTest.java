/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.backend;

import static org.mockito.Mockito.mock;

import java.util.Properties;

import org.hibernate.search.backend.impl.LocalBackend;
import org.hibernate.search.backend.impl.blackhole.BlackHoleBackend;
import org.hibernate.search.backend.spi.Backend;
import org.hibernate.search.backend.spi.BackendQueueProcessor;
import org.hibernate.search.indexes.spi.IndexManager;
import org.hibernate.search.spi.WorkerBuildContext;

import org.junit.Rule;
import org.junit.Test;

import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.quality.Strictness;

/**
 * @author Sanne Grinovero (C) 2011 Red Hat Inc.
 */
public class BackendQueueProcessorTest {

	@Rule
	public final MockitoRule mockito = MockitoJUnit.rule().strictness( Strictness.STRICT_STUBS );

	@Test
	public void testCheckingForNullWork() {
		checkBackendBehaviour( LocalBackend.INSTANCE );
		checkBackendBehaviour( BlackHoleBackend.INSTANCE );
	}

	private void checkBackendBehaviour(Backend backend) {
		try {
			WorkerBuildContext context = mock( WorkerBuildContext.class );
			IndexManager indexManager = mock( IndexManager.class );
			backend.initialize( new Properties(), context );
			BackendQueueProcessor processor = backend.createQueueProcessor( indexManager, context );
			processor.applyWork( null, null );
		}
		catch (IllegalArgumentException e) {
			// this is ok, we just want to avoid other exceptions or NPEs
		}
	}
}
