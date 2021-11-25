/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.mapping.impl;

import java.time.Clock;

import org.hibernate.search.engine.cfg.ConfigurationPropertySource;
import org.hibernate.search.engine.environment.bean.BeanResolver;
import org.hibernate.search.engine.environment.thread.spi.ThreadPoolProvider;
import org.hibernate.search.engine.mapper.mapping.spi.MappingStartContext;
import org.hibernate.search.engine.reporting.spi.ContextualFailureCollector;
import org.hibernate.search.mapper.orm.automaticindexing.spi.AutomaticIndexingMappingContext;
import org.hibernate.search.mapper.orm.coordination.common.spi.CoordinationStrategyStartContext;
import org.hibernate.search.mapper.orm.cfg.HibernateOrmMapperSettings;

public class CoordinationStrategyStartContextImpl implements CoordinationStrategyStartContext {
	private final AutomaticIndexingMappingContext mapping;
	private final MappingStartContext delegate;
	private final ConfigurationPropertySource configurationPropertySource;

	public CoordinationStrategyStartContextImpl(AutomaticIndexingMappingContext mapping,
			MappingStartContext delegate) {
		this.mapping = mapping;
		this.delegate = delegate;
		this.configurationPropertySource = delegate.configurationPropertySource()
				.withMask( HibernateOrmMapperSettings.Radicals.COORDINATION );
	}

	@Override
	public ContextualFailureCollector failureCollector() {
		return delegate.failureCollector();
	}

	@Override
	public BeanResolver beanResolver() {
		return delegate.beanResolver();
	}

	@Override
	public ConfigurationPropertySource configurationPropertySource() {
		return configurationPropertySource;
	}

	@Override
	public ThreadPoolProvider threadPoolProvider() {
		return delegate.threadPoolProvider();
	}

	@Override
	public Clock clock() {
		return Clock.systemUTC();
	}

	@Override
	public AutomaticIndexingMappingContext mapping() {
		return mapping;
	}
}
