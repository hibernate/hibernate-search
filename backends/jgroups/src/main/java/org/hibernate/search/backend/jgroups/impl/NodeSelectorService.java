/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.jgroups.impl;

import org.hibernate.search.engine.service.spi.Service;
import org.jgroups.Address;
import org.jgroups.View;


/**
 * Contains the {@link NodeSelectorStrategy} selected for each index,
 * or returns the default one.
 * Chosen strategies can be changed at any time.
 *
 * Implementors need to be threadsafe.
 *
 * @author Sanne Grinovero (C) 2012 Red Hat Inc.
 */
public interface NodeSelectorService extends Service {
	NodeSelectorStrategy getMasterNodeSelector(String indexName);

	void setNodeSelectorStrategy(String indexName, NodeSelectorStrategy selector);

	void setLocalAddress(Address address);

	void viewAccepted(View view);

}
