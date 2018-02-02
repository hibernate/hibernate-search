/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.predicate.impl;

import org.hibernate.search.backend.elasticsearch.gson.impl.JsonAccessor;
import org.hibernate.search.engine.search.predicate.spi.NestedPredicateBuilder;

import com.google.gson.JsonObject;


/**
 * @author Yoann Rodiere
 */
class NestedPredicateBuilderImpl extends AbstractSearchPredicateBuilder
		implements NestedPredicateBuilder<ElasticsearchSearchPredicateCollector> {

	private static final JsonAccessor<String> PATH = JsonAccessor.root().property( "path" ).asString();
	private static final JsonAccessor<JsonObject> QUERY = JsonAccessor.root().property( "query" ).asObject();

	private final String fieldName;

	NestedPredicateBuilderImpl(String fieldName) {
		this.fieldName = fieldName;
	}

	@Override
	public ElasticsearchSearchPredicateCollector getNestedCollector() {
		return this::nested;
	}

	private void nested(JsonObject query) {
		QUERY.set( getInnerObject(), query );
	}

	@Override
	public void contribute(ElasticsearchSearchPredicateCollector collector) {
		JsonObject outerObject = getOuterObject();
		JsonObject innerObject = getInnerObject();
		PATH.set( innerObject, fieldName );
		outerObject.add( "nested", getInnerObject() );
		collector.collectPredicate( outerObject );
	}

}
