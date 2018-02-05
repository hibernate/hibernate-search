/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.sort.impl;

import org.hibernate.search.backend.elasticsearch.gson.impl.JsonAccessor;
import org.hibernate.search.engine.search.dsl.sort.SortOrder;
import org.hibernate.search.engine.search.sort.spi.SearchSortContributor;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;


/**
 * @author Yoann Rodiere
 */
abstract class AbstractSearchSortBuilder implements SearchSortContributor<ElasticsearchSearchSortCollector> {

	private static final JsonAccessor<JsonElement> ORDER = JsonAccessor.root().property( "order" );
	private static final JsonPrimitive ASC_KEYWORD_JSON = new JsonPrimitive( "asc" );
	private static final JsonPrimitive DESC_KEYWORD_JSON = new JsonPrimitive( "desc" );

	private final JsonObject innerObject = new JsonObject();

	JsonObject getInnerObject() {
		return innerObject;
	}

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
	public abstract void contribute(ElasticsearchSearchSortCollector collector);

}
