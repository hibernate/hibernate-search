/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.orchestration.impl;

import java.util.concurrent.CompletableFuture;

import org.hibernate.search.backend.elasticsearch.work.impl.ElasticsearchWork;
import org.hibernate.search.engine.backend.orchestration.spi.AbstractWorkOrchestrator;

/**
 * An abstract base for {@link ElasticsearchWorkOrchestratorImplementor} implementations.
 */
abstract class AbstractElasticsearchWorkOrchestrator
		extends AbstractWorkOrchestrator<ElasticsearchWorkSet>
		implements ElasticsearchWorkOrchestratorImplementor {

	AbstractElasticsearchWorkOrchestrator(String name) {
		super( name );
	}

	@Override
	public <T> CompletableFuture<T> submit(ElasticsearchWork<T> work) {
		CompletableFuture<T> future = new CompletableFuture<>();
		submit( new ElasticsearchSingleWorkSet<>( work, future ) );
		return future;
	}

}
