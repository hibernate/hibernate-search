/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.coordination.outboxpolling;

import org.hibernate.search.util.impl.integrationtest.mapper.orm.CoordinationStrategyExpectations;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelperConfig;

public class OutboxPollingOrmSetupHelperConfig implements OrmSetupHelperConfig {

	@Override
	public CoordinationStrategyExpectations coordinationStrategyExpectations() {
		return CoordinationStrategyExpectations.outboxPolling();
	}
}
