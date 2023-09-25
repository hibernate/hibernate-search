/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.outboxpolling.testsupport.util;

import java.util.function.BiConsumer;

import org.hibernate.search.mapper.orm.outboxpolling.cfg.HibernateOrmMapperOutboxPollingSettings;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.CoordinationStrategyExpectations;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelperConfig;

public class OutboxPollingOrmSetupHelperConfig implements OrmSetupHelperConfig {

	@Override
	public CoordinationStrategyExpectations coordinationStrategyExpectations() {
		return CoordinationStrategyExpectations.outboxPolling();
	}

	@Override
	public void overrideHibernateSearchDefaults(BiConsumer<String, Object> propertyConsumer) {
		// Use a shorter polling interval.
		// The default 100ms is just fine in normal applications that expect asynchronous indexing,
		// but our many tests often wait for indexing to complete before carrying on,
		// and waiting for 100ms a thousand times really adds up.
		propertyConsumer.accept(
				HibernateOrmMapperOutboxPollingSettings.COORDINATION_EVENT_PROCESSOR_POLLING_INTERVAL,
				10 );
	}
}
