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
 * A {@literal NodeSelectorStrategy} represents the strategy by which a node out of
 * the JGroups cluster is selected to apply changes to the Lucene index.
 *
 * All index update operations are forwarded to the current master node, the purpose
 * of implementors is to define where messages have to be sent to, or if they
 * have to be applied by the current (local) node.
 *
 * A different {@literal NodeSelectorStrategy} can be chosen for each index.
 *
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2012 Red Hat Inc.
 */
public interface NodeSelectorStrategy {

	/**
	 * Specifies if the current (local) node should apply
	 * changes to the index.
	 */
	boolean isIndexOwnerLocal();

	/**
	 * The implementation might need to know it's own address, so this
	 * is provided at channel initial connection.
	 *
	 * @param address the local address
	 */
	void setLocalAddress(Address address);

	/**
	 * Invoked by JGroups on view change. {@link org.jgroups.MembershipListener#viewAccepted(View)}
	 *
	 * @param view contains information on the current members of the cluster group
	 */
	void viewAccepted(View view);

	/**
	 * Different message options can be applied using different constructors,
	 * hence we delegate Message construction to the strategy.
	 *
	 * @param data the information to be sent to the master.
	 * @return the message to be sent.
	 */
	Message createMessage(byte[] data);

}
