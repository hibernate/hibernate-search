package org.hibernate.search.backend.elasticsearch.types.predicate.impl;

import java.util.List;

import org.hibernate.search.backend.elasticsearch.gson.impl.JsonAccessor;
import org.hibernate.search.backend.elasticsearch.search.common.impl.AbstractElasticsearchCodecAwareSearchQueryElementFactory;
import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexScope;
import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexValueFieldContext;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchFieldCodec;
import org.hibernate.search.engine.search.common.ValueModel;
import org.hibernate.search.engine.search.predicate.spi.FieldValueFactorFunctionBuilder;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class ElasticsearchFieldValueFactorFunction {

	private static final JsonAccessor<String> FIELD_ACCESSOR = JsonAccessor.root().property( "field" ).asString();
	private static final JsonAccessor<String> MODIFIER_ACCESSOR = JsonAccessor.root().property( "modifier" ).asString();
	private static final JsonAccessor<Float> FACTOR_ACCESSOR = JsonAccessor.root().property( "factor" ).asFloat();
	private static final JsonAccessor<JsonElement> MISSING_ACCESSOR = JsonAccessor.root().property( "missing" );

	private ElasticsearchFieldValueFactorFunction() {
	}

	public static class Factory<F>
			extends AbstractElasticsearchCodecAwareSearchQueryElementFactory<FieldValueFactorFunctionBuilder, F> {
		public Factory(ElasticsearchFieldCodec<F> codec) {
			super( codec );
		}

		@Override
		public FieldValueFactorFunctionBuilder create(ElasticsearchSearchIndexScope<?> scope,
				ElasticsearchSearchIndexValueFieldContext<F> field) {
			return new Builder<>( codec, scope, field );
		}
	}

	private enum Modifier {
		NONE( "none" ),
		LOG( "log" ),
		LOG1P( "log1p" ),
		LOG2P( "log2p" ),
		LN( "ln" ),
		LN1P( "ln1p" ),
		LN2P( "ln2p" ),
		SQUARE( "square" ),
		SQRT( "sqrt" ),
		RECIPROCAL( "reciprocal" );

		private final String jsonValue;

		Modifier(String jsonValue) {
			this.jsonValue = jsonValue;
		}
	}

	public static class Builder<F> implements FieldValueFactorFunctionBuilder {

		private final ElasticsearchFieldCodec<F> codec;
		private final ElasticsearchSearchIndexScope<?> scope;
		private final ElasticsearchSearchIndexValueFieldContext<F> field;
		private final List<String> nestedPathHierarchy;

		private Modifier modifier = Modifier.NONE;
		private Float factor;
		private JsonElement missing;

		Builder(ElasticsearchFieldCodec<F> codec, ElasticsearchSearchIndexScope<?> scope,
				ElasticsearchSearchIndexValueFieldContext<F> field) {
			this.codec = codec;
			this.scope = scope;
			this.field = field;
			this.nestedPathHierarchy = field.nestedPathHierarchy();
		}

		@Override
		public List<String> nestedPathHierarchy() {
			return nestedPathHierarchy;
		}

		@Override
		public void modifierNone() {
			this.modifier = Modifier.NONE;
		}

		@Override
		public void modifierLog() {
			this.modifier = Modifier.LOG;
		}

		@Override
		public void modifierLog1p() {
			this.modifier = Modifier.LOG1P;
		}

		@Override
		public void modifierLog2p() {
			this.modifier = Modifier.LOG2P;
		}

		@Override
		public void modifierLn() {
			this.modifier = Modifier.LN;
		}

		@Override
		public void modifierLn1p() {
			this.modifier = Modifier.LN1P;
		}

		@Override
		public void modifierLn2p() {
			this.modifier = Modifier.LN2P;
		}

		@Override
		public void modifierSquare() {
			this.modifier = Modifier.SQUARE;
		}

		@Override
		public void modifierSqrt() {
			this.modifier = Modifier.SQRT;
		}

		@Override
		public void modifierReciprocal() {
			this.modifier = Modifier.RECIPROCAL;
		}

		@Override
		public void factor(float factor) {
			this.factor = factor;
		}

		@Override
		public void missing(Object value, ValueModel valueModel) {
			this.missing = field.encodingContext().convertAndEncode( scope, field, value, valueModel,
					ElasticsearchFieldCodec::encode );
		}

		public String fieldPath() {
			return field.absolutePath();
		}

		public JsonObject toJson() {
			JsonObject fvf = new JsonObject();
			FIELD_ACCESSOR.set( fvf, field.absolutePath() );
			if ( modifier != Modifier.NONE ) {
				MODIFIER_ACCESSOR.set( fvf, modifier.jsonValue );
			}
			if ( factor != null ) {
				FACTOR_ACCESSOR.set( fvf, factor );
			}
			if ( missing != null ) {
				MISSING_ACCESSOR.set( fvf, missing );
			}
			return fvf;
		}
	}

}
