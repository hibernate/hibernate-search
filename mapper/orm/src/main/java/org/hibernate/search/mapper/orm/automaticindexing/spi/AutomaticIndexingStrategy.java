/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.automaticindexing.spi;

import java.util.concurrent.CompletableFuture;

/**
 * A strategy for the automatic handling of indexing events.
 * <p>
 * Advanced implementations may involve an external system to store and asynchronously consume indexing events,
 * ultimately routing them back to Hibernate Search's in-JVM indexing plans.
 */
public interface AutomaticIndexingStrategy {

	/**
	 * Configures this strategy and starts processing events in the background.
	 * <p>
	 * Called by the engine once during bootstrap,
	 * after backends and index managers were started.
	 *
	 * @param context The start context.
	 * @return A future that completes when the strategy is completely started.
	 */
	CompletableFuture<?> start(AutomaticIndexingStrategyStartContext context);

	/**
	 * Configures automatic indexing.
	 *
	 * @param context The configuration context.
	 */
	void configure(AutomaticIndexingConfigurationContext context);

	/**
	 * Prepares for {@link #stop()},
	 * executing any operations that need to be executed before shutdown.
	 * <p>
	 * Called by the engine once on shutdown,
	 * before backends and index managers are stopped.
	 *
	 * @param context The pre-stop context.
	 * @return A future that completes when pre-stop operations complete.
	 */
	CompletableFuture<?> preStop(AutomaticIndexingStrategyPreStopContext context);

	/**
	 * Stops and releases all resources.
	 * <p>
	 * Called by the engine once on shutdown,
	 * after the future returned by {@link #preStop(AutomaticIndexingStrategyPreStopContext)} completed.
	 */
	void stop();
}
