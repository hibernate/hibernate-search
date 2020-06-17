/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.predicate.impl;

import org.hibernate.search.backend.elasticsearch.gson.impl.JsonAccessor;
import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchContext;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.spi.SearchPredicateBuilder;

import com.google.gson.JsonObject;


public abstract class AbstractElasticsearchSearchPredicateBuilder
		implements SearchPredicateBuilder,
		ElasticsearchSearchPredicateBuilder {

	private static final JsonAccessor<Float> BOOST_ACCESSOR = JsonAccessor.root().property( "boost" ).asFloat();

	protected final ElasticsearchSearchContext searchContext;

	private Float boost;
	private boolean withConstantScore = false;

	AbstractElasticsearchSearchPredicateBuilder(ElasticsearchSearchContext searchContext) {
		this.searchContext = searchContext;
	}

	@Override
	public void boost(float boost) {
		this.boost = boost;
	}

	@Override
	public void constantScore() {
		this.withConstantScore = true;
	}

	@Override
	public SearchPredicate build() {
		// TODO HSEARCH-3476 this is just a temporary hack:
		//  we should move to one SearchPredicate implementation per type of predicate.
		return ElasticsearchSearchPredicate.of( searchContext, this );
	}

	@Override
	public JsonObject toJsonQuery(PredicateRequestContext context) {
		JsonObject outerObject = new JsonObject();
		JsonObject innerObject = new JsonObject();

		// in case of withConstantScore boots is set by constant_score clause
		if ( boost != null && !withConstantScore ) {
			BOOST_ACCESSOR.set( innerObject, boost );
		}

		JsonObject result = doBuild( context, outerObject, innerObject );
		return ( withConstantScore ) ? applyConstantScore( result ) : result;
	}

	protected abstract JsonObject doBuild(PredicateRequestContext context,
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
