/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.bridge.binding.impl;

import org.hibernate.search.engine.common.tree.spi.TreeContributionListener;

class PojoTreeContributionListener implements TreeContributionListener {
	private boolean schemaContributed = false;

	@Override
	public void onNodeContributed() {
		schemaContributed = true;
	}

	boolean isAnySchemaContributed() {
		return schemaContributed;
	}
}
