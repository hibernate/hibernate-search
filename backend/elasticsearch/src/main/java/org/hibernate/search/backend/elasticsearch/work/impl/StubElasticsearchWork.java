/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.work.impl;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import org.hibernate.search.backend.elasticsearch.client.impl.ElasticsearchRequest;
import org.hibernate.search.backend.elasticsearch.client.impl.ElasticsearchResponse;

import com.google.gson.JsonObject;

/**
 * @author Yoann Rodiere
 */
public class StubElasticsearchWork<T> implements ElasticsearchWork<T> {

	private final ElasticsearchRequest request;

	private final Function<JsonObject, T> resultFunction;

	public StubElasticsearchWork(ElasticsearchRequest request) {
		this( request, ignored -> null );
	}

	public StubElasticsearchWork(ElasticsearchRequest request, Function<JsonObject, T> resultFunction) {
		this.request = request;
		this.resultFunction = resultFunction;
	}

	@Override
	public CompletableFuture<T> execute(ElasticsearchWorkExecutionContext context) {
		CompletableFuture<ElasticsearchResponse> response = context.getClient().submit( request );
		if ( resultFunction != null ) {
			return response.thenApply( ElasticsearchResponse::getBody ).thenApply( resultFunction );
		}
		else {
			return response.thenApply( r -> null );
		}
	}

}
