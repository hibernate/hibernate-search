/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.impl;

import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.engine.environment.bean.spi.BeanConfigurationContext;
import org.hibernate.search.engine.environment.bean.spi.BeanConfigurer;
import org.hibernate.search.mapper.orm.automaticindexing.session.AutomaticIndexingSynchronizationStrategyNames;
import org.hibernate.search.mapper.orm.automaticindexing.session.AutomaticIndexingSynchronizationStrategy;

public class HibernateOrmBeanConfigurer implements BeanConfigurer {
	@Override
	public void configure(BeanConfigurationContext context) {
		context.define(
				AutomaticIndexingSynchronizationStrategy.class,
				AutomaticIndexingSynchronizationStrategyNames.ASYNC,
				factoryCreationContext -> BeanHolder.of( AutomaticIndexingSynchronizationStrategy.async() )
		);
		context.define(
				AutomaticIndexingSynchronizationStrategy.class,
				AutomaticIndexingSynchronizationStrategyNames.WRITE_SYNC,
				factoryCreationContext -> BeanHolder.of( AutomaticIndexingSynchronizationStrategy.writeSync() )
		);
		context.define(
				AutomaticIndexingSynchronizationStrategy.class,
				AutomaticIndexingSynchronizationStrategyNames.READ_SYNC,
				factoryCreationContext -> BeanHolder.of( AutomaticIndexingSynchronizationStrategy.readSync() )
		);
		context.define(
				AutomaticIndexingSynchronizationStrategy.class,
				AutomaticIndexingSynchronizationStrategyNames.SYNC,
				factoryCreationContext -> BeanHolder.of( AutomaticIndexingSynchronizationStrategy.sync() )
		);
	}
}
