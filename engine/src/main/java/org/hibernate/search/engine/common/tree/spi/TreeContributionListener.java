/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.common.tree.spi;

public interface TreeContributionListener {

	/**
	 * Called at least once if a node was contributed to the tree.
	 */
	void onNodeContributed();

}
