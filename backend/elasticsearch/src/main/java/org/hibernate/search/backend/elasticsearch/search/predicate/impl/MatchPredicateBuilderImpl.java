/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.predicate.impl;

import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchFieldFormatter;
import org.hibernate.search.backend.elasticsearch.gson.impl.JsonAccessor;
import org.hibernate.search.backend.elasticsearch.search.dsl.impl.ElasticsearchSearchPredicateCollector;
import org.hibernate.search.engine.search.predicate.spi.MatchPredicateBuilder;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * @author Yoann Rodiere
 */
class MatchPredicateBuilderImpl extends AbstractSearchPredicateBuilder
		implements MatchPredicateBuilder<ElasticsearchSearchPredicateCollector> {

	private static final JsonAccessor<JsonElement> VALUE = JsonAccessor.root().property( "value" );

	private final String fieldName;

	private final ElasticsearchFieldFormatter formatter;

	public MatchPredicateBuilderImpl(String fieldName, ElasticsearchFieldFormatter formatter) {
		this.fieldName = fieldName;
		this.formatter = formatter;
	}

	@Override
	public void value(Object value) {
		VALUE.set( getInnerObject(), formatter.format( value ) );
	}

	@Override
	public void contribute(ElasticsearchSearchPredicateCollector collector) {
		JsonObject outerObject = getOuterObject();
		JsonObject middleObject = new JsonObject();
		middleObject.add( fieldName, getInnerObject() );
		outerObject.add( "match", middleObject );
		collector.collect( outerObject );
	}

}
