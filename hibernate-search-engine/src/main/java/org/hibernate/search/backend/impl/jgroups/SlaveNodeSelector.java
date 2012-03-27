/* 
 * Hibernate, Relational Persistence for Idiomatic Java
 * 
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
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
package org.hibernate.search.backend.impl.jgroups;

import org.jgroups.Address;
import org.jgroups.Message;
import org.jgroups.View;


/**
 * <p>This {@link NodeSelectorStrategy} is a static configuration for the local
 * node to avoid processing any indexing operations locally.
 * It is assumed that some other node in the cluster will process it; which
 * node exactly is unknown, so messages are broadcasted to the group.</p>
 * 
 * <p>There is no guarantee of processing: if no master picks up the task,
 * the index update operation is skipped. This can be mitigated by making
 * sure at least one master is always online; if a persistent queue is
 * needed it's better to use the JMS backend.</p>
 * 
 * <p>This implementation matches the {@literal jgroupsSlave} configuration property.</p>
 *
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2012 Red Hat Inc.
 */
public class SlaveNodeSelector implements NodeSelectorStrategy {

	private Address localAddress;

	@Override
	public boolean isIndexOwnerLocal() {
		return false;
	}

	@Override
	public void setLocalAddress(Address address) {
		this.localAddress = address;
	}

	@Override
	public void viewAccepted(View view) {
		//nothing to do
	}

	@Override
	public Message createMessage(byte[] data) {
		return new Message( null, localAddress, data );
	}

}
