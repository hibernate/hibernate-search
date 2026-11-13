package org.hibernate.search.engine.search.predicate.dsl.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.hibernate.search.engine.search.common.ValueModel;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.dsl.FunctionScoreBoostModeStep;
import org.hibernate.search.engine.search.predicate.dsl.FunctionScoreClausesStep;
import org.hibernate.search.engine.search.predicate.dsl.FunctionScoreFieldValueFactorModifierStep;
import org.hibernate.search.engine.search.predicate.dsl.FunctionScoreFieldValueFactorStep;
import org.hibernate.search.engine.search.predicate.dsl.FunctionScoreScoreModeStep;
import org.hibernate.search.engine.search.predicate.dsl.FunctionScoreWeightStep;
import org.hibernate.search.engine.search.predicate.dsl.PredicateFinalStep;
import org.hibernate.search.engine.search.predicate.dsl.TypedSearchPredicateFactory;
import org.hibernate.search.engine.search.predicate.dsl.spi.AbstractPredicateFinalStep;
import org.hibernate.search.engine.search.predicate.dsl.spi.SearchPredicateDslContext;
import org.hibernate.search.engine.search.predicate.spi.FieldValueFactorFunctionBuilder;
import org.hibernate.search.engine.search.predicate.spi.FunctionScorePredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.ScoreFunctionTypeKeys;
import org.hibernate.search.engine.search.reference.scorefunction.FieldValueFactorFieldReference;

public final class FunctionScoreClausesStepImpl<SR>
		extends AbstractPredicateFinalStep
		implements FunctionScoreClausesStep<SR, FunctionScoreClausesStepImpl<SR>> {

	private final TypedSearchPredicateFactory<SR> factory;
	private final FunctionScorePredicateBuilder builder;
	private final ScoreModeStepImpl scoreModeStep = new ScoreModeStepImpl();
	private final BoostModeStepImpl boostModeStep = new BoostModeStepImpl();
	private final List<FunctionStep> functions = new ArrayList<>();

	public FunctionScoreClausesStepImpl(SearchPredicateDslContext<?> dslContext,
			TypedSearchPredicateFactory<SR> factory) {
		super( dslContext );
		this.factory = factory;
		this.builder = dslContext.scope().predicateBuilders().functionScore();
	}

	// -- Inner predicate --

	public FunctionScoreClausesStepImpl<SR> innerPredicate(SearchPredicate predicate) {
		builder.innerPredicate( predicate );
		return this;
	}

	public FunctionScoreClausesStepImpl<SR> innerPredicate(PredicateFinalStep predicateStep) {
		return innerPredicate( predicateStep.toPredicate() );
	}

	// -- FunctionScoreClausesStep --

	@Override
	public FunctionScoreFieldValueFactorStep<SR, FunctionScoreClausesStepImpl<SR>> fieldValueFactor(String fieldPath) {
		return addFieldValueFactor( fieldPath, ValueModel.MAPPING );
	}

	@Override
	public <T> FunctionScoreFieldValueFactorStep<SR, FunctionScoreClausesStepImpl<SR>> fieldValueFactor(
			FieldValueFactorFieldReference<? super SR, T> fieldReference) {
		return addFieldValueFactor( fieldReference.absolutePath(), fieldReference.valueModel() );
	}

	private FieldValueFactorStepImpl addFieldValueFactor(String fieldPath, ValueModel valueModel) {
		FieldValueFactorStepImpl step = new FieldValueFactorStepImpl(
				dslContext.scope().fieldQueryElement( fieldPath, ScoreFunctionTypeKeys.FIELD_VALUE_FACTOR ), valueModel );
		functions.add( step );
		return step;
	}

	@Override
	public FunctionScoreWeightStep<SR, FunctionScoreClausesStepImpl<SR>> weight(float weight) {
		WeightStepImpl step = new WeightStepImpl( weight );
		functions.add( step );
		return step;
	}

	@Override
	public FunctionScoreScoreModeStep<FunctionScoreClausesStepImpl<SR>> scoreMode() {
		return scoreModeStep;
	}

	@Override
	public FunctionScoreBoostModeStep<FunctionScoreClausesStepImpl<SR>> boostMode() {
		return boostModeStep;
	}

	// -- PredicateScoreStep --

	@Override
	public FunctionScoreClausesStepImpl<SR> boost(float boost) {
		builder.boost( boost );
		return self();
	}

	@Override
	public FunctionScoreClausesStepImpl<SR> constantScore() {
		builder.constantScore();
		return self();
	}

	// -- Build --

	@Override
	protected SearchPredicate build() {
		for ( FunctionStep function : functions ) {
			function.contribute();
		}
		return builder.build();
	}

	// -- Internal --

	private FunctionScoreClausesStepImpl<SR> self() {
		return this;
	}

	// -- Sub-step implementations --

	private class ScoreModeStepImpl implements FunctionScoreScoreModeStep<FunctionScoreClausesStepImpl<SR>> {
		@Override
		public FunctionScoreClausesStepImpl<SR> multiply() {
			builder.scoreModeMultiply();
			return self();
		}

		@Override
		public FunctionScoreClausesStepImpl<SR> sum() {
			builder.scoreModeSum();
			return self();
		}

		@Override
		public FunctionScoreClausesStepImpl<SR> avg() {
			builder.scoreModeAvg();
			return self();
		}

		@Override
		public FunctionScoreClausesStepImpl<SR> first() {
			builder.scoreModeFirst();
			return self();
		}

		@Override
		public FunctionScoreClausesStepImpl<SR> max() {
			builder.scoreModeMax();
			return self();
		}

		@Override
		public FunctionScoreClausesStepImpl<SR> min() {
			builder.scoreModeMin();
			return self();
		}
	}

	private class BoostModeStepImpl implements FunctionScoreBoostModeStep<FunctionScoreClausesStepImpl<SR>> {
		@Override
		public FunctionScoreClausesStepImpl<SR> multiply() {
			builder.boostModeMultiply();
			return self();
		}

		@Override
		public FunctionScoreClausesStepImpl<SR> replace() {
			builder.boostModeReplace();
			return self();
		}

		@Override
		public FunctionScoreClausesStepImpl<SR> sum() {
			builder.boostModeSum();
			return self();
		}

		@Override
		public FunctionScoreClausesStepImpl<SR> avg() {
			builder.boostModeAvg();
			return self();
		}

		@Override
		public FunctionScoreClausesStepImpl<SR> max() {
			builder.boostModeMax();
			return self();
		}

		@Override
		public FunctionScoreClausesStepImpl<SR> min() {
			builder.boostModeMin();
			return self();
		}
	}

	private abstract class FunctionStep implements FunctionScoreClausesStep<SR, FunctionScoreClausesStepImpl<SR>> {
		protected float weight;
		protected SearchPredicate filter;

		FunctionStep(float weight) {
			this.weight = weight;
		}

		abstract void contribute();

		@Override
		public FunctionScoreFieldValueFactorStep<SR, FunctionScoreClausesStepImpl<SR>> fieldValueFactor(String fieldPath) {
			return FunctionScoreClausesStepImpl.this.fieldValueFactor( fieldPath );
		}

		@Override
		public <T> FunctionScoreFieldValueFactorStep<SR, FunctionScoreClausesStepImpl<SR>> fieldValueFactor(
				FieldValueFactorFieldReference<? super SR, T> fieldReference) {
			return FunctionScoreClausesStepImpl.this.fieldValueFactor( fieldReference );
		}

		@Override
		public FunctionScoreWeightStep<SR, FunctionScoreClausesStepImpl<SR>> weight(float weight) {
			return FunctionScoreClausesStepImpl.this.weight( weight );
		}

		@Override
		public FunctionScoreScoreModeStep<FunctionScoreClausesStepImpl<SR>> scoreMode() {
			return FunctionScoreClausesStepImpl.this.scoreMode();
		}

		@Override
		public FunctionScoreBoostModeStep<FunctionScoreClausesStepImpl<SR>> boostMode() {
			return FunctionScoreClausesStepImpl.this.boostMode();
		}

		@Override
		public FunctionScoreClausesStepImpl<SR> boost(float boost) {
			return FunctionScoreClausesStepImpl.this.boost( boost );
		}

		@Override
		public FunctionScoreClausesStepImpl<SR> constantScore() {
			return FunctionScoreClausesStepImpl.this.constantScore();
		}

		@Override
		public SearchPredicate toPredicate() {
			return FunctionScoreClausesStepImpl.this.toPredicate();
		}
	}

	private class WeightStepImpl extends FunctionStep
			implements FunctionScoreWeightStep<SR, FunctionScoreClausesStepImpl<SR>> {
		WeightStepImpl(float weight) {
			super( weight );
		}

		@Override
		void contribute() {
			builder.addWeightFunction( weight, filter );
		}

		@Override
		public FunctionScoreClausesStepImpl<SR> filter(SearchPredicate filter) {
			this.filter = filter;
			return self();
		}

		@Override
		public FunctionScoreClausesStepImpl<SR> filter(
				Function<? super TypedSearchPredicateFactory<SR>, ? extends PredicateFinalStep> filterContributor) {
			return filter( filterContributor.apply( factory ).toPredicate() );
		}
	}

	private class FieldValueFactorStepImpl extends FunctionStep
			implements FunctionScoreFieldValueFactorStep<SR, FunctionScoreClausesStepImpl<SR>> {
		private final FieldValueFactorFunctionBuilder fieldValueFactor;
		private final ValueModel valueModel;
		private final ModifierStepImpl modifierStep = new ModifierStepImpl();

		FieldValueFactorStepImpl(FieldValueFactorFunctionBuilder fieldValueFactor, ValueModel valueModel) {
			super( 1.0f );
			this.fieldValueFactor = fieldValueFactor;
			this.valueModel = valueModel;
		}

		@Override
		void contribute() {
			builder.addFieldValueFactorFunction( fieldValueFactor, weight, filter );
		}

		@Override
		public FunctionScoreFieldValueFactorModifierStep<
				? extends FunctionScoreFieldValueFactorStep<SR, FunctionScoreClausesStepImpl<SR>>> modifier() {
			return modifierStep;
		}

		@Override
		public FieldValueFactorStepImpl factor(float factor) {
			fieldValueFactor.factor( factor );
			return this;
		}

		@Override
		public FieldValueFactorStepImpl missing(Object value) {
			return missing( value, valueModel );
		}

		@Override
		public FieldValueFactorStepImpl missing(Object value, ValueModel valueModel) {
			fieldValueFactor.missing( value, valueModel );
			return this;
		}

		@Override
		public FieldValueFactorStepImpl functionWeight(float weight) {
			this.weight = weight;
			return this;
		}

		@Override
		public FieldValueFactorStepImpl filter(SearchPredicate filter) {
			this.filter = filter;
			return this;
		}

		@Override
		public FieldValueFactorStepImpl filter(
				Function<? super TypedSearchPredicateFactory<SR>, ? extends PredicateFinalStep> filterContributor) {
			return filter( filterContributor.apply( factory ).toPredicate() );
		}

		private class ModifierStepImpl
				implements
				FunctionScoreFieldValueFactorModifierStep<
						FunctionScoreFieldValueFactorStep<SR, FunctionScoreClausesStepImpl<SR>>> {
			@Override
			public FunctionScoreFieldValueFactorStep<SR, FunctionScoreClausesStepImpl<SR>> none() {
				fieldValueFactor.modifierNone();
				return FieldValueFactorStepImpl.this;
			}

			@Override
			public FunctionScoreFieldValueFactorStep<SR, FunctionScoreClausesStepImpl<SR>> log() {
				fieldValueFactor.modifierLog();
				return FieldValueFactorStepImpl.this;
			}

			@Override
			public FunctionScoreFieldValueFactorStep<SR, FunctionScoreClausesStepImpl<SR>> log1p() {
				fieldValueFactor.modifierLog1p();
				return FieldValueFactorStepImpl.this;
			}

			@Override
			public FunctionScoreFieldValueFactorStep<SR, FunctionScoreClausesStepImpl<SR>> log2p() {
				fieldValueFactor.modifierLog2p();
				return FieldValueFactorStepImpl.this;
			}

			@Override
			public FunctionScoreFieldValueFactorStep<SR, FunctionScoreClausesStepImpl<SR>> ln() {
				fieldValueFactor.modifierLn();
				return FieldValueFactorStepImpl.this;
			}

			@Override
			public FunctionScoreFieldValueFactorStep<SR, FunctionScoreClausesStepImpl<SR>> ln1p() {
				fieldValueFactor.modifierLn1p();
				return FieldValueFactorStepImpl.this;
			}

			@Override
			public FunctionScoreFieldValueFactorStep<SR, FunctionScoreClausesStepImpl<SR>> ln2p() {
				fieldValueFactor.modifierLn2p();
				return FieldValueFactorStepImpl.this;
			}

			@Override
			public FunctionScoreFieldValueFactorStep<SR, FunctionScoreClausesStepImpl<SR>> square() {
				fieldValueFactor.modifierSquare();
				return FieldValueFactorStepImpl.this;
			}

			@Override
			public FunctionScoreFieldValueFactorStep<SR, FunctionScoreClausesStepImpl<SR>> sqrt() {
				fieldValueFactor.modifierSqrt();
				return FieldValueFactorStepImpl.this;
			}

			@Override
			public FunctionScoreFieldValueFactorStep<SR, FunctionScoreClausesStepImpl<SR>> reciprocal() {
				fieldValueFactor.modifierReciprocal();
				return FieldValueFactorStepImpl.this;
			}
		}

	}

}
