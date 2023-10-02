/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.orm.outboxpolling.impl;

import org.hibernate.search.engine.environment.bean.BeanReference;
import org.hibernate.search.engine.environment.bean.spi.BeanConfigurationContext;
import org.hibernate.search.engine.environment.bean.spi.BeanConfigurer;
import org.hibernate.search.mapper.orm.coordination.common.spi.CoordinationStrategy;
import org.hibernate.search.mapper.orm.outboxpolling.cfg.HibernateOrmMapperOutboxPollingSettings;

public class OutboxPollingBeanConfigurer implements BeanConfigurer {

	@Override
	public void configure(BeanConfigurationContext context) {
		context.define(
				CoordinationStrategy.class,
				HibernateOrmMapperOutboxPollingSettings.COORDINATION_STRATEGY_NAME,
				BeanReference.ofInstance( new OutboxPollingCoordinationStrategy() )
		);
	}
}
