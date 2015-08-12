/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.jgroups.impl;

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
 * @author Sanne Grinovero (C) 2012 Red Hat Inc.
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
