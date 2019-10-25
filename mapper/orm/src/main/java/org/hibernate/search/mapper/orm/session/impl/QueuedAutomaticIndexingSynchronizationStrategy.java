/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.session.impl;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.engine.backend.work.execution.DocumentCommitStrategy;
import org.hibernate.search.engine.backend.work.execution.DocumentRefreshStrategy;
import org.hibernate.search.engine.reporting.EntityIndexingFailureContext;
import org.hibernate.search.engine.reporting.FailureHandler;
import org.hibernate.search.mapper.orm.common.EntityReference;
import org.hibernate.search.mapper.orm.logging.impl.Log;
import org.hibernate.search.mapper.orm.session.AutomaticIndexingSynchronizationConfigurationContext;
import org.hibernate.search.mapper.orm.session.AutomaticIndexingSynchronizationStrategy;
import org.hibernate.search.util.common.impl.Futures;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public final class QueuedAutomaticIndexingSynchronizationStrategy
		implements AutomaticIndexingSynchronizationStrategy {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	public static final AutomaticIndexingSynchronizationStrategy INSTANCE = new QueuedAutomaticIndexingSynchronizationStrategy();

	private QueuedAutomaticIndexingSynchronizationStrategy() {
	}

	@Override
	public String toString() {
		return AutomaticIndexingSynchronizationStrategy.class.getSimpleName() + ".queued()";
	}

	@Override
	public void apply(AutomaticIndexingSynchronizationConfigurationContext context) {
		context.documentCommitStrategy( DocumentCommitStrategy.NONE );
		context.documentRefreshStrategy( DocumentRefreshStrategy.NONE );
		FailureHandler failureHandler = context.getFailureHandler();
		context.indexingFutureHandler( future -> {
			future.whenComplete( Futures.handler( (result, throwable) -> {
				if ( throwable != null ) {
					EntityIndexingFailureContext.Builder contextBuilder = EntityIndexingFailureContext.builder();
					contextBuilder.throwable( throwable );
					contextBuilder.failingOperation( log.automaticIndexing() );
					failureHandler.handle( contextBuilder.build() );
				}
				else if ( result != null && result.getThrowable().isPresent() ) {
					EntityIndexingFailureContext.Builder contextBuilder = EntityIndexingFailureContext.builder();
					contextBuilder.throwable( result.getThrowable().get() );
					contextBuilder.failingOperation( log.automaticIndexing() );
					for ( EntityReference entityReference : result.getFailingEntities() ) {
						contextBuilder.entityReference( entityReference );
					}
					failureHandler.handle( contextBuilder.build() );
				}
			} ) );
		} );
	}
}
