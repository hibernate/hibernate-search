/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.orchestration.impl;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.hibernate.search.backend.elasticsearch.client.impl.ElasticsearchClient;
import org.hibernate.search.backend.elasticsearch.work.impl.ElasticsearchWork;


/**
 * @author Yoann Rodiere
 */
public class StubElasticsearchWorkOrchestrator implements ElasticsearchWorkOrchestrator {

	private final StubElasticsearchWorkExecutionContext context;

	public StubElasticsearchWorkOrchestrator(ElasticsearchClient client) {
		this.context = new StubElasticsearchWorkExecutionContext( client );
	}

	@Override
	public CompletableFuture<?> submit(ElasticsearchWork<?> work) {
		return work.execute( context );
	}

	@Override
	public CompletableFuture<?> submit(List<ElasticsearchWork<?>> works) {
		for ( ElasticsearchWork<?> work : works ) {
			work.execute( context );
		}
		return CompletableFuture.completedFuture( null );
	}

}
