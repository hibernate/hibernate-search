/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.hibernate.search.test.backends;

import org.junit.Test;

import org.hibernate.search.backend.impl.blackhole.BlackHoleBackendQueueProcessor;
import org.hibernate.search.backend.impl.jgroups.JGroupsBackendQueueProcessor;
import org.hibernate.search.backend.impl.jgroups.SlaveNodeSelector;
import org.hibernate.search.backend.impl.jms.JndiJMSBackendQueueProcessor;
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
		checkBackendBehaviour( new JGroupsBackendQueueProcessor( new SlaveNodeSelector() ) );
		checkBackendBehaviour( new JndiJMSBackendQueueProcessor() );
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
