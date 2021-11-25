/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.coordination.outboxpolling.impl;

import org.hibernate.search.engine.environment.bean.BeanReference;
import org.hibernate.search.engine.environment.bean.spi.BeanConfigurationContext;
import org.hibernate.search.engine.environment.bean.spi.BeanConfigurer;
import org.hibernate.search.mapper.orm.coordination.common.spi.CooordinationStrategy;
import org.hibernate.search.mapper.orm.coordination.outboxpolling.cfg.HibernateOrmMapperOutboxPollingSettings;

public class OutboxPollingBeanConfigurer implements BeanConfigurer {

	@Override
	public void configure(BeanConfigurationContext context) {
		context.define(
				CooordinationStrategy.class,
				HibernateOrmMapperOutboxPollingSettings.COORDINATION_STRATEGY_NAME,
				BeanReference.ofInstance( new OutboxPollingCoordinationStrategy() )
		);
	}
}
