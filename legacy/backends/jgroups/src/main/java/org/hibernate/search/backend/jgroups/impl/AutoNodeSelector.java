/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.jgroups.impl;

import java.lang.invoke.MethodHandles;
import java.util.List;

import org.hibernate.search.backend.jgroups.logging.impl.Log;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.jgroups.Address;
import org.jgroups.View;


/**
 * <p>This {@link NodeSelectorStrategy} picks a single master across all nodes participating
 * in the JGroups cluster deterministically: this way all nodes in the group will
 * have an agreement on which node is going to be the master.</p>
 *
 * <p>Advantage: if the master node fails, a new node is elected.</p>
 * <p>Limitation: make sure all nodes in the group are having the same application running
 * and use the same configuration, or the master might ignore incoming messages.</p>
 *
 * @author Sanne Grinovero (C) 2012 Red Hat Inc.
 */
public final class AutoNodeSelector implements NodeSelectorStrategy {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final String indexName;
	private volatile Address localAddress;
	private volatile Address masterAddress;

	/**
	 * @param indexName the name of the index
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
		log.acceptingNewClusterView( view, masterAddress, indexName );
	}

}
