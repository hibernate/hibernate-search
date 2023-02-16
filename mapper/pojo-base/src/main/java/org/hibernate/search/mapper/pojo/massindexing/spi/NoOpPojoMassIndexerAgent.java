/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
