/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.sort.impl;

import org.hibernate.search.backend.elasticsearch.gson.impl.JsonAccessor;
import org.hibernate.search.engine.search.dsl.sort.SortOrder;
import org.hibernate.search.engine.search.sort.spi.SearchSortBuilder;
import org.hibernate.search.util.AssertionFailure;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;


/**
 * @author Yoann Rodiere
 */
abstract class AbstractElasticsearchSearchSortBuilder implements SearchSortBuilder<ElasticsearchSearchSortBuilder>,
		ElasticsearchSearchSortBuilder {

	private static final JsonAccessor<JsonElement> ORDER = JsonAccessor.root().property( "order" );
	private static final JsonPrimitive ASC_KEYWORD_JSON = new JsonPrimitive( "asc" );
	private static final JsonPrimitive DESC_KEYWORD_JSON = new JsonPrimitive( "desc" );

	private final JsonObject innerObject = new JsonObject();

	private boolean built;

	@Override
	public ElasticsearchSearchSortBuilder toImplementation() {
		return this;
	}

	@Override
	public void order(SortOrder order) {
		switch ( order ) {
			case ASC:
				ORDER.set( getInnerObject(), ASC_KEYWORD_JSON );
				break;
			case DESC:
				ORDER.set( getInnerObject(), DESC_KEYWORD_JSON );
				break;
		}
	}

	@Override
	public final void buildAndAddTo(ElasticsearchSearchSortCollector collector) {
		if ( built ) {
			// we must never call a builder twice. Building may have side-effects.
			throw new AssertionFailure(
					"A sort builder was called twice. There is a bug in Hibernate Search, please report it."
			);
		}
		built = true;
		doBuildAndAddTo( collector );
	}

	protected final JsonObject getInnerObject() {
		return innerObject;
	}

	protected abstract void doBuildAndAddTo(ElasticsearchSearchSortCollector collector);

}
