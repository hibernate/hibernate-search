/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.sort.impl;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.backend.elasticsearch.gson.impl.JsonAccessor;
import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchContext;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchFieldCodec;
import org.hibernate.search.engine.backend.types.converter.ToDocumentFieldValueConverter;
import org.hibernate.search.engine.logging.spi.EventContexts;
import org.hibernate.search.engine.search.sort.spi.FieldSortBuilder;
import org.hibernate.search.util.impl.common.LoggerFactory;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

public class ElasticsearchFieldSortBuilder<F> extends AbstractElasticsearchSearchSortBuilder
		implements FieldSortBuilder<ElasticsearchSearchSortBuilder> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private static final JsonAccessor<JsonElement> MISSING = JsonAccessor.root().property( "missing" );
	private static final JsonPrimitive MISSING_FIRST_KEYWORD_JSON = new JsonPrimitive( "_first" );
	private static final JsonPrimitive MISSING_LAST_KEYWORD_JSON = new JsonPrimitive( "_last" );

	private final ElasticsearchSearchContext searchContext;

	private final String absoluteFieldPath;
	private final ToDocumentFieldValueConverter<?, ? extends F> dslToIndexConverter;
	private final ElasticsearchFieldCodec<F> codec;

	public ElasticsearchFieldSortBuilder(ElasticsearchSearchContext searchContext,
			String absoluteFieldPath,
			ToDocumentFieldValueConverter<?, ? extends F> dslToIndexConverter,
			ElasticsearchFieldCodec<F> codec) {
		this.searchContext = searchContext;
		this.absoluteFieldPath = absoluteFieldPath;
		this.dslToIndexConverter = dslToIndexConverter;
		this.codec = codec;
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
		JsonElement element;
		try {
			F converted = dslToIndexConverter.convertUnknown( value, searchContext.getToDocumentFieldValueConvertContext() );
			element = codec.encode( converted );
		}
		catch (RuntimeException e) {
			throw log.cannotConvertDslParameter(
					e.getMessage(), e, EventContexts.fromIndexFieldAbsolutePath( absoluteFieldPath )
			);
		}
		MISSING.set( getInnerObject(), element );
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
