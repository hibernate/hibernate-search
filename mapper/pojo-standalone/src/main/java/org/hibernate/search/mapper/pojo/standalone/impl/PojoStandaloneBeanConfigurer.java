/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.standalone.impl;

import org.hibernate.search.engine.environment.bean.BeanReference;
import org.hibernate.search.engine.environment.bean.spi.BeanConfigurationContext;
import org.hibernate.search.engine.environment.bean.spi.BeanConfigurer;
import org.hibernate.search.mapper.pojo.standalone.plan.synchronization.PojoStandaloneIndexingPlanSynchronizationStrategy;
import org.hibernate.search.mapper.pojo.standalone.plan.synchronization.PojoStandaloneIndexingPlanSynchronizationStrategyNames;

public class PojoStandaloneBeanConfigurer implements BeanConfigurer {
	@Override
	public void configure(BeanConfigurationContext context) {

		context.define(
				PojoStandaloneIndexingPlanSynchronizationStrategy.class,
				PojoStandaloneIndexingPlanSynchronizationStrategyNames.ASYNC,
				BeanReference.ofInstance( PojoStandaloneIndexingPlanSynchronizationStrategy.async() )
		);
		context.define(
				PojoStandaloneIndexingPlanSynchronizationStrategy.class,
				PojoStandaloneIndexingPlanSynchronizationStrategyNames.WRITE_SYNC,
				BeanReference.ofInstance( PojoStandaloneIndexingPlanSynchronizationStrategy.writeSync() )
		);
		context.define(
				PojoStandaloneIndexingPlanSynchronizationStrategy.class,
				PojoStandaloneIndexingPlanSynchronizationStrategyNames.READ_SYNC,
				BeanReference.ofInstance( PojoStandaloneIndexingPlanSynchronizationStrategy.readSync() )
		);
		context.define(
				PojoStandaloneIndexingPlanSynchronizationStrategy.class,
				PojoStandaloneIndexingPlanSynchronizationStrategyNames.SYNC,
				BeanReference.ofInstance( PojoStandaloneIndexingPlanSynchronizationStrategy.sync() )
		);
	}
}
