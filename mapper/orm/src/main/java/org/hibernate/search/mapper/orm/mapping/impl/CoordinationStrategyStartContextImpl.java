/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.orm.mapping.impl;

import java.time.Clock;

import org.hibernate.search.engine.cfg.ConfigurationPropertySource;
import org.hibernate.search.engine.environment.bean.BeanResolver;
import org.hibernate.search.engine.environment.thread.spi.ThreadPoolProvider;
import org.hibernate.search.engine.mapper.mapping.spi.MappingStartContext;
import org.hibernate.search.engine.reporting.spi.ContextualFailureCollector;
import org.hibernate.search.mapper.orm.automaticindexing.spi.AutomaticIndexingMappingContext;
import org.hibernate.search.mapper.orm.cfg.HibernateOrmMapperSettings;
import org.hibernate.search.mapper.orm.coordination.common.spi.CoordinationStrategyStartContext;
import org.hibernate.search.mapper.orm.tenancy.spi.TenancyConfiguration;

public class CoordinationStrategyStartContextImpl implements CoordinationStrategyStartContext {
	private final AutomaticIndexingMappingContext mapping;
	private final MappingStartContext delegate;
	private final ConfigurationPropertySource configurationPropertySource;
	private final TenancyConfiguration tenancyConfiguration;

	public CoordinationStrategyStartContextImpl(AutomaticIndexingMappingContext mapping,
			MappingStartContext delegate, TenancyConfiguration tenancyConfiguration) {
		this.mapping = mapping;
		this.delegate = delegate;
		this.configurationPropertySource = delegate.configurationPropertySource()
				.withMask( HibernateOrmMapperSettings.Radicals.COORDINATION );
		this.tenancyConfiguration = tenancyConfiguration;
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

	@Override
	public TenancyConfiguration tenancyConfiguration() {
		return tenancyConfiguration;
	}
}
