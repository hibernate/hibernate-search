/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.orm.coordination.common.spi;

import java.util.concurrent.CompletableFuture;

import org.hibernate.search.mapper.pojo.massindexing.spi.PojoMassIndexerAgent;
import org.hibernate.search.mapper.pojo.massindexing.spi.PojoMassIndexerAgentCreateContext;

/**
 * The strategy for coordinating between threads of a single-node application,
 * or between nodes of a distributed application.
 * <p>
 * Advanced implementations may involve an external system to store and asynchronously consume indexing events,
 * ultimately routing them back to Hibernate Search's in-JVM indexing plans.
 *
 * @see org.hibernate.search.mapper.orm.cfg.HibernateOrmMapperSettings#COORDINATION_STRATEGY
 */
public interface CoordinationStrategy {

	/**
	 * Configures coordination.
	 * <p>
	 * Called once during bootstrap, before anything (mapper, backends, index managers) is started.
	 *
	 * @param context The configuration context.
	 */
	void configure(CoordinationConfigurationContext context);

	/**
	 * Configures this strategy and starts processing events in the background.
	 * <p>
	 * Called once during bootstrap, after {@link #configure(CoordinationConfigurationContext)}.
	 *
	 * @param context The start context.
	 * @return A future that completes when the strategy is completely started.
	 */
	CompletableFuture<?> start(CoordinationStrategyStartContext context);

	/**
	 * Creates a {@link PojoMassIndexerAgent},
	 * able to exert control over other agents that could perform indexing concurrently (e.g. background processing of entity change events with outbox-polling coordination strategy).
	 *
	 * @param context A context with information about the mass indexing that is about to start.
	 * @return An agent.
	 */
	PojoMassIndexerAgent createMassIndexerAgent(PojoMassIndexerAgentCreateContext context);

	/**
	 * @return A future that completes when all works submitted to background executors so far are completely executed.
	 * Works submitted to the executors after entering this method may delay the wait.
	 */
	CompletableFuture<?> completion();

	/**
	 * Prepares for {@link #stop()},
	 * executing any operations that need to be executed before shutdown.
	 * <p>
	 * Called once on shutdown,
	 * before backends and index managers are stopped.
	 *
	 * @param context The pre-stop context.
	 * @return A future that completes when pre-stop operations complete.
	 */
	CompletableFuture<?> preStop(CoordinationStrategyPreStopContext context);

	/**
	 * Stops and releases all resources.
	 * <p>
	 * Called once on shutdown,
	 * after the future returned by {@link #preStop(CoordinationStrategyPreStopContext)} completed.
	 */
	void stop();
}
