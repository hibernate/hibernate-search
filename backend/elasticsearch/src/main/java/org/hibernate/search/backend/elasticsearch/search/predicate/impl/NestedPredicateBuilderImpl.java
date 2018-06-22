/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.predicate.impl;

import java.util.function.Consumer;

import org.hibernate.search.backend.elasticsearch.gson.impl.JsonAccessor;
import org.hibernate.search.engine.search.predicate.spi.NestedPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.SearchPredicateContributor;

import com.google.gson.JsonObject;


/**
 * @author Yoann Rodiere
 */
class NestedPredicateBuilderImpl extends AbstractSearchPredicateBuilder
		implements NestedPredicateBuilder<Void, ElasticsearchSearchPredicateCollector> {

	private static final JsonAccessor<String> PATH = JsonAccessor.root().property( "path" ).asString();
	private static final JsonAccessor<JsonObject> QUERY = JsonAccessor.root().property( "query" ).asObject();

	private final String absoluteFieldPath;

	private SearchPredicateContributor<Void, ? super ElasticsearchSearchPredicateCollector> nestedContributor;

	NestedPredicateBuilderImpl(String absoluteFieldPath) {
		this.absoluteFieldPath = absoluteFieldPath;
	}

	@Override
	public Consumer<SearchPredicateContributor<Void, ? super ElasticsearchSearchPredicateCollector>> getNestedCollector() {
		return this::nested;
	}

	private void nested(SearchPredicateContributor<Void, ? super ElasticsearchSearchPredicateCollector> nestedContributor) {
		this.nestedContributor = nestedContributor;
	}

	@Override
	public void contribute(Void context, ElasticsearchSearchPredicateCollector collector) {
		JsonObject outerObject = getOuterObject();
		JsonObject innerObject = getInnerObject();
		PATH.set( innerObject, absoluteFieldPath );
		QUERY.set( innerObject, getQueryFromContributor( nestedContributor ) );
		outerObject.add( "nested", getInnerObject() );
		collector.collectPredicate( outerObject );
	}

}
