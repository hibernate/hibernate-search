/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.standalone.mapping.impl;

import org.hibernate.search.engine.cfg.spi.ConfigurationProperty;
import org.hibernate.search.engine.cfg.spi.OptionalConfigurationProperty;
import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.engine.environment.bean.BeanReference;
import org.hibernate.search.engine.mapper.mapping.spi.MappingStartContext;
import org.hibernate.search.mapper.pojo.plan.synchronization.IndexingPlanSynchronizationStrategy;
import org.hibernate.search.mapper.pojo.plan.synchronization.spi.ConfiguredIndexingPlanSynchronizationStrategy;
import org.hibernate.search.mapper.pojo.standalone.cfg.StandalonePojoMapperSettings;
import org.hibernate.search.mapper.pojo.standalone.common.EntityReference;
import org.hibernate.search.mapper.pojo.standalone.session.impl.StandalonePojoSearchSessionMappingContext;
import org.hibernate.search.util.common.impl.Closer;

public class ConfiguredIndexingPlanSynchronizationStrategyHolder {

	private static final OptionalConfigurationProperty<BeanReference<? extends IndexingPlanSynchronizationStrategy>> INDEXING_PLAN_SYNCHRONIZATION_STRATEGY =
			ConfigurationProperty.forKey( StandalonePojoMapperSettings.Radicals.INDEXING_PLAN_SYNCHRONIZATION_STRATEGY )
					.asBeanReference( IndexingPlanSynchronizationStrategy.class )
					.build();

	private final StandalonePojoSearchSessionMappingContext mappingContext;
	private BeanHolder<? extends IndexingPlanSynchronizationStrategy> defaultSynchronizationStrategyHolder;
	private ConfiguredIndexingPlanSynchronizationStrategy<EntityReference> defaultSynchronizationStrategy;

	public ConfiguredIndexingPlanSynchronizationStrategyHolder(
			StandalonePojoSearchSessionMappingContext mappingContext) {
		this.mappingContext = mappingContext;
	}

	public void start(MappingStartContext context) {
		this.defaultSynchronizationStrategyHolder = INDEXING_PLAN_SYNCHRONIZATION_STRATEGY.getAndTransform(
				context.configurationPropertySource(),
				referenceOptional -> context.beanResolver().resolve( referenceOptional.orElse(
						StandalonePojoMapperSettings.Defaults.INDEXING_PLAN_SYNCHRONIZATION_STRATEGY ) )
		);

		this.defaultSynchronizationStrategy = configure( defaultSynchronizationStrategyHolder.get() );
	}

	public ConfiguredIndexingPlanSynchronizationStrategy<EntityReference> defaultSynchronizationStrategy() {
		return defaultSynchronizationStrategy;
	}

	public ConfiguredIndexingPlanSynchronizationStrategy<EntityReference> configureOverriddenSynchronizationStrategy(
			IndexingPlanSynchronizationStrategy synchronizationStrategy) {
		if ( synchronizationStrategy == null ) {
			return defaultSynchronizationStrategy();
		}
		return configure( synchronizationStrategy );
	}

	private ConfiguredIndexingPlanSynchronizationStrategy<EntityReference> configure(
			IndexingPlanSynchronizationStrategy synchronizationStrategy) {
		ConfiguredIndexingPlanSynchronizationStrategy.Builder<EntityReference> builder =
				new ConfiguredIndexingPlanSynchronizationStrategy.Builder<>(
						mappingContext.failureHandler(),
						mappingContext.entityReferenceFactory()
				);
		synchronizationStrategy.apply( builder );
		return builder.build();
	}

	public void close() {
		try ( Closer<RuntimeException> closer = new Closer<>() ) {
			closer.push( BeanHolder::close, defaultSynchronizationStrategyHolder );
		}
	}
}
