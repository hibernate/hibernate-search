/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.work.impl;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.engine.backend.work.execution.DocumentCommitStrategy;
import org.hibernate.search.engine.backend.work.execution.DocumentRefreshStrategy;
import org.hibernate.search.engine.common.EntityReference;
import org.hibernate.search.engine.reporting.EntityIndexingFailureContext;
import org.hibernate.search.engine.reporting.FailureHandler;
import org.hibernate.search.mapper.pojo.logging.impl.Log;
import org.hibernate.search.mapper.pojo.work.IndexingPlanSynchronizationStrategy;
import org.hibernate.search.mapper.pojo.work.IndexingPlanSynchronizationStrategyConfigurationContext;
import org.hibernate.search.util.common.annotation.Incubating;
import org.hibernate.search.util.common.impl.Futures;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

@Incubating
public final class AsyncIndexingPlanSynchronizationStrategy implements IndexingPlanSynchronizationStrategy {

	public static final IndexingPlanSynchronizationStrategy INSTANCE = new AsyncIndexingPlanSynchronizationStrategy();
	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private AsyncIndexingPlanSynchronizationStrategy() {
	}

	@Override
	public String toString() {
		return IndexingPlanSynchronizationStrategy.class.getSimpleName() + ".async()";
	}

	@Override
	public void apply(IndexingPlanSynchronizationStrategyConfigurationContext context) {
		context.documentCommitStrategy( DocumentCommitStrategy.NONE );
		context.documentRefreshStrategy( DocumentRefreshStrategy.NONE );
		FailureHandler failureHandler = context.failureHandler();
		context.indexingFutureHandler( future -> future.whenComplete( Futures.handler( (result, throwable) -> {
			if ( throwable != null ) {
				EntityIndexingFailureContext.Builder contextBuilder = EntityIndexingFailureContext.builder();
				contextBuilder.throwable( throwable );
				contextBuilder.failingOperation( log.backgroundIndexing() );
				failureHandler.handle( contextBuilder.build() );
			}
			else if ( result != null && result.throwable().isPresent() ) {
				EntityIndexingFailureContext.Builder contextBuilder = EntityIndexingFailureContext.builder();
				contextBuilder.throwable( result.throwable().get() );
				contextBuilder.failingOperation( log.backgroundIndexing() );
				for ( EntityReference entityReference : result.failingEntities() ) {
					contextBuilder.failingEntityReference( entityReference );
				}
				failureHandler.handle( contextBuilder.build() );
			}
		} )
		)
		);
	}
}
