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

import java.util.List;

import org.jgroups.Address;
import org.jgroups.Message;
import org.jgroups.View;


/**
 * <p>This {@link org.hibernate.search.backend.impl.jgroups.NodeSelectorStrategy} picks a single master across all nodes participating
 * in the JGroups cluster deterministically: this way all nodes in the group will
 * have an agreement on which node is going to be the master.</p>
 *
 * <p>Advantage: if the master node fails, a new node is elected.</p>
 * <p>Limitation: make sure all nodes in the group are having the same application running
 * and use the same configuration, or the master might ignore incoming messages.</p>
 *
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2012 Red Hat Inc.
 */
public class AutoNodeSelector implements NodeSelectorStrategy {

	private final String indexName;
	private volatile Address localAddress;
	private volatile Address masterAddress;

	/**
	 * @param indexName
	 */
	public AutoNodeSelector(String indexName) {
		this.indexName = indexName;
	}

	@Override
	public boolean isIndexOwnerLocal() {
		return localAddress == null || localAddress.equals( masterAddress );
	}

	@Override
	public void setLocalAddress(Address address) {
		localAddress = address;
	}

	@Override
	public void viewAccepted(View view) {
		List<Address> members = view.getMembers();
		if ( members.size() == 1 ) {
			masterAddress = members.get( 0 );
		}
		else if ( members.size() == 2 ) {
			// pick the non-coordinator
			masterAddress = members.get( 1 );
		}
		else {
			// exclude cluster coordinator (the first)
			int selectionRange = members.size() - 1;
			int selected = Math.abs( indexName.hashCode() % selectionRange ) + 1;
			masterAddress = members.get( selected );
		}
	}

	@Override
	public Message createMessage(byte[] data) {
		return new Message( null, localAddress, data );
	}

}
