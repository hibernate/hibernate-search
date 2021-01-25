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
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.search.engine.cfg.spi.ConfigurationProperty;
import org.hibernate.search.engine.cfg.spi.ConfigurationPropertySource;
import org.hibernate.search.engine.cfg.spi.OptionalConfigurationProperty;
import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.engine.environment.bean.BeanReference;
import org.hibernate.search.engine.environment.bean.BeanResolver;
import org.hibernate.search.mapper.orm.automaticindexing.impl.HibernateOrmIndexingQueueEventSendingPlan;
import org.hibernate.search.mapper.orm.automaticindexing.session.AutomaticIndexingSynchronizationStrategy;
import org.hibernate.search.mapper.orm.automaticindexing.session.impl.ConfiguredAutomaticIndexingSynchronizationStrategy;
import org.hibernate.search.mapper.orm.automaticindexing.spi.AutomaticIndexingConfigurationContext;
import org.hibernate.search.mapper.orm.automaticindexing.spi.AutomaticIndexingEventSendingSessionContext;
import org.hibernate.search.mapper.orm.automaticindexing.spi.AutomaticIndexingQueueEventSendingPlan;
import org.hibernate.search.mapper.orm.cfg.HibernateOrmMapperSettings;
import org.hibernate.search.mapper.orm.event.impl.HibernateOrmListenerContextProvider;
import org.hibernate.search.mapper.orm.event.impl.HibernateSearchEventListener;
import org.hibernate.search.mapper.orm.logging.impl.Log;
import org.hibernate.search.mapper.pojo.work.spi.PojoIndexingQueueEventProcessingPlan;
import org.hibernate.search.mapper.pojo.work.spi.PojoIndexingPlan;
import org.hibernate.search.mapper.pojo.work.spi.PojoWorkSessionContext;
import org.hibernate.search.util.common.impl.SuppressingCloser;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public final class ConfiguredAutomaticIndexingStrategy {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private static final OptionalConfigurationProperty<BeanReference<? extends AutomaticIndexingSynchronizationStrategy>> AUTOMATIC_INDEXING_SYNCHRONIZATION_STRATEGY =
			ConfigurationProperty.forKey( HibernateOrmMapperSettings.Radicals.AUTOMATIC_INDEXING_SYNCHRONIZATION_STRATEGY )
					.asBeanReference( AutomaticIndexingSynchronizationStrategy.class )
					.build();

	private static final ConfigurationProperty<Boolean> DIRTY_CHECK_ENABLED =
			ConfigurationProperty.forKey( HibernateOrmMapperSettings.Radicals.AUTOMATIC_INDEXING_ENABLE_DIRTY_CHECK )
					.asBoolean()
					.withDefault( HibernateOrmMapperSettings.Defaults.AUTOMATIC_INDEXING_ENABLE_DIRTY_CHECK )
					.build();

	private final HibernateOrmSearchSessionMappingContext mappingContext;

	private final boolean enableOrmEventListener;
	private final Function<AutomaticIndexingEventSendingSessionContext, AutomaticIndexingQueueEventSendingPlan> senderFactory;
	private final boolean enlistsInTransaction;
	private final BeanHolder<? extends AutomaticIndexingSynchronizationStrategy> defaultSynchronizationStrategyHolder;
	private final ConfiguredAutomaticIndexingSynchronizationStrategy defaultSynchronizationStrategy;

	private ConfiguredAutomaticIndexingStrategy(Builder builder,
			BeanHolder<? extends AutomaticIndexingSynchronizationStrategy> defaultSynchronizationStrategyHolder) {
		mappingContext = builder.mappingContext;
		enableOrmEventListener = builder.enableOrmEventListener;
		senderFactory = builder.senderFactory;
		enlistsInTransaction = builder.enlistsInTransaction;
		this.defaultSynchronizationStrategyHolder = defaultSynchronizationStrategyHolder;
		defaultSynchronizationStrategy = configure( defaultSynchronizationStrategyHolder.get() );
	}

	public void close() {
		defaultSynchronizationStrategyHolder.close();
	}

	public void registerListeners(SessionFactoryImplementor sessionFactory,
			HibernateOrmListenerContextProvider contextProvider, ConfigurationPropertySource propertySource) {
		if ( enableOrmEventListener ) {
			log.debug( "Hibernate Search event listeners activated" );
			HibernateSearchEventListener hibernateSearchEventListener = new HibernateSearchEventListener(
					contextProvider, DIRTY_CHECK_ENABLED.get( propertySource ) );
			hibernateSearchEventListener.registerTo( sessionFactory );
		}
		else {
			log.debug( "Hibernate Search event listeners deactivated" );
		}
	}

	public ConfiguredAutomaticIndexingSynchronizationStrategy defaultIndexingPlanSynchronizationStrategy() {
		return defaultSynchronizationStrategy;
	}

	public ConfiguredAutomaticIndexingSynchronizationStrategy configureOverriddenSynchronizationStrategy(
			AutomaticIndexingSynchronizationStrategy synchronizationStrategy) {
		if ( senderFactory != null ) {
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
		if ( senderFactory != null ) {
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

	public PojoIndexingQueueEventProcessingPlan createIndexingQueueEventProcessingPlan(PojoWorkSessionContext context,
			ConfiguredAutomaticIndexingSynchronizationStrategy synchronizationStrategy) {
		return mappingContext.createIndexingQueueEventProcessingPlan( context,
				synchronizationStrategy.getDocumentCommitStrategy(),
				synchronizationStrategy.getDocumentRefreshStrategy() );
	}

	private ConfiguredAutomaticIndexingSynchronizationStrategy configure(
			AutomaticIndexingSynchronizationStrategy synchronizationStrategy) {
		ConfiguredAutomaticIndexingSynchronizationStrategy.Builder builder =
				new ConfiguredAutomaticIndexingSynchronizationStrategy.Builder( mappingContext.failureHandler(),
						mappingContext.entityReferenceFactory() );
		synchronizationStrategy.apply( builder );
		return builder.build();
	}

	public static final class Builder implements AutomaticIndexingConfigurationContext {
		private final HibernateOrmSearchSessionMappingContext mappingContext;

		private boolean enableOrmEventListener = true;
		private Function<AutomaticIndexingEventSendingSessionContext, AutomaticIndexingQueueEventSendingPlan> senderFactory;
		private boolean enlistsInTransaction = false;

		public Builder(HibernateOrmSearchSessionMappingContext mappingContext) {
			this.mappingContext = mappingContext;
		}

		// TODO HSEARCH-168 move this to a configuration property: this is really orthogonal to the indexing strategy,
		//  as one could want to send indexing events to an outbox table,
		//  but only manually.
		@Override
		public void enableOrmEventListener(boolean enableOrmEventListener) {
			this.enableOrmEventListener = enableOrmEventListener;
		}

		@Override
		public void reindexInSession() {
			enableOrmEventListener = true;
			this.senderFactory = null;
			this.enlistsInTransaction = false;
		}

		@Override
		public void sendIndexingEventsTo(
				Function<AutomaticIndexingEventSendingSessionContext, AutomaticIndexingQueueEventSendingPlan> senderFactory,
				boolean enlistsInTransaction) {
			enableOrmEventListener = true;
			this.senderFactory = senderFactory;
			this.enlistsInTransaction = enlistsInTransaction;
		}

		public ConfiguredAutomaticIndexingStrategy build(BeanResolver beanResolver,
				ConfigurationPropertySource propertySource) {
			BeanHolder<? extends AutomaticIndexingSynchronizationStrategy> synchronizationStrategyHolder = null;
			try {
				synchronizationStrategyHolder =
						AUTOMATIC_INDEXING_SYNCHRONIZATION_STRATEGY.getAndTransform( propertySource, referenceOptional -> {
							if ( senderFactory != null ) {
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
								return beanResolver.resolve( referenceOptional
										.orElse( HibernateOrmMapperSettings.Defaults.AUTOMATIC_INDEXING_SYNCHRONIZATION_STRATEGY ) );
							}
						} );
				return new ConfiguredAutomaticIndexingStrategy( this, synchronizationStrategyHolder );
			}
			catch (RuntimeException e) {
				new SuppressingCloser( e )
						.push( synchronizationStrategyHolder );
				throw e;
			}
		}
	}

}
