/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.outbox.impl;

import java.lang.invoke.MethodHandles;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;

import org.hibernate.search.engine.cfg.spi.ConfigurationProperty;
import org.hibernate.search.engine.cfg.spi.OptionalConfigurationProperty;
import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.engine.environment.bean.BeanReference;
import org.hibernate.search.mapper.orm.automaticindexing.spi.AutomaticIndexingConfigurationContext;
import org.hibernate.search.mapper.orm.automaticindexing.spi.AutomaticIndexingStrategy;
import org.hibernate.search.mapper.orm.automaticindexing.spi.AutomaticIndexingStrategyPreStopContext;
import org.hibernate.search.mapper.orm.automaticindexing.spi.AutomaticIndexingStrategyStartContext;
import org.hibernate.search.mapper.orm.cfg.HibernateOrmMapperSettings;
import org.hibernate.search.mapper.orm.cfg.impl.HibernateOrmMapperImplSettings;
import org.hibernate.search.mapper.orm.logging.impl.Log;
import org.hibernate.search.util.common.impl.Closer;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public class OutboxPollingAutomaticIndexingStrategy implements AutomaticIndexingStrategy {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private static final ConfigurationProperty<Integer> AUTOMATIC_INDEXING_POLLING_INTERVAL =
			ConfigurationProperty.forKey( HibernateOrmMapperSettings.AutomaticIndexingRadicals.POLLING_INTERVAL )
					.asInteger()
					.withDefault( HibernateOrmMapperSettings.Defaults.AUTOMATIC_INDEXING_POLLING_INTERVAL )
					.build();

	private static final OptionalConfigurationProperty<BeanReference<? extends OutboxEventFinder>> AUTOMATIC_INDEXING_OUTBOX_EVENT_FINDER =
			ConfigurationProperty.forKey( HibernateOrmMapperImplSettings.AutomaticIndexingRadicals.OUTBOX_EVENT_FINDER )
					.asBeanReference( OutboxEventFinder.class )
					.build();

	private static final ConfigurationProperty<Integer> AUTOMATIC_INDEXING_BATCH_SIZE =
			ConfigurationProperty.forKey( HibernateOrmMapperSettings.AutomaticIndexingRadicals.BATCH_SIZE )
					.asInteger()
					.withDefault( HibernateOrmMapperSettings.Defaults.AUTOMATIC_INDEXING_BATCH_SIZE )
					.build();

	public static final String NAME = "Outbox table automatic indexing";

	private BeanHolder<? extends OutboxEventFinder> finderHolder;
	private ScheduledExecutorService scheduledExecutor;
	private volatile OutboxEventBackgroundExecutor executor;

	@Override
	public void configure(AutomaticIndexingConfigurationContext context) {
		context.sendIndexingEventsTo( ctx -> new OutboxTableSendingPlan( ctx.session() ), true );
	}

	@Override
	public CompletableFuture<?> start(AutomaticIndexingStrategyStartContext context) {
		Optional<BeanHolder<? extends OutboxEventFinder>> finderHolderOptional =
				AUTOMATIC_INDEXING_OUTBOX_EVENT_FINDER.getAndMap( context.configurationPropertySource(), context.beanResolver()::resolve );
		if ( finderHolderOptional.isPresent() ) {
			finderHolder = finderHolderOptional.get();
			log.debugf( "Outbox processing will use custom outbox event finder '%s'.", finderHolder.get() );
		}
		else {
			finderHolder = BeanHolder.of( new DefaultOutboxEventFinder() );
		}

		int pollingInterval = AUTOMATIC_INDEXING_POLLING_INTERVAL.get( context.configurationPropertySource() );

		int batchSize = AUTOMATIC_INDEXING_BATCH_SIZE.get( context.configurationPropertySource() );

		scheduledExecutor = context.threadPoolProvider().newScheduledExecutor( 1, NAME );
		executor = new OutboxEventBackgroundExecutor( context.mapping(), scheduledExecutor, finderHolder.get(),
				pollingInterval, batchSize );
		executor.start();
		return CompletableFuture.completedFuture( null );
	}

	@Override
	public CompletableFuture<?> preStop(AutomaticIndexingStrategyPreStopContext context) {
		if ( executor == null ) {
			// Nothing to do
			return CompletableFuture.completedFuture( null );
		}
		return executor.preStop();
	}

	@Override
	public void stop() {
		try ( Closer<RuntimeException> closer = new Closer<>() ) {
			closer.push( OutboxEventBackgroundExecutor::stop, executor );
			closer.push( ScheduledExecutorService::shutdownNow, scheduledExecutor );
			closer.push( BeanHolder::close, finderHolder );
		}
	}
}
