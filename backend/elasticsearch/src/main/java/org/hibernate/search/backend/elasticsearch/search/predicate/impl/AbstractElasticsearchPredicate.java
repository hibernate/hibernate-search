/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.predicate.impl;

import java.util.Set;

import org.hibernate.search.backend.elasticsearch.gson.impl.JsonAccessor;
import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchContext;
import org.hibernate.search.engine.search.predicate.spi.SearchPredicateBuilder;

import com.google.gson.JsonObject;

public abstract class AbstractElasticsearchPredicate implements ElasticsearchSearchPredicate {

	private static final JsonAccessor<Float> BOOST_ACCESSOR = JsonAccessor.root().property( "boost" ).asFloat();

	private final Set<String> indexNames;
	private final Float boost;
	private final boolean withConstantScore;

	protected AbstractElasticsearchPredicate(AbstractBuilder builder) {
		indexNames = builder.searchContext.indexes().hibernateSearchIndexNames();
		boost = builder.boost;
		withConstantScore = builder.withConstantScore;
	}

	@Override
	public Set<String> indexNames() {
		return indexNames;
	}

	@Override
	public JsonObject toJsonQuery(PredicateRequestContext context) {
		JsonObject outerObject = new JsonObject();
		JsonObject innerObject = new JsonObject();

		// in case of withConstantScore boots is set by constant_score clause
		if ( boost != null && !withConstantScore ) {
			BOOST_ACCESSOR.set( innerObject, boost );
		}

		JsonObject result = doToJsonQuery( context, outerObject, innerObject );
		return ( withConstantScore ) ? applyConstantScore( result ) : result;
	}

	protected abstract JsonObject doToJsonQuery(PredicateRequestContext context,
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

	protected abstract static class AbstractBuilder implements SearchPredicateBuilder {
		protected final ElasticsearchSearchContext searchContext;

		private Float boost;
		private boolean withConstantScore = false;

		AbstractBuilder(ElasticsearchSearchContext searchContext) {
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

	}
}
