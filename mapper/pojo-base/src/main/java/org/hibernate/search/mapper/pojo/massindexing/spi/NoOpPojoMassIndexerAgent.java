/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.massindexing.spi;

import java.util.concurrent.CompletableFuture;

class NoOpPojoMassIndexerAgent implements PojoMassIndexerAgent {
	public static final NoOpPojoMassIndexerAgent INSTANCE = new NoOpPojoMassIndexerAgent();

	private NoOpPojoMassIndexerAgent() {
	}

	@Override
	public CompletableFuture<?> start(PojoMassIndexerAgentStartContext context) {
		return CompletableFuture.completedFuture( null );
	}

	@Override
	public CompletableFuture<?> preStop() {
		return CompletableFuture.completedFuture( null );
	}

	@Override
	public void stop() {
		// Nothing to do
	}
}
