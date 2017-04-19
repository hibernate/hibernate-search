/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.jgroups.impl;

import org.easymock.EasyMock;
import org.hibernate.search.backend.spi.BackendQueueProcessor;
import org.junit.Test;

/**
 * @author Sanne Grinovero (C) 2011 Red Hat Inc.
 */
public class JGroupsBackendQueueProcessorTest {

	@Test
	public void testCheckingForNullWork() {
		try {
			JGroupsBackendQueueTask queueTask = EasyMock.createNiceMock( JGroupsBackendQueueTask.class );
			BackendQueueProcessor backend = new JGroupsBackendQueueProcessor( new SlaveNodeSelector(), queueTask, null );
			backend.applyWork( null, null );
		}
		catch (IllegalArgumentException e2) {
			// this is ok, we just want to avoid other exceptions or NPEs
		}
	}

}
