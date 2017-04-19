/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.jgroups.impl;

import org.hibernate.search.indexes.spi.IndexManager;

/**
 * @author Yoann Rodiere
 */
public class JGroupsMasterBackend extends JGroupsBackend {

	@Override
	protected NodeSelectorStrategy createNodeSelectorStrategy(IndexManager indexManager) {
		return new MasterNodeSelector();
	}

}
