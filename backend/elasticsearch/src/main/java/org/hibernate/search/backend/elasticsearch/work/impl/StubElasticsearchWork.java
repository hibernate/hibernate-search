/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.work.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Supplier;

import org.hibernate.search.backend.elasticsearch.client.impl.StubElasticsearchClient;

import com.google.gson.JsonObject;

/**
 * @author Yoann Rodiere
 */
public class StubElasticsearchWork<T> implements ElasticsearchWork<T> {

	private final String workType;

	private final JsonObject document;

	private final Map<String, List<String>> parameters;

	private Supplier<T> resultSupplier = () -> null;

	public StubElasticsearchWork(String workType, JsonObject document) {
		super();
		this.workType = workType;
		this.document = document;
		this.parameters = new LinkedHashMap<>();
	}

	public StubElasticsearchWork<T> addParam(String name, String value) {
		if ( value != null ) {
			parameters.computeIfAbsent( name, ignored -> new ArrayList<>() ).add( value );
		}
		return this;
	}

	public <U> StubElasticsearchWork<T> addParam(String name, U value, Function<U, String> renderer) {
		if ( value != null ) {
			parameters.computeIfAbsent( name, ignored -> new ArrayList<>() ).add( renderer.apply( value ) );
		}
		return this;
	}

	public StubElasticsearchWork<T> addParam(String name, Collection<String> values) {
		if ( values != null && values.stream().anyMatch( Objects::nonNull ) ) {
			parameters.computeIfAbsent( name, ignored -> new ArrayList<>() ).addAll( values );
		}
		return this;
	}

	public StubElasticsearchWork<T> setResult(Supplier<T> resultSupplier) {
		this.resultSupplier = resultSupplier;
		return this;
	}

	@Override
	public CompletableFuture<T> execute(ElasticsearchWorkExecutionContext context) {
		return ((StubElasticsearchClient) context.getClient()).execute( workType, parameters, document, resultSupplier );
	}

}
