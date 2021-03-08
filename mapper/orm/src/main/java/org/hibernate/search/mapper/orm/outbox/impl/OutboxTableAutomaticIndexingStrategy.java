/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.outbox.impl;

import java.lang.invoke.MethodHandles;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;

import org.hibernate.search.engine.cfg.spi.ConfigurationProperty;
import org.hibernate.search.mapper.orm.automaticindexing.spi.AutomaticIndexingConfigurationContext;
import org.hibernate.search.mapper.orm.automaticindexing.spi.AutomaticIndexingStrategy;
import org.hibernate.search.mapper.orm.automaticindexing.spi.AutomaticIndexingStrategyPreStopContext;
import org.hibernate.search.mapper.orm.automaticindexing.spi.AutomaticIndexingStrategyStartContext;
import org.hibernate.search.mapper.orm.cfg.HibernateOrmMapperSettings;
import org.hibernate.search.mapper.orm.logging.impl.Log;
import org.hibernate.search.util.common.impl.Closer;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public class OutboxTableAutomaticIndexingStrategy implements AutomaticIndexingStrategy {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private static final ConfigurationProperty<Boolean> PROCESS_OUTBOX_TABLE =
			ConfigurationProperty.forKey( HibernateOrmMapperSettings.Radicals.PROCESS_OUTBOX_TABLE )
					.asBoolean()
					.withDefault( HibernateOrmMapperSettings.Defaults.AUTOMATIC_INDEXING_PROCESS_OUTBOX_TABLE )
					.build();

	private static final String NAME = "Outbox table automatic indexing";

	private ScheduledExecutorService scheduledExecutor;
	private volatile OutboxTableIndexerExecutor executor;

	@Override
	public void configure(AutomaticIndexingConfigurationContext context) {
		context.sendIndexingEventsTo( ctx -> new OutboxTableSendingPlan( ctx.session() ), true );
	}

	@Override
	public CompletableFuture<?> start(AutomaticIndexingStrategyStartContext context) {
		if ( !PROCESS_OUTBOX_TABLE.get( context.configurationPropertySource() ) ) {
			log.debug( "The outbox table processing is disabled through configuration properties." );
			// Nothing to do
			return CompletableFuture.completedFuture( null );
		}

		scheduledExecutor = context.threadPoolProvider().newScheduledExecutor( 1, NAME );
		executor = new OutboxTableIndexerExecutor( context.mapping(), scheduledExecutor );
		executor.start();
		return CompletableFuture.completedFuture( null );
	}

	@Override
	public CompletableFuture<?> preStop(AutomaticIndexingStrategyPreStopContext context) {
		if ( executor == null ) {
			// Nothing to do
			return CompletableFuture.completedFuture( null );
		}
		return executor.stop();
	}

	@Override
	public void stop() {
		try ( Closer<RuntimeException> closer = new Closer<>() ) {
			closer.push( OutboxTableIndexerExecutor::stop, executor );
			closer.push( ScheduledExecutorService::shutdownNow, scheduledExecutor );
		}
	}
}
