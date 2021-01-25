/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.automaticindexing.impl;

import java.util.concurrent.CompletableFuture;

import org.hibernate.search.mapper.orm.automaticindexing.spi.AutomaticIndexingConfigurationContext;
import org.hibernate.search.mapper.orm.automaticindexing.spi.AutomaticIndexingStrategy;
import org.hibernate.search.mapper.orm.automaticindexing.spi.AutomaticIndexingStrategyPreStopContext;
import org.hibernate.search.mapper.orm.automaticindexing.spi.AutomaticIndexingStrategyStartContext;

public class SessionAutomaticIndexingStrategy implements AutomaticIndexingStrategy {
	@Override
	public CompletableFuture<?> start(AutomaticIndexingStrategyStartContext context) {
		// Nothing to do
		return CompletableFuture.completedFuture( null );
	}

	@Override
	public void configure(AutomaticIndexingConfigurationContext context) {
		context.reindexInSession();
	}

	@Override
	public CompletableFuture<?> preStop(AutomaticIndexingStrategyPreStopContext context) {
		// Nothing to do
		return CompletableFuture.completedFuture( null );
	}

	@Override
	public void stop() {
		// Nothing to do
	}
}
