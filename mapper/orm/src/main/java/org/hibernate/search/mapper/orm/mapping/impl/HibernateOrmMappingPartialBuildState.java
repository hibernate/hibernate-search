/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.orm.mapping.impl;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.engine.mapper.mapping.building.spi.MappingFinalizationContext;
import org.hibernate.search.engine.mapper.mapping.building.spi.MappingPartialBuildState;
import org.hibernate.search.engine.mapper.mapping.spi.MappingImplementor;
import org.hibernate.search.mapper.orm.coordination.common.spi.CoordinationStrategy;
import org.hibernate.search.mapper.orm.session.impl.ConfiguredAutomaticIndexingStrategy;
import org.hibernate.search.mapper.pojo.mapping.spi.PojoMappingDelegate;
import org.hibernate.search.util.common.impl.Closer;

public class HibernateOrmMappingPartialBuildState implements MappingPartialBuildState {

	private final PojoMappingDelegate mappingDelegate;
	private final HibernateOrmTypeContextContainer.Builder typeContextContainerBuilder;
	private final BeanHolder<? extends CoordinationStrategy> coordinationStrategyHolder;
	private final ConfiguredAutomaticIndexingStrategy configuredAutomaticIndexingStrategy;

	HibernateOrmMappingPartialBuildState(PojoMappingDelegate mappingDelegate,
			HibernateOrmTypeContextContainer.Builder typeContextContainerBuilder,
			BeanHolder<? extends CoordinationStrategy> coordinationStrategyHolder,
			ConfiguredAutomaticIndexingStrategy configuredAutomaticIndexingStrategy) {
		this.mappingDelegate = mappingDelegate;
		this.typeContextContainerBuilder = typeContextContainerBuilder;
		this.coordinationStrategyHolder = coordinationStrategyHolder;
		this.configuredAutomaticIndexingStrategy = configuredAutomaticIndexingStrategy;
	}

	public MappingImplementor<HibernateOrmMapping> bindToSessionFactory(
			MappingFinalizationContext context,
			SessionFactoryImplementor sessionFactory) {
		return HibernateOrmMapping.create( mappingDelegate,
				typeContextContainerBuilder.build( mappingDelegate.typeContextProvider(), sessionFactory ),
				coordinationStrategyHolder,
				configuredAutomaticIndexingStrategy,
				sessionFactory,
				context.configurationPropertySource()
		);
	}

	@Override
	public void closeOnFailure() {
		try ( Closer<RuntimeException> closer = new Closer<>() ) {
			closer.push( ConfiguredAutomaticIndexingStrategy::stop, configuredAutomaticIndexingStrategy );
			closer.push( CoordinationStrategy::stop, coordinationStrategyHolder, BeanHolder::get );
			closer.push( BeanHolder::close, coordinationStrategyHolder );
			closer.push( PojoMappingDelegate::close, mappingDelegate );
		}
	}

}
