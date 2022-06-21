/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.pojo.schema.management.strategy;

import org.hibernate.search.mapper.pojo.standalone.schema.management.SchemaManagementStrategyName;

public class SchemaManagementStrategyDefaultIT extends SchemaManagementStrategyCreateOrValidateIT {

	@Override
	protected SchemaManagementStrategyName getStrategyName() {
		return null; // Set it to default
	}
}
