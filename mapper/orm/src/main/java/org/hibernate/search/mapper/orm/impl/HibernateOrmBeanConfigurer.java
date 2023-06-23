/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.impl;

import org.hibernate.search.engine.environment.bean.BeanReference;
import org.hibernate.search.engine.environment.bean.spi.BeanConfigurationContext;
import org.hibernate.search.engine.environment.bean.spi.BeanConfigurer;
import org.hibernate.search.mapper.orm.coordination.common.spi.CoordinationStrategy;
import org.hibernate.search.mapper.orm.coordination.impl.NoCoordinationStrategy;

public class HibernateOrmBeanConfigurer implements BeanConfigurer {
	@Override
	@SuppressWarnings("deprecation")
	public void configure(BeanConfigurationContext context) {
		context.define(
				CoordinationStrategy.class,
				NoCoordinationStrategy.NAME,
				BeanReference.ofInstance( new NoCoordinationStrategy() )
		);

		context.define(
				org.hibernate.search.mapper.orm.automaticindexing.session.AutomaticIndexingSynchronizationStrategy.class,
				org.hibernate.search.mapper.orm.automaticindexing.session.AutomaticIndexingSynchronizationStrategyNames.ASYNC,
				BeanReference.ofInstance(
						org.hibernate.search.mapper.orm.automaticindexing.session.AutomaticIndexingSynchronizationStrategy
								.async() )
		);
		context.define(
				org.hibernate.search.mapper.orm.automaticindexing.session.AutomaticIndexingSynchronizationStrategy.class,
				org.hibernate.search.mapper.orm.automaticindexing.session.AutomaticIndexingSynchronizationStrategyNames.WRITE_SYNC,
				BeanReference.ofInstance(
						org.hibernate.search.mapper.orm.automaticindexing.session.AutomaticIndexingSynchronizationStrategy
								.writeSync() )
		);
		context.define(
				org.hibernate.search.mapper.orm.automaticindexing.session.AutomaticIndexingSynchronizationStrategy.class,
				org.hibernate.search.mapper.orm.automaticindexing.session.AutomaticIndexingSynchronizationStrategyNames.READ_SYNC,
				BeanReference.ofInstance(
						org.hibernate.search.mapper.orm.automaticindexing.session.AutomaticIndexingSynchronizationStrategy
								.readSync() )
		);
		context.define(
				org.hibernate.search.mapper.orm.automaticindexing.session.AutomaticIndexingSynchronizationStrategy.class,
				org.hibernate.search.mapper.orm.automaticindexing.session.AutomaticIndexingSynchronizationStrategyNames.SYNC,
				BeanReference.ofInstance(
						org.hibernate.search.mapper.orm.automaticindexing.session.AutomaticIndexingSynchronizationStrategy
								.sync() )
		);
	}
}
