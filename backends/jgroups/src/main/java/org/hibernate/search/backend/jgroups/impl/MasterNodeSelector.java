/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.jgroups.impl;

import org.hibernate.search.exception.AssertionFailure;
import org.jgroups.Address;
import org.jgroups.Message;
import org.jgroups.View;


/**
 * <p>This {@link NodeSelectorStrategy} is a static configuration for the local
 * node to always process index operations locally, and accept index operations
 * from remote nodes configured as slaves.</p>
 *
 * <p>JGroups does not provide a persistent queue, if that level of reliability
 * is needed, use the JMS backend.</p>
 *
 * <p>This implementation matches the {@literal jgroupsMaster} configuration property.</p>
 *
 * @author Sanne Grinovero (C) 2012 Red Hat Inc.
 */
public class MasterNodeSelector implements NodeSelectorStrategy {

	@Override
	public boolean isIndexOwnerLocal() {
		return true;
	}

	@Override
	public void setLocalAddress(Address address) {
		//not needed
	}

	@Override
	public void viewAccepted(View view) {
		//nothing to do
	}

	@Override
	public Message createMessage(byte[] data) {
		throw new AssertionFailure( "A Master node should never create new Messages" );
	}

}
