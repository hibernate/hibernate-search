/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.impl;

import org.hibernate.search.engine.environment.bean.BeanReference;
import org.hibernate.search.engine.environment.bean.spi.BeanConfigurationContext;
import org.hibernate.search.engine.environment.bean.spi.BeanConfigurer;
import org.hibernate.search.mapper.pojo.work.IndexingPlanSynchronizationStrategy;
import org.hibernate.search.mapper.pojo.work.IndexingPlanSynchronizationStrategyNames;

public class PojoBaseBeanConfigurer implements BeanConfigurer {
	@Override
	public void configure(BeanConfigurationContext context) {

		context.define(
				IndexingPlanSynchronizationStrategy.class,
				IndexingPlanSynchronizationStrategyNames.ASYNC,
				BeanReference.ofInstance( IndexingPlanSynchronizationStrategy.async() )
		);
		context.define(
				IndexingPlanSynchronizationStrategy.class,
				IndexingPlanSynchronizationStrategyNames.WRITE_SYNC,
				BeanReference.ofInstance( IndexingPlanSynchronizationStrategy.writeSync() )
		);
		context.define(
				IndexingPlanSynchronizationStrategy.class,
				IndexingPlanSynchronizationStrategyNames.READ_SYNC,
				BeanReference.ofInstance( IndexingPlanSynchronizationStrategy.readSync() )
		);
		context.define(
				IndexingPlanSynchronizationStrategy.class,
				IndexingPlanSynchronizationStrategyNames.SYNC,
				BeanReference.ofInstance( IndexingPlanSynchronizationStrategy.sync() )
		);
	}
}
