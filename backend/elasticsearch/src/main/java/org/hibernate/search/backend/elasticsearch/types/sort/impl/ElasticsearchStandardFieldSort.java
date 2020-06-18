/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.types.sort.impl;

import java.lang.invoke.MethodHandles;
import java.time.temporal.TemporalAccessor;

import org.hibernate.search.backend.elasticsearch.gson.impl.JsonAccessor;
import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchContext;
import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchFieldContext;
import org.hibernate.search.backend.elasticsearch.search.sort.impl.ElasticsearchSearchSortCollector;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchFieldCodec;
import org.hibernate.search.engine.backend.types.converter.spi.DslConverter;
import org.hibernate.search.engine.search.common.SortMode;
import org.hibernate.search.engine.search.common.ValueConvert;
import org.hibernate.search.engine.search.sort.SearchSort;
import org.hibernate.search.engine.search.sort.spi.FieldSortBuilder;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

public class ElasticsearchStandardFieldSort extends AbstractElasticsearchDocumentValueSort {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private static final JsonAccessor<JsonElement> MISSING_ACCESSOR = JsonAccessor.root().property( "missing" );
	private static final JsonPrimitive MISSING_FIRST_KEYWORD_JSON = new JsonPrimitive( "_first" );
	private static final JsonPrimitive MISSING_LAST_KEYWORD_JSON = new JsonPrimitive( "_last" );

	private final JsonElement missing;

	public ElasticsearchStandardFieldSort(Builder<?> builder) {
		super( builder );
		missing = builder.missing;
	}

	@Override
	public void doToJsonSorts(ElasticsearchSearchSortCollector collector, JsonObject innerObject) {
		if ( missing != null ) {
			MISSING_ACCESSOR.set( innerObject, missing );
		}

		if ( innerObject.size() == 0 ) {
			collector.collectSort( new JsonPrimitive( absoluteFieldPath ) );
		}
		else {
			JsonObject outerObject = new JsonObject();
			outerObject.add( absoluteFieldPath, innerObject );
			collector.collectSort( outerObject );
		}
	}

	public static class Builder<F> extends AbstractBuilder<F> implements FieldSortBuilder {
		private final ElasticsearchFieldCodec<F> codec;

		private JsonElement missing;

		public Builder(ElasticsearchSearchContext searchContext, ElasticsearchSearchFieldContext<F> field,
				ElasticsearchFieldCodec<F> codec) {
			super( searchContext, field );
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
		public SearchSort build() {
			return new ElasticsearchStandardFieldSort( this );
		}
	}

	public static class TemporalFieldBuilder<F extends TemporalAccessor> extends Builder<F> {
		public TemporalFieldBuilder(ElasticsearchSearchContext searchContext, ElasticsearchSearchFieldContext<F> field,
				ElasticsearchFieldCodec<F> codec) {
			super( searchContext, field, codec );
		}

		@Override
		public void mode(SortMode mode) {
			switch ( mode ) {
				case MIN:
				case MAX:
				case AVG:
				case MEDIAN:
					super.mode( mode );
					break;
				case SUM:
				default:
					throw log.cannotComputeSumForTemporalField( field.eventContext() );
			}
		}
	}

	public static class TextFieldBuilder extends Builder<String> {
		public TextFieldBuilder(ElasticsearchSearchContext searchContext, ElasticsearchSearchFieldContext<String> field,
				ElasticsearchFieldCodec<String> codec) {
			super( searchContext, field, codec );
		}

		@Override
		public void mode(SortMode mode) {
			switch ( mode ) {
				case MIN:
				case MAX:
					super.mode( mode );
					break;
				case SUM:
				case AVG:
				case MEDIAN:
				default:
					throw log.cannotComputeSumOrAvgOrMedianForStringField( field.eventContext() );
			}
		}
	}
}
