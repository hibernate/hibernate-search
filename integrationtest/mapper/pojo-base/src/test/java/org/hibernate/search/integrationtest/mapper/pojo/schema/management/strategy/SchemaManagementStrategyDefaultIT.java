package org.hibernate.search.integrationtest.mapper.pojo.schema.management.strategy;

import org.hibernate.search.mapper.pojo.standalone.schema.management.SchemaManagementStrategyName;

class SchemaManagementStrategyDefaultIT extends SchemaManagementStrategyCreateOrValidateIT {

	@Override
	protected SchemaManagementStrategyName getStrategyName() {
		return null; // Set it to default
	}
}
