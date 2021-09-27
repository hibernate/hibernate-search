/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.coordination.impl;

import java.util.concurrent.CompletableFuture;

import org.hibernate.search.mapper.orm.automaticindexing.spi.AutomaticIndexingConfigurationContext;
import org.hibernate.search.mapper.orm.coordination.common.spi.CooordinationStrategy;
import org.hibernate.search.mapper.orm.coordination.common.spi.CoordinationStrategyPreStopContext;
import org.hibernate.search.mapper.orm.coordination.common.spi.CoordinationStrategyStartContext;

public class NoCoordinationStrategy implements CooordinationStrategy {

	public static final String NAME = "none";

	@Override
	public CompletableFuture<?> start(CoordinationStrategyStartContext context) {
		// Nothing to do
		return CompletableFuture.completedFuture( null );
	}

	@Override
	public void configureAutomaticIndexing(AutomaticIndexingConfigurationContext context) {
		context.reindexInSession();
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
