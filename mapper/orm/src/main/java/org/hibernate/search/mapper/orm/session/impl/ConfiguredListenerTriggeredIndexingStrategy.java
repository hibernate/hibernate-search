/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.orm.session.impl;

import java.util.function.Function;

import jakarta.transaction.Synchronization;

import org.hibernate.Transaction;
import org.hibernate.search.engine.cfg.ConfigurationPropertySource;
import org.hibernate.search.engine.cfg.spi.ConfigurationProperty;
import org.hibernate.search.engine.cfg.spi.OptionalConfigurationProperty;
import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.engine.environment.bean.BeanReference;
import org.hibernate.search.mapper.orm.automaticindexing.impl.AutomaticIndexingStrategyStartContext;
import org.hibernate.search.mapper.orm.automaticindexing.impl.HibernateOrmIndexingQueueEventSendingPlan;
import org.hibernate.search.mapper.orm.automaticindexing.spi.AutomaticIndexingEventSendingSessionContext;
import org.hibernate.search.mapper.orm.automaticindexing.spi.AutomaticIndexingQueueEventSendingPlan;
import org.hibernate.search.mapper.orm.cfg.HibernateOrmMapperSettings;
import org.hibernate.search.mapper.orm.event.impl.HibernateOrmListenerContextProvider;
import org.hibernate.search.mapper.orm.event.impl.HibernateSearchEventListener;
import org.hibernate.search.mapper.orm.logging.impl.ConfigurationLog;
import org.hibernate.search.mapper.pojo.work.IndexingPlanSynchronizationStrategy;
import org.hibernate.search.mapper.pojo.work.spi.ConfiguredIndexingPlanSynchronizationStrategy;
import org.hibernate.search.mapper.pojo.work.spi.PojoIndexingPlan;
import org.hibernate.search.mapper.pojo.work.spi.PojoIndexingQueueEventProcessingPlan;
import org.hibernate.search.util.common.impl.Closer;

public final class ConfiguredListenerTriggeredIndexingStrategy {

	private static final ConfigurationProperty<Boolean> INDEXING_LISTENERS_ENABLED =
			ConfigurationProperty.forKey( HibernateOrmMapperSettings.Radicals.INDEXING_LISTENERS_ENABLED )
					.asBoolean()
					.withDefault( HibernateOrmMapperSettings.Defaults.INDEXING_LISTENERS_ENABLED )
					.build();

	private static final OptionalConfigurationProperty<
			BeanReference<? extends IndexingPlanSynchronizationStrategy>> INDEXING_PLAN_SYNCHRONIZATION_STRATEGY =
					ConfigurationProperty.forKey( HibernateOrmMapperSettings.Radicals.INDEXING_PLAN_SYNCHRONIZATION_STRATEGY )
							.asBeanReference( IndexingPlanSynchronizationStrategy.class )
							.build();

	private final Function<AutomaticIndexingEventSendingSessionContext, AutomaticIndexingQueueEventSendingPlan> senderFactory;
	private final boolean enlistsInTransaction;

	private HibernateOrmSearchSessionMappingContext mappingContext;
	private BeanHolder<? extends IndexingPlanSynchronizationStrategy> defaultSynchronizationStrategyHolder;
	private ConfiguredIndexingPlanSynchronizationStrategy defaultSynchronizationStrategy;

	public ConfiguredListenerTriggeredIndexingStrategy(
			Function<AutomaticIndexingEventSendingSessionContext, AutomaticIndexingQueueEventSendingPlan> senderFactory,
			boolean enlistsInTransaction) {
		this.senderFactory = senderFactory;
		this.enlistsInTransaction = enlistsInTransaction;
	}

	public boolean usesAsyncProcessing() {
		return senderFactory != null;
	}

	// Do everything related to runtime configuration or that doesn't involve I/O
	public void start(HibernateOrmSearchSessionMappingContext mappingContext,
			AutomaticIndexingStrategyStartContext startContext,
			HibernateOrmListenerContextProvider contextProvider) {
		this.mappingContext = mappingContext;
		ConfigurationPropertySource configurationSource = startContext.configurationPropertySource();

		resolveDefaultSyncStrategyHolder( startContext );

		defaultSynchronizationStrategy = configure( defaultSynchronizationStrategyHolder.get() );

		if ( INDEXING_LISTENERS_ENABLED.get( configurationSource ) ) {
			ConfigurationLog.INSTANCE.hibernateSearchListenerEnabled();
			HibernateSearchEventListener hibernateSearchEventListener =
					new HibernateSearchEventListener( contextProvider, true );
			hibernateSearchEventListener.registerTo( mappingContext.sessionFactory() );
		}
		else {
			ConfigurationLog.INSTANCE.hibernateSearchListenerDisabled();
		}
	}

	private void resolveDefaultSyncStrategyHolder(AutomaticIndexingStrategyStartContext startContext) {
		ConfigurationPropertySource configurationSource = startContext.configurationPropertySource();
		boolean syncStrategySet = INDEXING_PLAN_SYNCHRONIZATION_STRATEGY.get( configurationSource ).isPresent();

		if ( usesAsyncProcessing() ) {
			if ( syncStrategySet ) {
				// If we send events to a queue, we're mostly asynchronous
				// and thus configuring the synchronization strategy does not make sense.
				throw ConfigurationLog.INSTANCE.cannotConfigureSynchronizationStrategyWithIndexingEventQueue();
			}

			// We force the synchronization strategy to sync.
			// The commit/refresh strategies will be ignored,
			// but we're only interested in the future handler:
			// we need it to block until the sender is done pushing events to the queue.
			defaultSynchronizationStrategyHolder = BeanHolder.of( IndexingPlanSynchronizationStrategy.writeSync() );
		}
		else {
			// Going through the config property source again in order to get context if an error occurs.
			defaultSynchronizationStrategyHolder = INDEXING_PLAN_SYNCHRONIZATION_STRATEGY.getAndTransform(
					configurationSource,
					referenceOptional -> startContext.beanResolver().resolve( referenceOptional
							.orElse( HibernateOrmMapperSettings.Defaults.INDEXING_PLAN_SYNCHRONIZATION_STRATEGY ) )
			);
		}
	}

	public void stop() {
		try ( Closer<RuntimeException> closer = new Closer<>() ) {
			closer.push( BeanHolder::close, defaultSynchronizationStrategyHolder );
			defaultSynchronizationStrategy = null;
			defaultSynchronizationStrategyHolder = null;
			mappingContext = null;
		}
	}

	public ConfiguredIndexingPlanSynchronizationStrategy defaultIndexingPlanSynchronizationStrategy() {
		return defaultSynchronizationStrategy;
	}

	public ConfiguredIndexingPlanSynchronizationStrategy configureOverriddenSynchronizationStrategy(
			IndexingPlanSynchronizationStrategy synchronizationStrategy) {
		if ( usesAsyncProcessing() ) {
			throw ConfigurationLog.INSTANCE.cannotConfigureSynchronizationStrategyWithIndexingEventQueue();
		}

		return configure( synchronizationStrategy );
	}

	public PojoIndexingPlan createIndexingPlan(HibernateOrmSearchSession context,
			ConfiguredIndexingPlanSynchronizationStrategy synchronizationStrategy) {
		if ( usesAsyncProcessing() ) {
			AutomaticIndexingQueueEventSendingPlan delegate = senderFactory.apply( context );
			return mappingContext.createIndexingPlan( context, new HibernateOrmIndexingQueueEventSendingPlan( delegate ) );
		}
		else {
			return mappingContext.createIndexingPlan( context,
					synchronizationStrategy.documentCommitStrategy(),
					synchronizationStrategy.documentRefreshStrategy() );
		}
	}

	public Synchronization createTransactionWorkQueueSynchronization(PojoIndexingPlan indexingPlan,
			HibernateOrmSearchSessionExtension sessionExtension,
			Transaction transactionIdentifier,
			ConfiguredIndexingPlanSynchronizationStrategy synchronizationStrategy) {
		if ( enlistsInTransaction ) {
			return new BeforeCommitIndexingPlanSynchronization( indexingPlan, sessionExtension, transactionIdentifier,
					synchronizationStrategy );
		}
		else {
			return new AfterCommitIndexingPlanSynchronization( indexingPlan, sessionExtension, transactionIdentifier,
					synchronizationStrategy );
		}
	}

	public PojoIndexingQueueEventProcessingPlan createIndexingQueueEventProcessingPlan(HibernateOrmSearchSession context,
			ConfiguredIndexingPlanSynchronizationStrategy synchronizationStrategy) {
		AutomaticIndexingQueueEventSendingPlan delegate = senderFactory.apply( context );
		return mappingContext.createIndexingQueueEventProcessingPlan( context,
				synchronizationStrategy.documentCommitStrategy(),
				synchronizationStrategy.documentRefreshStrategy(),
				new HibernateOrmIndexingQueueEventSendingPlan( delegate ) );
	}

	private ConfiguredIndexingPlanSynchronizationStrategy configure(
			IndexingPlanSynchronizationStrategy synchronizationStrategy) {
		ConfiguredIndexingPlanSynchronizationStrategy.Builder builder =
				new ConfiguredIndexingPlanSynchronizationStrategy.Builder(
						mappingContext.failureHandler()
				);
		synchronizationStrategy.apply( builder );
		return builder.build();
	}

}
