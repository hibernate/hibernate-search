/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.predicate.impl;

import org.hibernate.search.backend.elasticsearch.gson.impl.JsonAccessor;
import org.hibernate.search.engine.search.predicate.spi.SearchPredicateBuilder;

import com.google.gson.JsonObject;


public abstract class AbstractElasticsearchSearchPredicateBuilder
		implements SearchPredicateBuilder<ElasticsearchSearchPredicateBuilder>,
		ElasticsearchSearchPredicateBuilder {

	private static final JsonAccessor<Float> BOOST_ACCESSOR = JsonAccessor.root().property( "boost" ).asFloat();

	private Float boost;
	private boolean withConstantScore = false;

	@Override
	public void boost(float boost) {
		this.boost = boost;
	}

	@Override
	public void constantScore() {
		this.withConstantScore = true;
	}

	@Override
	public ElasticsearchSearchPredicateBuilder toImplementation() {
		return this;
	}

	@Override
	public JsonObject build(ElasticsearchSearchPredicateContext context) {
		JsonObject outerObject = new JsonObject();
		JsonObject innerObject = new JsonObject();

		// in case of withConstantScore boots is set by constant_score clause
		if ( boost != null && !withConstantScore ) {
			BOOST_ACCESSOR.set( innerObject, boost );
		}

		JsonObject result = doBuild( context, outerObject, innerObject );
		return ( withConstantScore ) ? applyConstantScore( result ) : result;
	}

	protected abstract JsonObject doBuild(ElasticsearchSearchPredicateContext context,
			JsonObject outerObject, JsonObject innerObject);

	private JsonObject applyConstantScore(JsonObject filter) {
		JsonObject constantScore = new JsonObject();
		constantScore.add( "filter", filter );
		if ( boost != null ) {
			BOOST_ACCESSOR.set( constantScore, boost );
		}

		JsonObject result = new JsonObject();
		result.add( "constant_score", constantScore );

		return result;
	}
}
