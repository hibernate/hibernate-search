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
import org.hibernate.search.backend.elasticsearch.search.impl.AbstractElasticsearchCodecAwareSearchFieldQueryElementFactory;
import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchContext;
import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchValueFieldContext;
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

	private ElasticsearchStandardFieldSort(Builder<?> builder) {
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

	public static class Factory<F>
			extends AbstractElasticsearchCodecAwareSearchFieldQueryElementFactory<FieldSortBuilder, F> {
		public Factory(ElasticsearchFieldCodec<F> codec) {
			super( codec );
		}

		@Override
		public Builder<F> create(ElasticsearchSearchContext searchContext, ElasticsearchSearchValueFieldContext<F> field) {
			return new Builder<>( codec, searchContext, field );
		}
	}

	private static class Builder<F> extends AbstractBuilder<F> implements FieldSortBuilder {
		private final ElasticsearchFieldCodec<F> codec;

		private JsonElement missing;

		protected Builder(ElasticsearchFieldCodec<F> codec, ElasticsearchSearchContext searchContext,
				ElasticsearchSearchValueFieldContext<F> field) {
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

	public static class TemporalFieldFactory<F extends TemporalAccessor> extends Factory<F> {
		public TemporalFieldFactory(ElasticsearchFieldCodec<F> codec) {
			super( codec );
		}

		@Override
		public TemporalFieldBuilder<F> create(ElasticsearchSearchContext searchContext,
				ElasticsearchSearchValueFieldContext<F> field) {
			return new TemporalFieldBuilder<>( codec, searchContext, field );
		}
	}

	private static class TemporalFieldBuilder<F extends TemporalAccessor> extends Builder<F> {
		private TemporalFieldBuilder(ElasticsearchFieldCodec<F> codec, ElasticsearchSearchContext searchContext,
				ElasticsearchSearchValueFieldContext<F> field) {
			super( codec, searchContext, field );
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

	public static class TextFieldFactory extends Factory<String> {
		public TextFieldFactory(ElasticsearchFieldCodec<String> codec) {
			super( codec );
		}

		@Override
		public TextFieldBuilder create(ElasticsearchSearchContext searchContext,
				ElasticsearchSearchValueFieldContext<String> field) {
			return new TextFieldBuilder( codec, searchContext, field );
		}
	}

	private static class TextFieldBuilder extends Builder<String> {
		private TextFieldBuilder(ElasticsearchFieldCodec<String> codec, ElasticsearchSearchContext searchContext,
				ElasticsearchSearchValueFieldContext<String> field) {
			super( codec, searchContext, field );
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
