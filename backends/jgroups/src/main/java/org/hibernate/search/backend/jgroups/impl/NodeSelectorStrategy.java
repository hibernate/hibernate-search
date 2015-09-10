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
 * A {@literal NodeSelectorStrategy} represents the strategy by which a node out of
 * the JGroups cluster is selected to apply changes to the Lucene index.
 *
 * All index update operations are forwarded to the current master node, the purpose
 * of implementors is to define where messages have to be sent to, or if they
 * have to be applied by the current (local) node.
 *
 * A different {@literal NodeSelectorStrategy} can be chosen for each index.
 *
 * @author Sanne Grinovero (C) 2012 Red Hat Inc.
 */
public interface NodeSelectorStrategy {

	/**
	 * Specifies if the current (local) node should apply
	 * changes to the index.
	 *
	 * @return true if the local node is the owner
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
