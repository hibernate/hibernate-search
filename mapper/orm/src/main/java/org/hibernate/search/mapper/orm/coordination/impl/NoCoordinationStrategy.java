/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.orm.coordination.impl;

import java.util.concurrent.CompletableFuture;

import org.hibernate.search.mapper.orm.coordination.common.spi.CoordinationConfigurationContext;
import org.hibernate.search.mapper.orm.coordination.common.spi.CoordinationStrategy;
import org.hibernate.search.mapper.orm.coordination.common.spi.CoordinationStrategyPreStopContext;
import org.hibernate.search.mapper.orm.coordination.common.spi.CoordinationStrategyStartContext;
import org.hibernate.search.mapper.pojo.massindexing.spi.PojoMassIndexerAgent;
import org.hibernate.search.mapper.pojo.massindexing.spi.PojoMassIndexerAgentCreateContext;

public class NoCoordinationStrategy implements CoordinationStrategy {

	public static final String NAME = "none";

	@Override
	public CompletableFuture<?> start(CoordinationStrategyStartContext context) {
		// Nothing to do
		return CompletableFuture.completedFuture( null );
	}

	@Override
	public void configure(CoordinationConfigurationContext context) {
		context.reindexInSession();
	}

	@Override
	public PojoMassIndexerAgent createMassIndexerAgent(PojoMassIndexerAgentCreateContext context) {
		// No coordination: we don't prevent background indexing from continuing while mass indexing.
		return PojoMassIndexerAgent.noOp();
	}

	@Override
	public CompletableFuture<?> completion() {
		// Nothing operation in progress
		return CompletableFuture.completedFuture( null );
	}

	@Override
	public CompletableFuture<?> preStop(CoordinationStrategyPreStopContext context) {
		// Nothing to do
		return CompletableFuture.completedFuture( null );
	}

	@Override
	public void stop() {
		// Nothing to do
	}

}
