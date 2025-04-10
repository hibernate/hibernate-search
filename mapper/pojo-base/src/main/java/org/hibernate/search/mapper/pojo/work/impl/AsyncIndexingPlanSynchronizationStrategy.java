/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.work.impl;


import org.hibernate.search.engine.backend.work.execution.DocumentCommitStrategy;
import org.hibernate.search.engine.backend.work.execution.DocumentRefreshStrategy;
import org.hibernate.search.engine.common.EntityReference;
import org.hibernate.search.engine.reporting.EntityIndexingFailureContext;
import org.hibernate.search.engine.reporting.FailureHandler;
import org.hibernate.search.mapper.pojo.reporting.impl.PojoMassIndexerMessages;
import org.hibernate.search.mapper.pojo.work.IndexingPlanSynchronizationStrategy;
import org.hibernate.search.mapper.pojo.work.IndexingPlanSynchronizationStrategyConfigurationContext;
import org.hibernate.search.util.common.annotation.Incubating;
import org.hibernate.search.util.common.impl.Futures;

@Incubating
public final class AsyncIndexingPlanSynchronizationStrategy implements IndexingPlanSynchronizationStrategy {

	public static final IndexingPlanSynchronizationStrategy INSTANCE = new AsyncIndexingPlanSynchronizationStrategy();

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
				contextBuilder.failingOperation( PojoMassIndexerMessages.INSTANCE.backgroundIndexing() );
				failureHandler.handle( contextBuilder.build() );
			}
			else if ( result != null && result.throwable().isPresent() ) {
				EntityIndexingFailureContext.Builder contextBuilder = EntityIndexingFailureContext.builder();
				contextBuilder.throwable( result.throwable().get() );
				contextBuilder.failingOperation( PojoMassIndexerMessages.INSTANCE.backgroundIndexing() );
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
