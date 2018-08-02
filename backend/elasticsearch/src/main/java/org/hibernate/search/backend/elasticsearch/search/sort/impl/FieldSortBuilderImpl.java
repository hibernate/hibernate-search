/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.sort.impl;

import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexSchemaFieldNode;
import org.hibernate.search.backend.elasticsearch.gson.impl.JsonAccessor;
import org.hibernate.search.backend.elasticsearch.types.converter.impl.ElasticsearchFieldConverter;
import org.hibernate.search.engine.search.sort.spi.FieldSortBuilder;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

class FieldSortBuilderImpl<F> extends AbstractSearchSortBuilder
		implements FieldSortBuilder<ElasticsearchSearchSortBuilder> {

	private static final JsonAccessor<JsonElement> MISSING = JsonAccessor.root().property( "missing" );
	private static final JsonPrimitive MISSING_FIRST_KEYWORD_JSON = new JsonPrimitive( "_first" );
	private static final JsonPrimitive MISSING_LAST_KEYWORD_JSON = new JsonPrimitive( "_last" );

	private final String absoluteFieldPath;
	private final ElasticsearchFieldConverter converter;

	FieldSortBuilderImpl(String absoluteFieldPath, ElasticsearchIndexSchemaFieldNode<F> node) {
		this.absoluteFieldPath = absoluteFieldPath;
		this.converter = node.getConverter();
	}

	@Override
	public void missingFirst() {
		MISSING.set( getInnerObject(), MISSING_FIRST_KEYWORD_JSON );
	}

	@Override
	public void missingLast() {
		MISSING.set( getInnerObject(), MISSING_LAST_KEYWORD_JSON );
	}

	@Override
	public void missingAs(Object value) {
		MISSING.set( getInnerObject(), converter.convertFromDsl( value ) );
	}

	@Override
	public void doBuildAndAddTo(ElasticsearchSearchSortCollector collector) {
		JsonObject innerObject = getInnerObject();
		if ( innerObject.size() == 0 ) {
			collector.collectSort( new JsonPrimitive( absoluteFieldPath ) );
		}
		else {
			JsonObject outerObject = new JsonObject();
			outerObject.add( absoluteFieldPath, innerObject );
			collector.collectSort( outerObject );
		}
	}
}
