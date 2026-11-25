package org.hibernate.search.engine.search.predicate.spi;

import java.util.List;

import org.hibernate.search.engine.search.common.ValueModel;

// TODO: when adding the next function type (e.g. DecayFunctionBuilder), extract a common
//  ScoreFunctionBuilder interface with nestedPathHierarchy() so the predicate builder's
//  nesting validation works uniformly across all function types.
public interface FieldValueFactorFunctionBuilder {

	List<String> nestedPathHierarchy();

	void modifierNone();

	void modifierLog();

	void modifierLog1p();

	void modifierLog2p();

	void modifierLn();

	void modifierLn1p();

	void modifierLn2p();

	void modifierSquare();

	void modifierSqrt();

	void modifierReciprocal();

	void factor(float factor);

	void missing(Object value, ValueModel valueModel);

}
