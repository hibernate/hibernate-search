/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.work.impl;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.util.spi.LoggerFactory;

import com.google.gson.JsonObject;

/**
 * @author Yoann Rodiere
 */
public class StubElasticsearchWork<T> implements ElasticsearchWork<T> {

	private static final Log log = LoggerFactory.make( Log.class );

	private final String workType;

	private final JsonObject document;

	private final Map<String, String> parameters;

	public StubElasticsearchWork(String workType, JsonObject document) {
		super();
		this.workType = workType;
		this.document = document;
		this.parameters = new LinkedHashMap<>();
	}

	public StubElasticsearchWork<T> addParam(String name, String value) {
		parameters.put( name, value );
		return this;
	}

	@Override
	public CompletableFuture<T> execute(ElasticsearchWorkExecutionContext context) {
		log.executingWork( context.getClient(), workType, parameters, document );
		return CompletableFuture.completedFuture( null );
	}

}
