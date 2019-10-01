/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.bridge.binding.impl;

import org.hibernate.search.engine.mapper.mapping.building.spi.IndexSchemaContributionListener;

class PojoIndexSchemaContributionListener implements IndexSchemaContributionListener {
	private boolean schemaContributed = false;

	@Override
	public void onSchemaContributed() {
		schemaContributed = true;
	}

	boolean isAnySchemaContributed() {
		return schemaContributed;
	}
}