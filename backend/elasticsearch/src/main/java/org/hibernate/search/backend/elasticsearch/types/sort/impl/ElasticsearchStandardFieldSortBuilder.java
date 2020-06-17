/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.types.sort.impl;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.backend.elasticsearch.gson.impl.JsonAccessor;
import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchContext;
import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchFieldContext;
import org.hibernate.search.backend.elasticsearch.search.sort.impl.ElasticsearchSearchSortBuilder;
import org.hibernate.search.backend.elasticsearch.search.sort.impl.ElasticsearchSearchSortCollector;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchFieldCodec;
import org.hibernate.search.engine.backend.types.converter.spi.DslConverter;
import org.hibernate.search.engine.search.common.ValueConvert;
import org.hibernate.search.engine.search.sort.spi.FieldSortBuilder;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

public class ElasticsearchStandardFieldSortBuilder<F> extends AbstractElasticsearchDocumentValueSortBuilder<F>
		implements FieldSortBuilder<ElasticsearchSearchSortBuilder> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private static final JsonAccessor<JsonElement> MISSING_ACCESSOR = JsonAccessor.root().property( "missing" );
	private static final JsonPrimitive MISSING_FIRST_KEYWORD_JSON = new JsonPrimitive( "_first" );
	private static final JsonPrimitive MISSING_LAST_KEYWORD_JSON = new JsonPrimitive( "_last" );

	private final ElasticsearchSearchContext searchContext;
	private final ElasticsearchFieldCodec<F> codec;

	private JsonElement missing;

	public ElasticsearchStandardFieldSortBuilder(ElasticsearchSearchContext searchContext,
			ElasticsearchSearchFieldContext<F> field, ElasticsearchFieldCodec<F> codec) {
		super( searchContext, field );
		this.searchContext = searchContext;
		this.codec = codec;
	}

	@Override
	public void missingFirst() {
		this.missing = MISSING_FIRST_KEYWORD_JSON;
	}

	@Override
	public void missingLast() {
		this.missing = MISSING_LAST_KEYWORD_JSON;
	}

	@Override
	public void missingAs(Object value, ValueConvert convert) {
		DslConverter<?, ? extends F> dslToIndexConverter = field.type().dslConverter( convert );
		try {
			F converted = dslToIndexConverter.convertUnknown( value, searchContext.toDocumentFieldValueConvertContext() );
			this.missing = codec.encodeForMissing( converted );
		}
		catch (RuntimeException e) {
			throw log.cannotConvertDslParameter( e.getMessage(), e, field.eventContext() );
		}
	}

	@Override
	public void doBuildAndAddTo(ElasticsearchSearchSortCollector collector, JsonObject innerObject) {
		if ( missing != null ) {
			MISSING_ACCESSOR.set( innerObject, missing );
		}

		if ( innerObject.size() == 0 ) {
			collector.collectSort( new JsonPrimitive( field.absolutePath() ) );
		}
		else {
			JsonObject outerObject = new JsonObject();
			outerObject.add( field.absolutePath(), innerObject );
			collector.collectSort( outerObject );
		}
	}
}
