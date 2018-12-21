/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.predicate.impl;

import org.hibernate.search.backend.elasticsearch.gson.impl.JsonAccessor;
import org.hibernate.search.engine.search.predicate.spi.SearchPredicateBuilder;
import org.hibernate.search.util.AssertionFailure;

import com.google.gson.JsonObject;


/**
 * @author Yoann Rodiere
 */
public abstract class AbstractElasticsearchSearchPredicateBuilder
		implements SearchPredicateBuilder<ElasticsearchSearchPredicateBuilder>,
				ElasticsearchSearchPredicateBuilder {

	private static final JsonAccessor<Float> BOOST = JsonAccessor.root().property( "boost" ).asFloat();

	private Float boost;

	@Override
	public void boost(float boost) {
		this.boost = boost;
	}

	@Override
	public ElasticsearchSearchPredicateBuilder toImplementation() {
		return this;
	}

	@Override
	public final JsonObject build() {
		JsonObject outerObject = new JsonObject();
		JsonObject innerObject = new JsonObject();

		if ( boost != null ) {
			BOOST.set( innerObject, boost );
		}

		return doBuild( outerObject, innerObject );
	}

	protected abstract JsonObject doBuild(JsonObject outerObject, JsonObject innerObject);
}
