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
 * the cluster is selected to apply changes to the Lucene index.
 * 
 * A different {@literal NodeSelectorStrategy} can be chosen for each index.
 * 
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2012 Red Hat Inc.
 */
public interface NodeSelectorStrategy {

	/**
	 * Specifies if the current (local) node should apply
	 * changes to the index.
	 * @return
	 */
	boolean isIndexOwnerLocal();

	/**
	 * @param address
	 */
	void setLocalAddress(Address address);

	/**
	 * Invoked by JGroups on view change. {@link org.jgroups.MembershipListener#viewAccepted(View)}
	 * @param view
	 */
	void viewAccepted(View view);

	/**
	 * @param data
	 * @return
	 */
	Message createMessage(byte[] data);

}
