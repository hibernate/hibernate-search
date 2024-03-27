/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.massindexing.spi;

import java.util.concurrent.CompletableFuture;

public interface PojoMassIndexerAgent {

	static PojoMassIndexerAgent noOp() {
		return NoOpPojoMassIndexerAgent.INSTANCE;
	}

	/**
	 * Starts requesting from other agents that could possibly perform indexing (e.g. indexing plans)
	 * that they suspend themselves.
	 * <p>
	 * Other agents can be considered suspended when the returned future completes successfully;
	 * they will remain suspended until this agent is {@link #preStop() pre-stoppped} or {@link #stop() stopped}.
	 *
	 * @return A future that completes successfully when other agents have been successfully suspended.
	 * If no agents can be suspended (e.g. no coordination), returns a successfully completed future immediately.
	 */
	CompletableFuture<?> start(PojoMassIndexerAgentStartContext context);

	/**
	 * Performs preliminary operations necessary to safely stop this agent.
	 * <p>
	 * This should be called before {@link #stop()}, unless other errors are forcing us to make an emergency stop.
	 *
	 * @return A future that completes successfully when the agent has stopped.
	 */
	CompletableFuture<?> preStop();

	/**
	 * Stops this agent.
	 */
	void stop();

}
