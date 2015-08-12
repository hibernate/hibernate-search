/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.jgroups.impl;

import java.util.concurrent.ConcurrentHashMap;

import org.jgroups.Address;
import org.jgroups.View;


/**
 * Maintains a registry of node selectors per index,
 * so that we can handle each index independently while sharing
 * the same JGroups channel.
 *
 * @author Sanne Grinovero (C) 2012 Red Hat Inc.
 */
public final class DefaultNodeSelectorService implements NodeSelectorService {

	private final ConcurrentHashMap<String,NodeSelectorStrategy> register = new ConcurrentHashMap<String,NodeSelectorStrategy>( 16, 0.75f, 2 );
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
