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

import org.hibernate.annotations.common.AssertionFailure;
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
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2012 Red Hat Inc.
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
