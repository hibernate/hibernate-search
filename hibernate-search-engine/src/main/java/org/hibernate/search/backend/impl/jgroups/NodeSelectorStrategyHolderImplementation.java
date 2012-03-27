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

import java.util.concurrent.ConcurrentHashMap;

import org.jgroups.Address;
import org.jgroups.View;


/**
 * Maintains a registry of node selectors per index,
 * so that we can handle each index independently while sharing
 * the same JGroups channel.
 * 
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2012 Red Hat Inc.
 */
final class NodeSelectorStrategyHolderImplementation implements NodeSelectorStrategyHolder {

	private ConcurrentHashMap<String,NodeSelectorStrategy> register = new ConcurrentHashMap<String,NodeSelectorStrategy>( 16, 0.75f, 2 );
	private Address address;
	private View view;

	@Override
	public NodeSelectorStrategy getMasterNodeSelector(String indexName) {
		return register.get( indexName );
	}

	@Override
	public synchronized void setNodeSelectorStrategy(String indexName, NodeSelectorStrategy selector) {
		register.put( indexName, selector );
		if ( address != null ) {
			selector.setLocalAddress( address );
		}
		if ( view != null ) {
			selector.viewAccepted( view );
		}
	}

	@Override
	public synchronized void setLocalAddress(Address address) {
		this.address = address;
		for ( NodeSelectorStrategy s : register.values() ) {
			s.setLocalAddress( address );
		}
	}

	@Override
	public synchronized void viewAccepted(View view) {
		this.view = view;
		for ( NodeSelectorStrategy s : register.values() ) {
			s.viewAccepted( view );
		}
	}

}
