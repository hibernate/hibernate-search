/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.session.impl;

import java.lang.invoke.MethodHandles;
import java.util.function.Function;
import javax.transaction.Synchronization;

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
import org.hibernate.search.mapper.orm.logging.impl.Log;
import org.hibernate.search.mapper.pojo.work.IndexingPlanSynchronizationStrategy;
import org.hibernate.search.mapper.pojo.work.spi.ConfiguredIndexingPlanSynchronizationStrategy;
import org.hibernate.search.mapper.pojo.work.spi.PojoIndexingPlan;
import org.hibernate.search.mapper.pojo.work.spi.PojoIndexingQueueEventProcessingPlan;
import org.hibernate.search.util.common.impl.Closer;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public final class ConfiguredAutomaticIndexingStrategy {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private static final ConfigurationProperty<Boolean> AUTOMATIC_INDEXING_ENABLED =
			ConfigurationProperty.forKey( HibernateOrmMapperSettings.Radicals.AUTOMATIC_INDEXING_ENABLED )
					.asBoolean()
					.withDefault( HibernateOrmMapperSettings.Defaults.AUTOMATIC_INDEXING_ENABLED )
					.build();

	@SuppressWarnings("deprecation")
	private static final OptionalConfigurationProperty<Boolean> AUTOMATIC_INDEXING_ENABLED_LEGACY_STRATEGY =
			ConfigurationProperty.forKey( HibernateOrmMapperSettings.Radicals.AUTOMATIC_INDEXING_STRATEGY )
					.as( Boolean.class, v -> !org.hibernate.search.mapper.orm.automaticindexing.AutomaticIndexingStrategyName.NONE
							.equals( org.hibernate.search.mapper.orm.automaticindexing.AutomaticIndexingStrategyName.of( v ) ) )
					.build();

	@SuppressWarnings("deprecation")
	private static final OptionalConfigurationProperty<BeanReference<? extends org.hibernate.search.mapper.orm.automaticindexing.session.AutomaticIndexingSynchronizationStrategy>> AUTOMATIC_INDEXING_SYNCHRONIZATION_STRATEGY =
			ConfigurationProperty.forKey( HibernateOrmMapperSettings.Radicals.AUTOMATIC_INDEXING_SYNCHRONIZATION_STRATEGY )
					.asBeanReference( org.hibernate.search.mapper.orm.automaticindexing.session.AutomaticIndexingSynchronizationStrategy.class )
					.build();

	private static final OptionalConfigurationProperty<BeanReference<? extends IndexingPlanSynchronizationStrategy>> INDEXING_PLAN_SYNCHRONIZATION_STRATEGY =
			ConfigurationProperty.forKey( HibernateOrmMapperSettings.Radicals.INDEXING_PLAN_SYNCHRONIZATION_STRATEGY )
					.asBeanReference( IndexingPlanSynchronizationStrategy.class )
					.build();

	private static final ConfigurationProperty<Boolean> AUTOMATIC_INDEXING_ENABLE_DIRTY_CHECK =
			ConfigurationProperty.forKey( HibernateOrmMapperSettings.Radicals.AUTOMATIC_INDEXING_ENABLE_DIRTY_CHECK )
					.asBoolean()
					.withDefault( HibernateOrmMapperSettings.Defaults.AUTOMATIC_INDEXING_ENABLE_DIRTY_CHECK )
					.build();

	private final Function<AutomaticIndexingEventSendingSessionContext, AutomaticIndexingQueueEventSendingPlan> senderFactory;
	private final boolean enlistsInTransaction;

	private HibernateOrmSearchSessionMappingContext mappingContext;
	private BeanHolder<? extends IndexingPlanSynchronizationStrategy> defaultSynchronizationStrategyHolder;
	private ConfiguredIndexingPlanSynchronizationStrategy defaultSynchronizationStrategy;

	public ConfiguredAutomaticIndexingStrategy(
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
		if ( AUTOMATIC_INDEXING_ENABLED.get( configurationSource )
				&& AUTOMATIC_INDEXING_ENABLED_LEGACY_STRATEGY.getAndMap( configurationSource, enabled -> {
					log.automaticIndexingStrategyIsDeprecated( AUTOMATIC_INDEXING_ENABLED_LEGACY_STRATEGY.resolveOrRaw( configurationSource ),
							AUTOMATIC_INDEXING_ENABLED.resolveOrRaw( configurationSource ) );
					return enabled;
				} )
				.orElse( true ) ) {
			log.debug( "Hibernate Search event listeners activated" );
			HibernateSearchEventListener hibernateSearchEventListener = new HibernateSearchEventListener(
					contextProvider, AUTOMATIC_INDEXING_ENABLE_DIRTY_CHECK.get( startContext.configurationPropertySource() ) );
			hibernateSearchEventListener.registerTo( mappingContext.sessionFactory() );
		}
		else {
			log.debug( "Hibernate Search event listeners deactivated" );
		}
	}

	private void resolveDefaultSyncStrategyHolder(AutomaticIndexingStrategyStartContext startContext) {
		ConfigurationPropertySource configurationSource = startContext.configurationPropertySource();
		boolean legacyStrategySet = AUTOMATIC_INDEXING_SYNCHRONIZATION_STRATEGY.get( configurationSource ).isPresent();
		boolean newStrategySet = INDEXING_PLAN_SYNCHRONIZATION_STRATEGY
				.get( configurationSource ).isPresent();

		if ( legacyStrategySet && newStrategySet ) {
			throw log.bothNewAndOldConfigurationPropertiesForIndexingPlanSyncAreUsed(
					INDEXING_PLAN_SYNCHRONIZATION_STRATEGY.resolveOrRaw( configurationSource ),
					AUTOMATIC_INDEXING_SYNCHRONIZATION_STRATEGY.resolveOrRaw( configurationSource )
			);
		}

		if ( usesAsyncProcessing() ) {
			if ( legacyStrategySet || newStrategySet ) {
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
		else if ( legacyStrategySet ) {
			@SuppressWarnings("deprecation")
			BeanHolder<? extends org.hibernate.search.mapper.orm.automaticindexing.session.AutomaticIndexingSynchronizationStrategy> holder =
					// Going through the config property source again in order to get context if an error occurs.
					AUTOMATIC_INDEXING_SYNCHRONIZATION_STRATEGY.getAndMap( configurationSource, reference -> {
						log.automaticIndexingSynchronizationStrategyIsDeprecated( AUTOMATIC_INDEXING_SYNCHRONIZATION_STRATEGY.resolveOrRaw( configurationSource ),
								INDEXING_PLAN_SYNCHRONIZATION_STRATEGY.resolveOrRaw( configurationSource ) );
						return startContext.beanResolver().resolve( reference );
					} )
					.get(); // We know this optional is not empty
			defaultSynchronizationStrategyHolder = BeanHolder.of(
					new HibernateOrmIndexingPlanSynchronizationStrategyAdapter( holder.get() )
			).withDependencyAutoClosing( holder );
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
			throw log.cannotConfigureSynchronizationStrategyWithIndexingEventQueue();
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
			HibernateOrmSearchSessionHolder sessionProperties,
			Transaction transactionIdentifier,
			ConfiguredIndexingPlanSynchronizationStrategy synchronizationStrategy) {
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
