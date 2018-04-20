/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.predicate.impl;

import org.hibernate.search.backend.elasticsearch.gson.impl.JsonAccessor;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchFieldCodec;
import org.hibernate.search.engine.search.predicate.spi.MatchPredicateBuilder;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * @author Yoann Rodiere
 */
class MatchPredicateBuilderImpl extends AbstractSearchPredicateBuilder
		implements MatchPredicateBuilder<ElasticsearchSearchPredicateCollector> {

	private static final JsonAccessor<JsonElement> QUERY = JsonAccessor.root().property( "query" );

	private final String absoluteFieldPath;

	private final ElasticsearchFieldCodec codec;

	public MatchPredicateBuilderImpl(String absoluteFieldPath, ElasticsearchFieldCodec codec) {
		this.absoluteFieldPath = absoluteFieldPath;
		this.codec = codec;
	}

	@Override
	public void value(Object value) {
		QUERY.set( getInnerObject(), codec.encode( value ) );
	}

	@Override
	public void contribute(ElasticsearchSearchPredicateCollector collector) {
		JsonObject outerObject = getOuterObject();
		JsonObject middleObject = new JsonObject();
		middleObject.add( absoluteFieldPath, getInnerObject() );
		outerObject.add( "match", middleObject );
		collector.collectPredicate( outerObject );
	}

}
