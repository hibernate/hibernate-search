package org.hibernate.search.engine.search.predicate.spi;

import org.hibernate.search.engine.search.predicate.SearchPredicate;

public interface FunctionScorePredicateBuilder extends SearchPredicateBuilder {

	void innerPredicate(SearchPredicate predicate);

	void addFieldValueFactorFunction(FieldValueFactorFunctionBuilder functionBuilder,
			float weight, SearchPredicate filter);

	void addWeightFunction(float weight, SearchPredicate filter);

	void scoreModeMultiply();

	void scoreModeSum();

	void scoreModeAvg();

	void scoreModeFirst();

	void scoreModeMax();

	void scoreModeMin();

	void boostModeMultiply();

	void boostModeReplace();

	void boostModeSum();

	void boostModeAvg();

	void boostModeMax();

	void boostModeMin();

}
