/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.session.impl;

import java.lang.invoke.MethodHandles;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import javax.transaction.Synchronization;

import org.hibernate.Transaction;
import org.hibernate.search.engine.cfg.ConfigurationPropertySource;
import org.hibernate.search.engine.cfg.spi.ConfigurationProperty;
import org.hibernate.search.engine.cfg.spi.OptionalConfigurationProperty;
import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.engine.environment.bean.BeanReference;
import org.hibernate.search.engine.environment.bean.BeanResolver;
import org.hibernate.search.mapper.orm.automaticindexing.AutomaticIndexingStrategyNames;
import org.hibernate.search.mapper.orm.automaticindexing.impl.HibernateOrmIndexingQueueEventSendingPlan;
import org.hibernate.search.mapper.orm.automaticindexing.session.AutomaticIndexingSynchronizationStrategy;
import org.hibernate.search.mapper.orm.automaticindexing.session.impl.ConfiguredAutomaticIndexingSynchronizationStrategy;
import org.hibernate.search.mapper.orm.automaticindexing.spi.AutomaticIndexingConfigurationContext;
import org.hibernate.search.mapper.orm.automaticindexing.spi.AutomaticIndexingEventSendingSessionContext;
import org.hibernate.search.mapper.orm.automaticindexing.spi.AutomaticIndexingQueueEventSendingPlan;
import org.hibernate.search.mapper.orm.automaticindexing.spi.AutomaticIndexingStrategy;
import org.hibernate.search.mapper.orm.automaticindexing.spi.AutomaticIndexingStrategyStartContext;
import org.hibernate.search.mapper.orm.cfg.HibernateOrmMapperSettings;
import org.hibernate.search.mapper.orm.event.impl.HibernateOrmListenerContextProvider;
import org.hibernate.search.mapper.orm.event.impl.HibernateSearchEventListener;
import org.hibernate.search.mapper.orm.logging.impl.Log;
import org.hibernate.search.mapper.orm.mapping.impl.AutomaticIndexingStrategyPreStopContextImpl;
import org.hibernate.search.mapper.pojo.work.spi.PojoIndexingPlan;
import org.hibernate.search.mapper.pojo.work.spi.PojoIndexingQueueEventProcessingPlan;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.impl.Closer;
import org.hibernate.search.util.common.impl.SuppressingCloser;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public final class ConfiguredAutomaticIndexingStrategy {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	public static ConfiguredAutomaticIndexingStrategy create(BeanResolver beanResolver,
			ConfigurationPropertySource propertySource) {
		BeanHolder<? extends AutomaticIndexingStrategy> strategyHolder =
				AUTOMATIC_INDEXING_STRATEGY.getAndTransform( propertySource, beanResolver::resolve );
		try {
			Builder builder = new Builder( strategyHolder );
			strategyHolder.get().configure( builder );
			return builder.build();
		}
		catch (RuntimeException e) {
			new SuppressingCloser( e )
					.push( strategyHolder );
			throw e;
		}
	}

	private static final ConfigurationProperty<Boolean> AUTOMATIC_INDEXING_ENABLED =
			ConfigurationProperty.forKey( HibernateOrmMapperSettings.AutomaticIndexingRadicals.ENABLED )
					.asBoolean()
					.withDefault( HibernateOrmMapperSettings.Defaults.AUTOMATIC_INDEXING_ENABLED )
					.build();

	@SuppressWarnings("deprecation")
	private static final OptionalConfigurationProperty<Boolean> AUTOMATIC_INDEXING_ENABLED_LEGACY_STRATEGY =
			ConfigurationProperty.forKey( HibernateOrmMapperSettings.AutomaticIndexingRadicals.STRATEGY )
					.as( Boolean.class, v -> {
						try {
							return !org.hibernate.search.mapper.orm.automaticindexing.AutomaticIndexingStrategyName.NONE
									.equals( org.hibernate.search.mapper.orm.automaticindexing.AutomaticIndexingStrategyName.of( v ) );
						}
						// FIXME HSEARCH-4268 remove this when we stop configuring clustering through automatic indexing
						catch (SearchException e) {
							return true;
						}
					} )
					.build();

	@SuppressWarnings("deprecation")
	private static final ConfigurationProperty<BeanReference<? extends AutomaticIndexingStrategy>> AUTOMATIC_INDEXING_STRATEGY =
			ConfigurationProperty.forKey( HibernateOrmMapperSettings.Radicals.AUTOMATIC_INDEXING_STRATEGY )
					.asBeanReference( AutomaticIndexingStrategy.class )
					.substitute( org.hibernate.search.mapper.orm.automaticindexing.AutomaticIndexingStrategyName.NONE, AutomaticIndexingStrategyNames.NONE )
					.substitute( org.hibernate.search.mapper.orm.automaticindexing.AutomaticIndexingStrategyName.SESSION, AutomaticIndexingStrategyNames.SESSION )
					.withDefault( HibernateOrmMapperSettings.Defaults.AUTOMATIC_INDEXING_STRATEGY )
					.build();

	private static final OptionalConfigurationProperty<BeanReference<? extends AutomaticIndexingSynchronizationStrategy>> AUTOMATIC_INDEXING_SYNCHRONIZATION_STRATEGY =
			ConfigurationProperty.forKey( HibernateOrmMapperSettings.AutomaticIndexingRadicals.SYNCHRONIZATION_STRATEGY )
					.asBeanReference( AutomaticIndexingSynchronizationStrategy.class )
					.build();

	private static final ConfigurationProperty<Boolean> DIRTY_CHECK_ENABLED =
			ConfigurationProperty.forKey( HibernateOrmMapperSettings.AutomaticIndexingRadicals.ENABLE_DIRTY_CHECK )
					.asBoolean()
					.withDefault( HibernateOrmMapperSettings.Defaults.AUTOMATIC_INDEXING_ENABLE_DIRTY_CHECK )
					.build();

	private final BeanHolder<? extends AutomaticIndexingStrategy> strategyHolder;
	private final Function<AutomaticIndexingEventSendingSessionContext, AutomaticIndexingQueueEventSendingPlan> senderFactory;
	private final boolean enlistsInTransaction;

	private HibernateOrmSearchSessionMappingContext mappingContext;
	private BeanHolder<? extends AutomaticIndexingSynchronizationStrategy> defaultSynchronizationStrategyHolder;
	private ConfiguredAutomaticIndexingSynchronizationStrategy defaultSynchronizationStrategy;

	private ConfiguredAutomaticIndexingStrategy(Builder builder) {
		strategyHolder = builder.strategyHolder;
		senderFactory = builder.senderFactory;
		enlistsInTransaction = builder.enlistsInTransaction;
	}

	public boolean usesEventQueue() {
		return senderFactory != null;
	}

	// Do everything related to runtime configuration or that doesn't involve I/O
	public void preStart(HibernateOrmSearchSessionMappingContext mappingContext,
			AutomaticIndexingStrategyStartContext startContext,
			HibernateOrmListenerContextProvider contextProvider) {
		this.mappingContext = mappingContext;
		ConfigurationPropertySource configurationSource = startContext.configurationPropertySource();
		defaultSynchronizationStrategyHolder =
				AUTOMATIC_INDEXING_SYNCHRONIZATION_STRATEGY.getAndTransform( configurationSource, referenceOptional -> {
					if ( usesEventQueue() ) {
						// If we send events to a queue, we're mostly asynchronous
						// and thus configuring the synchronization strategy does not make sense.
						if ( referenceOptional.isPresent() ) {
							throw log.cannotConfigureSynchronizationStrategyWithIndexingEventQueue();
						}
						// We force the synchronization strategy to sync.
						// The commit/refresh strategies will be ignored,
						// but we're only interested in the future handler:
						// we need it to block until the sender is done pushing events to the queue.
						return BeanHolder.of( AutomaticIndexingSynchronizationStrategy.writeSync() );
					}
					else {
						return startContext.beanResolver().resolve( referenceOptional
								.orElse( HibernateOrmMapperSettings.Defaults.AUTOMATIC_INDEXING_SYNCHRONIZATION_STRATEGY ) );
					}
				} );
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

	// Do anything that couldn't be done in preStart()
	public CompletableFuture<?> start(AutomaticIndexingStrategyStartContext startContext) {
		return strategyHolder.get().start( startContext );
	}

	public CompletableFuture<?> preStop(AutomaticIndexingStrategyPreStopContextImpl context) {
		return strategyHolder.get().preStop( context );
	}

	public void stop() {
		try ( Closer<RuntimeException> closer = new Closer<>() ) {
			closer.push( BeanHolder::close, defaultSynchronizationStrategyHolder );
			defaultSynchronizationStrategy = null;
			defaultSynchronizationStrategyHolder = null;
			closer.push( AutomaticIndexingStrategy::stop, strategyHolder, BeanHolder::get );
			closer.push( BeanHolder::close, strategyHolder );
			mappingContext = null;
		}
	}

	public ConfiguredAutomaticIndexingSynchronizationStrategy defaultIndexingPlanSynchronizationStrategy() {
		return defaultSynchronizationStrategy;
	}

	public ConfiguredAutomaticIndexingSynchronizationStrategy configureOverriddenSynchronizationStrategy(
			AutomaticIndexingSynchronizationStrategy synchronizationStrategy) {
		if ( usesEventQueue() ) {
			throw log.cannotConfigureSynchronizationStrategyWithIndexingEventQueue();
		}
		ConfiguredAutomaticIndexingSynchronizationStrategy.Builder builder =
				new ConfiguredAutomaticIndexingSynchronizationStrategy.Builder( mappingContext.failureHandler(),
						mappingContext.entityReferenceFactory() );
		synchronizationStrategy.apply( builder );
		return builder.build();
	}

	public PojoIndexingPlan createIndexingPlan(HibernateOrmSearchSession context,
			ConfiguredAutomaticIndexingSynchronizationStrategy synchronizationStrategy) {
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
			ConfiguredAutomaticIndexingSynchronizationStrategy synchronizationStrategy) {
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
			ConfiguredAutomaticIndexingSynchronizationStrategy synchronizationStrategy) {
		AutomaticIndexingQueueEventSendingPlan delegate = senderFactory.apply( context );
		return mappingContext.createIndexingQueueEventProcessingPlan( context,
				synchronizationStrategy.getDocumentCommitStrategy(),
				synchronizationStrategy.getDocumentRefreshStrategy(),
				new HibernateOrmIndexingQueueEventSendingPlan( delegate ) );
	}

	private ConfiguredAutomaticIndexingSynchronizationStrategy configure(
			AutomaticIndexingSynchronizationStrategy synchronizationStrategy) {
		ConfiguredAutomaticIndexingSynchronizationStrategy.Builder builder =
				new ConfiguredAutomaticIndexingSynchronizationStrategy.Builder( mappingContext.failureHandler(),
						mappingContext.entityReferenceFactory() );
		synchronizationStrategy.apply( builder );
		return builder.build();
	}

	private static final class Builder implements AutomaticIndexingConfigurationContext {
		private final BeanHolder<? extends AutomaticIndexingStrategy> strategyHolder;
		private Function<AutomaticIndexingEventSendingSessionContext, AutomaticIndexingQueueEventSendingPlan> senderFactory;
		private boolean enlistsInTransaction = false;

		Builder(BeanHolder<? extends AutomaticIndexingStrategy> strategyHolder) {
			this.strategyHolder = strategyHolder;
		}

		@Override
		public void reindexInSession() {
			this.senderFactory = null;
			this.enlistsInTransaction = false;
		}

		@Override
		public void sendIndexingEventsTo(
				Function<AutomaticIndexingEventSendingSessionContext, AutomaticIndexingQueueEventSendingPlan> senderFactory,
				boolean enlistsInTransaction) {
			this.senderFactory = senderFactory;
			this.enlistsInTransaction = enlistsInTransaction;
		}

		public ConfiguredAutomaticIndexingStrategy build() {
			return new ConfiguredAutomaticIndexingStrategy( this );
		}
	}

}
