/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.index.management.impl;

import java.util.concurrent.CompletableFuture;

import org.hibernate.search.backend.elasticsearch.index.IndexLifecycleStrategyName;
import org.hibernate.search.backend.elasticsearch.index.admin.impl.ElasticsearchIndexAdministrationClient;
import org.hibernate.search.backend.elasticsearch.index.admin.impl.ElasticsearchIndexLifecycleExecutionOptions;
import org.hibernate.search.engine.backend.index.spi.IndexManagerStartContext;
import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.common.impl.Futures;

public class ElasticsearchIndexLifecycleStrategy {

	private final IndexLifecycleStrategyName strategyName;
	protected final ElasticsearchIndexLifecycleExecutionOptions executionOptions;

	public ElasticsearchIndexLifecycleStrategy(
			IndexLifecycleStrategyName strategyName,
			ElasticsearchIndexLifecycleExecutionOptions executionOptions) {
		this.strategyName = strategyName;
		this.executionOptions = executionOptions;
	}

	public CompletableFuture<?> onStart(ElasticsearchIndexAdministrationClient client, IndexManagerStartContext context) {
		switch ( strategyName ) {
			case CREATE:
				return client.createIfAbsent( executionOptions );
			case DROP_AND_CREATE:
			case DROP_AND_CREATE_AND_DROP:
				return client.dropAndCreate( executionOptions );
			case UPDATE:
				return client.update( executionOptions );
			case VALIDATE:
				return client.validate( executionOptions, context.getFailureCollector() );
			case NONE:
				// Nothing to do
				return CompletableFuture.completedFuture( null );
			default:
				throw new AssertionFailure( "Unexpected index management strategy: " + strategyName );
		}
	}

	public void onStop(ElasticsearchIndexAdministrationClient client) {
		switch ( strategyName ) {
			case DROP_AND_CREATE_AND_DROP:
				Futures.unwrappedExceptionJoin( client.dropIfExisting( executionOptions ) );
				break;
			case CREATE:
			case DROP_AND_CREATE:
			case UPDATE:
			case VALIDATE:
			case NONE:
				// Nothing to do
				break;
			default:
				throw new AssertionFailure( "Unexpected index management strategy: " + strategyName );
		}
	}
}
