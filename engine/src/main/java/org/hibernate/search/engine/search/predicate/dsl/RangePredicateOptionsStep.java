package org.hibernate.search.engine.search.predicate.dsl;

/**
 * The final step in a "range" predicate definition, where optional parameters can be set.
 *
 * @param <S> The "self" type (the actual exposed type of this step).
 */
public interface RangePredicateOptionsStep<S extends RangePredicateOptionsStep<?>>
		extends PredicateFinalStep, PredicateScoreStep<S> {

}
