/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.jms.impl;

import org.hibernate.search.backend.spi.BackendQueueProcessor;
import org.junit.Test;

/**
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2011 Red Hat Inc.
 */
public class JndiJMSBackendQueueProcessorTest {

	@Test
	public void testCheckingForNullWork() {
		try {
			BackendQueueProcessor backend = new JndiJMSBackendQueueProcessor();
			backend.applyWork( null, null );
		}
		catch (IllegalArgumentException e2) {
			// this is ok, we just want to avoid other exceptions or NPEs
		}
	}
}
