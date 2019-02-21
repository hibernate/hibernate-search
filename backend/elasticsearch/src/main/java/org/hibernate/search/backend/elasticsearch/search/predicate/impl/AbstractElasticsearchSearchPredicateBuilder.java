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


/**
 * @author Yoann Rodiere
 */
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
	public void withConstantScore() {
		this.withConstantScore = true;
	}

	@Override
	public ElasticsearchSearchPredicateBuilder toImplementation() {
		return this;
	}

	@Override
	public final JsonObject build(ElasticsearchSearchPredicateContext context) {
		JsonObject outerObject = new JsonObject();
		JsonObject innerObject = new JsonObject();

		// TODO handle withConstantScore value here!

		if ( boost != null ) {
			BOOST_ACCESSOR.set( innerObject, boost );
		}

		return doBuild( context, outerObject, innerObject );
	}

	protected abstract JsonObject doBuild(ElasticsearchSearchPredicateContext context,
			JsonObject outerObject, JsonObject innerObject);
}
