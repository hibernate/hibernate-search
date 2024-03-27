/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.types.sort.impl;

import java.lang.invoke.MethodHandles;
import java.time.temporal.TemporalAccessor;

import org.hibernate.search.backend.elasticsearch.gson.impl.JsonAccessor;
import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.DataTypes;
import org.hibernate.search.backend.elasticsearch.search.common.impl.AbstractElasticsearchCodecAwareSearchQueryElementFactory;
import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexScope;
import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexValueFieldContext;
import org.hibernate.search.backend.elasticsearch.search.sort.impl.ElasticsearchSearchSortCollector;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchFieldCodec;
import org.hibernate.search.engine.backend.types.converter.spi.DslConverter;
import org.hibernate.search.engine.search.common.SortMode;
import org.hibernate.search.engine.search.common.ValueConvert;
import org.hibernate.search.engine.search.sort.SearchSort;
import org.hibernate.search.engine.search.sort.dsl.SortOrder;
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
	private static final JsonPrimitive MISSING_HIGHEST_KEYWORD_JSON = new JsonPrimitive( "_highest_wont_work" );
	private static final JsonPrimitive MISSING_LOWEST_KEYWORD_JSON = new JsonPrimitive( "_lowest_wont_work" );
	private static final JsonAccessor<JsonElement> UNMAPPED_TYPE = JsonAccessor.root().property( "unmapped_type" );

	private final JsonElement missing;
	private final JsonPrimitive unmappedType;

	private ElasticsearchStandardFieldSort(Builder<?> builder) {
		super( builder );
		missing = builder.missing;
		unmappedType = builder.field.type().elasticsearchTypeAsJson();
	}

	@Override
	public void doToJsonSorts(ElasticsearchSearchSortCollector collector, JsonObject innerObject) {
		if ( missing != null ) {
			MISSING_ACCESSOR.set( innerObject, missing );
		}

		// We cannot use unmapped_type for scaled floats:
		// Elasticsearch complains it needs a scaling factor, but we don't have any way to provide it.
		// See https://hibernate.atlassian.net/browse/HSEARCH-4176
		if ( unmappedType != null && !DataTypes.SCALED_FLOAT.equals( unmappedType.getAsString() ) ) {
			// If there are multiple target indexes, or if the field is dynamic,
			// some target indexes may not have this field in their mapping (yet),
			// and in that case Elasticsearch would raise an exception.
			// Instruct ES to behave as if the field had no value in that case.
			UNMAPPED_TYPE.set( innerObject, unmappedType );
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
			extends AbstractElasticsearchCodecAwareSearchQueryElementFactory<FieldSortBuilder, F> {
		public Factory(ElasticsearchFieldCodec<F> codec) {
			super( codec );
		}

		@Override
		public Builder<F> create(ElasticsearchSearchIndexScope<?> scope, ElasticsearchSearchIndexValueFieldContext<F> field) {
			return new Builder<>( codec, scope, field );
		}
	}

	private static class Builder<F> extends AbstractBuilder<F> implements FieldSortBuilder {
		private final ElasticsearchFieldCodec<F> codec;

		private JsonElement missing;

		protected Builder(ElasticsearchFieldCodec<F> codec, ElasticsearchSearchIndexScope<?> scope,
				ElasticsearchSearchIndexValueFieldContext<F> field) {
			super( scope, field );
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
		public void missingHighest() {
			this.missing = MISSING_HIGHEST_KEYWORD_JSON;
		}

		@Override
		public void missingLowest() {
			this.missing = MISSING_LOWEST_KEYWORD_JSON;
		}

		@Override
		public void missingAs(Object value, ValueConvert convert) {
			DslConverter<?, ? extends F> dslToIndexConverter = field.type().dslConverter( convert );
			try {
				F converted = dslToIndexConverter.unknownTypeToDocumentValue( value, scope.toDocumentValueConvertContext() );
				this.missing = codec.encodeForMissing( converted );
			}
			catch (RuntimeException e) {
				throw log.cannotConvertDslParameter( e.getMessage(), e, field.eventContext() );
			}
		}

		@Override
		public SearchSort build() {
			if ( MISSING_HIGHEST_KEYWORD_JSON.equals( missing ) ) {
				this.missing = this.order == null || SortOrder.ASC.equals( this.order )
						? MISSING_LAST_KEYWORD_JSON
						: MISSING_FIRST_KEYWORD_JSON;
			}
			if ( MISSING_LOWEST_KEYWORD_JSON.equals( missing ) ) {
				this.missing = this.order == null || SortOrder.ASC.equals( this.order )
						? MISSING_FIRST_KEYWORD_JSON
						: MISSING_LAST_KEYWORD_JSON;
			}
			return new ElasticsearchStandardFieldSort( this );
		}
	}

	public static class TemporalFieldFactory<F extends TemporalAccessor> extends Factory<F> {
		public TemporalFieldFactory(ElasticsearchFieldCodec<F> codec) {
			super( codec );
		}

		@Override
		public TemporalFieldBuilder<F> create(ElasticsearchSearchIndexScope<?> scope,
				ElasticsearchSearchIndexValueFieldContext<F> field) {
			return new TemporalFieldBuilder<>( codec, scope, field );
		}
	}

	private static class TemporalFieldBuilder<F extends TemporalAccessor> extends Builder<F> {
		private TemporalFieldBuilder(ElasticsearchFieldCodec<F> codec, ElasticsearchSearchIndexScope<?> scope,
				ElasticsearchSearchIndexValueFieldContext<F> field) {
			super( codec, scope, field );
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
					throw log.invalidSortModeForTemporalField( mode, field.eventContext() );
			}
		}
	}

	public static class TextFieldFactory extends Factory<String> {
		public TextFieldFactory(ElasticsearchFieldCodec<String> codec) {
			super( codec );
		}

		@Override
		public TextFieldBuilder create(ElasticsearchSearchIndexScope<?> scope,
				ElasticsearchSearchIndexValueFieldContext<String> field) {
			return new TextFieldBuilder( codec, scope, field );
		}
	}

	private static class TextFieldBuilder extends Builder<String> {
		private TextFieldBuilder(ElasticsearchFieldCodec<String> codec, ElasticsearchSearchIndexScope<?> scope,
				ElasticsearchSearchIndexValueFieldContext<String> field) {
			super( codec, scope, field );
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
					throw log.invalidSortModeForStringField( mode, field.eventContext() );
			}
		}
	}
}
