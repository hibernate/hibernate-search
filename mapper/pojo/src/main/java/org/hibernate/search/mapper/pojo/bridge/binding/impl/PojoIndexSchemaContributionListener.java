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