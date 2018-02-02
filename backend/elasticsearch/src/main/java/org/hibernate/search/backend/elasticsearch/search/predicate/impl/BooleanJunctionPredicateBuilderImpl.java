/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.predicate.impl;

import org.hibernate.search.backend.elasticsearch.gson.impl.JsonAccessor;
import org.hibernate.search.engine.search.predicate.spi.BooleanJunctionPredicateBuilder;

import com.google.gson.JsonObject;


/**
 * @author Yoann Rodiere
 */
class BooleanJunctionPredicateBuilderImpl extends AbstractSearchPredicateBuilder
		implements BooleanJunctionPredicateBuilder<ElasticsearchSearchPredicateCollector> {

	private static final JsonAccessor<JsonObject> MUST = JsonAccessor.root().property( "must" ).asObject();
	private static final JsonAccessor<JsonObject> MUST_NOT = JsonAccessor.root().property( "must_not" ).asObject();
	private static final JsonAccessor<JsonObject> SHOULD = JsonAccessor.root().property( "should" ).asObject();
	private static final JsonAccessor<JsonObject> FILTER = JsonAccessor.root().property( "filter" ).asObject();

	@Override
	public ElasticsearchSearchPredicateCollector getMustCollector() {
		return this::must;
	}

	@Override
	public ElasticsearchSearchPredicateCollector getMustNotCollector() {
		return this::mustNot;
	}

	@Override
	public ElasticsearchSearchPredicateCollector getShouldCollector() {
		return this::should;
	}

	@Override
	public ElasticsearchSearchPredicateCollector getFilterCollector() {
		return this::filter;
	}

	private void must(JsonObject query) {
		MUST.add( getInnerObject(), query );
	}

	private void mustNot(JsonObject query) {
		MUST_NOT.add( getInnerObject(), query );
	}

	private void should(JsonObject query) {
		SHOULD.add( getInnerObject(), query );
	}

	private void filter(JsonObject query) {
		FILTER.add( getInnerObject(), query );
	}

	@Override
	public void contribute(ElasticsearchSearchPredicateCollector collector) {
		JsonObject outerObject = getOuterObject();
		outerObject.add( "bool", getInnerObject() );
		collector.collectPredicate( outerObject );
	}

}
