/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.sort.impl;

import org.hibernate.search.backend.elasticsearch.gson.impl.JsonAccessor;
import org.hibernate.search.engine.search.sort.dsl.SortOrder;
import org.hibernate.search.engine.search.sort.spi.SearchSortBuilder;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;



abstract class AbstractElasticsearchSearchSortBuilder implements SearchSortBuilder<ElasticsearchSearchSortBuilder>,
		ElasticsearchSearchSortBuilder {

	private static final JsonAccessor<JsonElement> ORDER = JsonAccessor.root().property( "order" );
	private static final JsonPrimitive ASC_KEYWORD_JSON = new JsonPrimitive( "asc" );
	private static final JsonPrimitive DESC_KEYWORD_JSON = new JsonPrimitive( "desc" );

	private SortOrder order;

	@Override
	public ElasticsearchSearchSortBuilder toImplementation() {
		return this;
	}

	@Override
	public void order(SortOrder order) {
		this.order = order;
	}

	@Override
	public final void buildAndAddTo(ElasticsearchSearchSortCollector collector) {
		JsonObject innerObject = new JsonObject();
		if ( order != null ) {
			switch ( order ) {
				case ASC:
					ORDER.set( innerObject, ASC_KEYWORD_JSON );
					break;
				case DESC:
					ORDER.set( innerObject, DESC_KEYWORD_JSON );
					break;
			}
		}
		enrichInnerObject( innerObject );
		doBuildAndAddTo( collector, innerObject );
	}

	protected void enrichInnerObject(JsonObject innerObject) {
	}

	protected abstract void doBuildAndAddTo(ElasticsearchSearchSortCollector collector, JsonObject innerObject);

}
