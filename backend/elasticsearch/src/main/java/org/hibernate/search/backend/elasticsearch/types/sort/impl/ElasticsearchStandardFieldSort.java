package org.hibernate.search.backend.elasticsearch.types.sort.impl;

import java.time.temporal.TemporalAccessor;

import org.hibernate.search.backend.elasticsearch.json.impl.JsonAccessor;
import org.hibernate.search.backend.elasticsearch.json.impl.JsonScalarConversions;
import org.hibernate.search.backend.elasticsearch.logging.impl.QueryLog;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.DataTypes;
import org.hibernate.search.backend.elasticsearch.search.common.impl.AbstractElasticsearchCodecAwareSearchQueryElementFactory;
import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexScope;
import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexValueFieldContext;
import org.hibernate.search.backend.elasticsearch.search.sort.impl.ElasticsearchSearchSortCollector;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchFieldCodec;
import org.hibernate.search.engine.search.common.SortMode;
import org.hibernate.search.engine.search.common.ValueModel;
import org.hibernate.search.engine.search.sort.SearchSort;
import org.hibernate.search.engine.search.sort.dsl.SortOrder;
import org.hibernate.search.engine.search.sort.spi.FieldSortBuilder;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

public class ElasticsearchStandardFieldSort extends AbstractElasticsearchDocumentValueSort {

	private static final JsonAccessor<JsonNode> MISSING_ACCESSOR = JsonAccessor.root().property( "missing" );

	private static final JsonNode MISSING_FIRST_KEYWORD_JSON = JsonScalarConversions.toNode( "_first" );

	private static final JsonNode MISSING_LAST_KEYWORD_JSON = JsonScalarConversions.toNode( "_last" );

	private static final JsonNode MISSING_HIGHEST_KEYWORD_JSON = JsonScalarConversions.toNode( "_highest_wont_work" );

	private static final JsonNode MISSING_LOWEST_KEYWORD_JSON = JsonScalarConversions.toNode( "_lowest_wont_work" );

	private static final JsonAccessor<JsonNode> UNMAPPED_TYPE = JsonAccessor.root().property( "unmapped_type" );

	private final JsonNode missing;

	private final JsonNode unmappedType;

	private ElasticsearchStandardFieldSort(Builder<?> builder) {
		super( builder );
		missing = builder.missing;
		unmappedType = builder.field.type().elasticsearchTypeAsJson();
	}

	@Override
	public void doToJsonSorts(ElasticsearchSearchSortCollector collector, ObjectNode innerObject) {
		if ( missing != null ) {
			MISSING_ACCESSOR.set( innerObject, missing );
		}
		// We cannot use unmapped_type for scaled floats:
		// Elasticsearch complains it needs a scaling factor, but we don't have any way to provide it.
		// See https://hibernate.atlassian.net/browse/HSEARCH-4176
		if ( unmappedType != null && !DataTypes.SCALED_FLOAT.equals( JsonScalarConversions.asString( unmappedType ) ) ) {
			// If there are multiple target indexes, or if the field is dynamic,
			// some target indexes may not have this field in their mapping (yet),
			// and in that case Elasticsearch would raise an exception.
			// Instruct ES to behave as if the field had no value in that case.
			UNMAPPED_TYPE.set( innerObject, unmappedType );
		}
		if ( innerObject.size() == 0 ) {
			collector.collectSort( JsonScalarConversions.toNode( absoluteFieldPath ) );
		}
		else {
			ObjectNode outerObject = new ObjectNode( JsonNodeFactory.instance );
			outerObject.set( absoluteFieldPath, innerObject );
			collector.collectSort( outerObject );
		}
	}

	public static class Factory<F> extends AbstractElasticsearchCodecAwareSearchQueryElementFactory<FieldSortBuilder, F> {

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

		private JsonNode missing;

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
		public void missingAs(Object value, ValueModel valueModel) {
			this.missing = field.encodingContext().convertAndEncode( scope, field, value, valueModel,
					ElasticsearchFieldCodec::encodeForMissing );
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
					throw QueryLog.INSTANCE.invalidSortModeForTemporalField( mode, field.eventContext() );
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
					throw QueryLog.INSTANCE.invalidSortModeForStringField( mode, field.eventContext() );
			}
		}
	}
}
