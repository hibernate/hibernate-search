/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.session.impl;

import java.lang.invoke.MethodHandles;
import java.util.Optional;
import java.util.function.Function;
import javax.transaction.Synchronization;

import org.hibernate.Transaction;
import org.hibernate.search.engine.cfg.ConfigurationPropertySource;
import org.hibernate.search.engine.cfg.spi.ConfigurationProperty;
import org.hibernate.search.engine.cfg.spi.OptionalConfigurationProperty;
import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.engine.environment.bean.BeanReference;
import org.hibernate.search.engine.mapper.mapping.spi.MappingStartContext;
import org.hibernate.search.mapper.orm.automaticindexing.impl.HibernateOrmIndexingQueueEventSendingPlan;
import org.hibernate.search.mapper.orm.automaticindexing.spi.AutomaticIndexingEventSendingSessionContext;
import org.hibernate.search.mapper.orm.automaticindexing.spi.AutomaticIndexingQueueEventSendingPlan;
import org.hibernate.search.mapper.orm.automaticindexing.spi.AutomaticIndexingStrategyStartContext;
import org.hibernate.search.mapper.orm.cfg.HibernateOrmMapperSettings;
import org.hibernate.search.mapper.orm.common.EntityReference;
import org.hibernate.search.mapper.orm.event.impl.HibernateOrmListenerContextProvider;
import org.hibernate.search.mapper.orm.event.impl.HibernateSearchEventListener;
import org.hibernate.search.mapper.orm.logging.impl.Log;
import org.hibernate.search.mapper.pojo.plan.synchronization.IndexingPlanSynchronizationStrategy;
import org.hibernate.search.mapper.pojo.plan.synchronization.impl.ConfiguredIndexingPlanSynchronizationStrategy;
import org.hibernate.search.mapper.pojo.work.spi.PojoIndexingPlan;
import org.hibernate.search.mapper.pojo.work.spi.PojoIndexingQueueEventProcessingPlan;
import org.hibernate.search.util.common.impl.Closer;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public final class ConfiguredAutomaticIndexingStrategy {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private static final ConfigurationProperty<Boolean> AUTOMATIC_INDEXING_ENABLED =
			ConfigurationProperty.forKey( HibernateOrmMapperSettings.AutomaticIndexingRadicals.ENABLED )
					.asBoolean()
					.withDefault( HibernateOrmMapperSettings.Defaults.AUTOMATIC_INDEXING_ENABLED )
					.build();

	@SuppressWarnings("deprecation")
	private static final OptionalConfigurationProperty<Boolean> AUTOMATIC_INDEXING_ENABLED_LEGACY_STRATEGY =
			ConfigurationProperty.forKey( HibernateOrmMapperSettings.AutomaticIndexingRadicals.STRATEGY )
					.as( Boolean.class, v -> !org.hibernate.search.mapper.orm.automaticindexing.AutomaticIndexingStrategyName.NONE
							.equals( org.hibernate.search.mapper.orm.automaticindexing.AutomaticIndexingStrategyName.of( v ) ) )
					.build();

	@SuppressWarnings("deprecation")
	private static final OptionalConfigurationProperty<BeanReference<? extends org.hibernate.search.mapper.orm.automaticindexing.session.AutomaticIndexingSynchronizationStrategy>> AUTOMATIC_INDEXING_SYNCHRONIZATION_STRATEGY =
			ConfigurationProperty.forKey( HibernateOrmMapperSettings.AutomaticIndexingRadicals.SYNCHRONIZATION_STRATEGY )
					.asBeanReference( org.hibernate.search.mapper.orm.automaticindexing.session.AutomaticIndexingSynchronizationStrategy.class )
					.build();

	private static final OptionalConfigurationProperty<BeanReference<? extends IndexingPlanSynchronizationStrategy>> INDEXING_PLAN_SYNCHRONIZATION_STRATEGY =
			ConfigurationProperty.forKey( HibernateOrmMapperSettings.Radicals.INDEXING_PLAN_SYNCHRONIZATION_STRATEGY )
					.asBeanReference( IndexingPlanSynchronizationStrategy.class )
					.build();

	private static final ConfigurationProperty<Boolean> DIRTY_CHECK_ENABLED =
			ConfigurationProperty.forKey( HibernateOrmMapperSettings.AutomaticIndexingRadicals.ENABLE_DIRTY_CHECK )
					.asBoolean()
					.withDefault( HibernateOrmMapperSettings.Defaults.AUTOMATIC_INDEXING_ENABLE_DIRTY_CHECK )
					.build();

	private final Function<AutomaticIndexingEventSendingSessionContext, AutomaticIndexingQueueEventSendingPlan> senderFactory;
	private final boolean enlistsInTransaction;

	private HibernateOrmSearchSessionMappingContext mappingContext;
	private BeanHolder<? extends IndexingPlanSynchronizationStrategy> defaultSynchronizationStrategyHolder;
	private ConfiguredIndexingPlanSynchronizationStrategy<EntityReference> defaultSynchronizationStrategy;

	public ConfiguredAutomaticIndexingStrategy(
			Function<AutomaticIndexingEventSendingSessionContext, AutomaticIndexingQueueEventSendingPlan> senderFactory,
			boolean enlistsInTransaction) {
		this.senderFactory = senderFactory;
		this.enlistsInTransaction = enlistsInTransaction;
	}

	public boolean usesEventQueue() {
		return senderFactory != null;
	}

	// Do everything related to runtime configuration or that doesn't involve I/O
	public void start(HibernateOrmSearchSessionMappingContext mappingContext,
			MappingStartContext mappingStartContext,
			AutomaticIndexingStrategyStartContext startContext,
			HibernateOrmListenerContextProvider contextProvider) {
		this.mappingContext = mappingContext;
		ConfigurationPropertySource configurationSource = startContext.configurationPropertySource();

		resolveDefaultSyncStrategyHolder( mappingStartContext, startContext, configurationSource );

		defaultSynchronizationStrategy = configure( defaultSynchronizationStrategyHolder.get() );
		if ( AUTOMATIC_INDEXING_ENABLED.get( configurationSource )
				&& AUTOMATIC_INDEXING_ENABLED_LEGACY_STRATEGY.getAndMap( configurationSource, enabled -> {
					log.automaticIndexingStrategyIsDeprecated( AUTOMATIC_INDEXING_ENABLED_LEGACY_STRATEGY.resolveOrRaw( configurationSource ),
							AUTOMATIC_INDEXING_ENABLED.resolveOrRaw( configurationSource ) );
					return enabled;
				} )
				.orElse( true ) ) {
			log.debug( "Hibernate Search event listeners activated" );
			HibernateSearchEventListener hibernateSearchEventListener = new HibernateSearchEventListener(
					contextProvider, DIRTY_CHECK_ENABLED.get( startContext.configurationPropertySource() ) );
			hibernateSearchEventListener.registerTo( mappingContext.sessionFactory() );
		}
		else {
			log.debug( "Hibernate Search event listeners deactivated" );
		}
	}

	private void resolveDefaultSyncStrategyHolder(MappingStartContext mappingStartContext, AutomaticIndexingStrategyStartContext startContext,
			ConfigurationPropertySource configurationSource) {
		@SuppressWarnings("deprecation")
		Optional<BeanReference<? extends org.hibernate.search.mapper.orm.automaticindexing.session.AutomaticIndexingSynchronizationStrategy>> automaticBeanReference = AUTOMATIC_INDEXING_SYNCHRONIZATION_STRATEGY.get(
				configurationSource );
		Optional<BeanReference<? extends IndexingPlanSynchronizationStrategy>> planBeanReference = INDEXING_PLAN_SYNCHRONIZATION_STRATEGY.get(
				mappingStartContext.configurationPropertySource() );

		if ( automaticBeanReference.isPresent() && planBeanReference.isPresent() ) {
			@SuppressWarnings("deprecation")
			String deprecatedProperty = HibernateOrmMapperSettings.AUTOMATIC_INDEXING_SYNCHRONIZATION_STRATEGY;
			throw log.bothNewAndOldConfigurationPropertiesForIndexingPlanSyncAreUsed(
					HibernateOrmMapperSettings.INDEXING_PLAN_SYNCHRONIZATION_STRATEGY, deprecatedProperty
			);
		}

		if ( usesEventQueue() ) {
			if ( ( automaticBeanReference.isPresent() || planBeanReference.isPresent() ) ) {
				// If we send events to a queue, we're mostly asynchronous
				// and thus configuring the synchronization strategy does not make sense.
				throw log.cannotConfigureSynchronizationStrategyWithIndexingEventQueue();
			}

			// We force the synchronization strategy to sync.
			// The commit/refresh strategies will be ignored,
			// but we're only interested in the future handler:
			// we need it to block until the sender is done pushing events to the queue.
			defaultSynchronizationStrategyHolder = BeanHolder.of( IndexingPlanSynchronizationStrategy.writeSync() );
		}
		else if ( automaticBeanReference.isPresent() ) {
			try {
				@SuppressWarnings("deprecation")
				BeanHolder<? extends org.hibernate.search.mapper.orm.automaticindexing.session.AutomaticIndexingSynchronizationStrategy> holder = startContext.beanResolver()
						.resolve( automaticBeanReference.get() );

				defaultSynchronizationStrategyHolder = BeanHolder.of(
						new IndexingPlanSynchronizationStrategyAdapter( holder.get() )
				).withDependencyAutoClosing( holder );
			}
			catch (Exception e) {
				@SuppressWarnings("deprecation")
				String deprecatedProperty = HibernateOrmMapperSettings.AUTOMATIC_INDEXING_SYNCHRONIZATION_STRATEGY;
				throw log.unableToConvertConfigurationProperty(
						deprecatedProperty,
						e.getMessage(),
						e
				);
			}
		}
		else {
			try {
				defaultSynchronizationStrategyHolder = startContext.beanResolver().resolve(
						planBeanReference
								.orElse( HibernateOrmMapperSettings.Defaults.INDEXING_PLAN_SYNCHRONIZATION_STRATEGY )
				);
			}
			catch (Exception e) {
				throw log.unableToConvertConfigurationProperty(
						HibernateOrmMapperSettings.INDEXING_PLAN_SYNCHRONIZATION_STRATEGY,
						e.getMessage(),
						e
				);
			}
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

	public ConfiguredIndexingPlanSynchronizationStrategy<EntityReference> defaultIndexingPlanSynchronizationStrategy() {
		return defaultSynchronizationStrategy;
	}

	public ConfiguredIndexingPlanSynchronizationStrategy<EntityReference> configureOverriddenSynchronizationStrategy(
			IndexingPlanSynchronizationStrategy synchronizationStrategy) {
		if ( usesEventQueue() ) {
			throw log.cannotConfigureSynchronizationStrategyWithIndexingEventQueue();
		}
		ConfiguredIndexingPlanSynchronizationStrategy.Builder<EntityReference> builder =
				new ConfiguredIndexingPlanSynchronizationStrategy.Builder<>( mappingContext.failureHandler(),
						mappingContext.entityReferenceFactory() );
		synchronizationStrategy.apply( builder );
		return builder.build();
	}

	public PojoIndexingPlan createIndexingPlan(HibernateOrmSearchSession context,
			ConfiguredIndexingPlanSynchronizationStrategy<EntityReference> synchronizationStrategy) {
		if ( usesEventQueue() ) {
			AutomaticIndexingQueueEventSendingPlan delegate = senderFactory.apply( context );
			return mappingContext.createIndexingPlan( context, new HibernateOrmIndexingQueueEventSendingPlan( delegate ) );
		}
		else {
			return mappingContext.createIndexingPlan( context,
					synchronizationStrategy.getDocumentCommitStrategy(),
					synchronizationStrategy.getDocumentRefreshStrategy() );
		}
	}

	public Synchronization createTransactionWorkQueueSynchronization(PojoIndexingPlan indexingPlan,
			HibernateOrmSearchSessionHolder sessionProperties,
			Transaction transactionIdentifier,
			ConfiguredIndexingPlanSynchronizationStrategy<EntityReference> synchronizationStrategy) {
		if ( enlistsInTransaction ) {
			return new BeforeCommitIndexingPlanSynchronization( indexingPlan, sessionProperties, transactionIdentifier,
					synchronizationStrategy );
		}
		else {
			return new AfterCommitIndexingPlanSynchronization( indexingPlan, sessionProperties, transactionIdentifier,
					synchronizationStrategy );
		}
	}

	public PojoIndexingQueueEventProcessingPlan createIndexingQueueEventProcessingPlan(HibernateOrmSearchSession context,
			ConfiguredIndexingPlanSynchronizationStrategy<EntityReference> synchronizationStrategy) {
		AutomaticIndexingQueueEventSendingPlan delegate = senderFactory.apply( context );
		return mappingContext.createIndexingQueueEventProcessingPlan( context,
				synchronizationStrategy.getDocumentCommitStrategy(),
				synchronizationStrategy.getDocumentRefreshStrategy(),
				new HibernateOrmIndexingQueueEventSendingPlan( delegate ) );
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

}
