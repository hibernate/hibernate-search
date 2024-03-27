/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.orm.schema.management.strategy;

import org.hibernate.search.mapper.orm.schema.management.SchemaManagementStrategyName;

public class SchemaManagementStrategyDefaultIT extends SchemaManagementStrategyCreateOrValidateIT {

	@Override
	protected SchemaManagementStrategyName getStrategyName() {
		return null; // Set it to default
	}
}
