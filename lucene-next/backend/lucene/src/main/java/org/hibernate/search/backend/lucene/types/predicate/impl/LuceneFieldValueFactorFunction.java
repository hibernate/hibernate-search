package org.hibernate.search.backend.lucene.types.predicate.impl;

import java.util.List;

import org.hibernate.search.backend.lucene.search.common.impl.AbstractLuceneCodecAwareSearchQueryElementFactory;
import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexScope;
import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexValueFieldContext;
import org.hibernate.search.backend.lucene.search.predicate.impl.LuceneFunctionScorePredicate;
import org.hibernate.search.backend.lucene.types.codec.impl.AbstractLuceneNumericFieldCodec;
import org.hibernate.search.engine.search.common.ValueModel;
import org.hibernate.search.engine.search.predicate.spi.FieldValueFactorFunctionBuilder;
import org.hibernate.search.util.common.reporting.EventContext;

public class LuceneFieldValueFactorFunction {

	private LuceneFieldValueFactorFunction() {
	}

	public static class Factory<F, E extends Number>
			extends AbstractLuceneCodecAwareSearchQueryElementFactory<FieldValueFactorFunctionBuilder,
					F,
					AbstractLuceneNumericFieldCodec<F, E>> {
		public Factory(AbstractLuceneNumericFieldCodec<F, E> codec) {
			super( codec );
		}

		@Override
		public FieldValueFactorFunctionBuilder create(LuceneSearchIndexScope<?> scope,
				LuceneSearchIndexValueFieldContext<F> field) {
			return new Builder<>( codec, scope, field );
		}
	}

	public static class Builder<F, E extends Number> implements FieldValueFactorFunctionBuilder {

		private final AbstractLuceneNumericFieldCodec<F, E> codec;
		private final LuceneSearchIndexScope<?> scope;
		private final LuceneSearchIndexValueFieldContext<F> field;
		private final List<String> nestedPathHierarchy;

		private LuceneFunctionScorePredicate.Modifier modifier = LuceneFunctionScorePredicate.Modifier.NONE;
		private float factor = 1.0f;
		private Double missing;

		Builder(AbstractLuceneNumericFieldCodec<F, E> codec, LuceneSearchIndexScope<?> scope,
				LuceneSearchIndexValueFieldContext<F> field) {
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
			this.modifier = LuceneFunctionScorePredicate.Modifier.NONE;
		}

		@Override
		public void modifierLog() {
			this.modifier = LuceneFunctionScorePredicate.Modifier.LOG;
		}

		@Override
		public void modifierLog1p() {
			this.modifier = LuceneFunctionScorePredicate.Modifier.LOG1P;
		}

		@Override
		public void modifierLog2p() {
			this.modifier = LuceneFunctionScorePredicate.Modifier.LOG2P;
		}

		@Override
		public void modifierLn() {
			this.modifier = LuceneFunctionScorePredicate.Modifier.LN;
		}

		@Override
		public void modifierLn1p() {
			this.modifier = LuceneFunctionScorePredicate.Modifier.LN1P;
		}

		@Override
		public void modifierLn2p() {
			this.modifier = LuceneFunctionScorePredicate.Modifier.LN2P;
		}

		@Override
		public void modifierSquare() {
			this.modifier = LuceneFunctionScorePredicate.Modifier.SQUARE;
		}

		@Override
		public void modifierSqrt() {
			this.modifier = LuceneFunctionScorePredicate.Modifier.SQRT;
		}

		@Override
		public void modifierReciprocal() {
			this.modifier = LuceneFunctionScorePredicate.Modifier.RECIPROCAL;
		}

		@Override
		public void factor(float factor) {
			this.factor = factor;
		}

		@Override
		public void missing(Object value, ValueModel valueModel) {
			E encoded = field.encodingContext().convertAndEncode( scope, field, codec, value, valueModel );
			this.missing = codec.encodedValueToDouble( encoded );
		}

		public EventContext eventContext() {
			return field.eventContext();
		}

		public String absoluteFieldPath() {
			return field.absolutePath();
		}

		public AbstractLuceneNumericFieldCodec<F, ?> codec() {
			return codec;
		}

		public LuceneFunctionScorePredicate.Modifier modifier() {
			return modifier;
		}

		public float factor() {
			return factor;
		}

		public Double missing() {
			return missing;
		}
	}

}
