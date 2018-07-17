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
		implements NestedPredicateBuilder<ElasticsearchSearchPredicateBuilder> {

	private static final JsonAccessor<String> PATH = JsonAccessor.root().property( "path" ).asString();
	private static final JsonAccessor<JsonObject> QUERY = JsonAccessor.root().property( "query" ).asObject();

	private final String absoluteFieldPath;

	private ElasticsearchSearchPredicateBuilder nestedBuilder;

	NestedPredicateBuilderImpl(String absoluteFieldPath) {
		this.absoluteFieldPath = absoluteFieldPath;
	}

	@Override
	public void nested(ElasticsearchSearchPredicateBuilder nestedBuilder) {
		this.nestedBuilder = nestedBuilder;
	}

	@Override
	protected JsonObject doBuild() {
		JsonObject outerObject = getOuterObject();
		JsonObject innerObject = getInnerObject();
		PATH.set( innerObject, absoluteFieldPath );
		QUERY.set( innerObject, nestedBuilder.build() );
		outerObject.add( "nested", getInnerObject() );
		return outerObject;
	}

}
