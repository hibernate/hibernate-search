/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.predicate.dsl;

import org.hibernate.search.engine.search.common.PredicateValueConvert;

/**
 * The step in a "match" predicate definition where the value to match can be set.
 *
 * @param <N> The type of the next step.
 */
public interface MatchPredicateMatchingStep<N extends MatchPredicateOptionsStep<?>> {

	/**
	 * Require at least one of the targeted fields to match the given value.
	 * <p>
	 * This method will apply DSL converters to {@code value} before Hibernate Search attempts to interpret it as a field value.
	 * See {@link PredicateValueConvert#MAPPING}.
	 *
	 * @param value The value to match.
	 * The signature of this method defines this parameter as an {@link Object},
	 * but a specific type is expected depending on the targeted field.
	 * See {@link PredicateValueConvert#MAPPING} for more information.
	 * @return The next step.
	 *
	 * @see #matching(Object, PredicateValueConvert)
	 */
	default N matching(Object value) {
		return matching( value, PredicateValueConvert.MAPPING );
	}

	/**
	 * Require at least one of the targeted fields to match the given value.
	 *
	 * @param value The value to match.
	 * The signature of this method defines this parameter as an {@link Object},
	 * but a specific type is expected depending on the targeted field and on the {@code dslConverter} parameter.
	 * See {@link org.hibernate.search.engine.search.common.ValueConvert} for more information.
	 * @param convert Controls how the {@code value} should be converted before Hibernate Search attempts to interpret it as a field value.
	 * See {@link org.hibernate.search.engine.search.common.ValueConvert} for more information.
	 * @return The next step.
	 * @deprecated Use {@link #matching(Object, PredicateValueConvert)} instead.
	 *
	 * @see org.hibernate.search.engine.search.common.ValueConvert
	 */
	@Deprecated
	default N matching(Object value, org.hibernate.search.engine.search.common.ValueConvert convert) {
		return matching( value, org.hibernate.search.engine.search.common.ValueConvert.toPredicateValueConvert( convert ) );
	}

	/**
	 * Require at least one of the targeted fields to match the given value.
	 *
	 * @param value The value to match.
	 * The signature of this method defines this parameter as an {@link Object},
	 * but a specific type is expected depending on the targeted field and on the {@code dslConverter} parameter.
	 * See {@link PredicateValueConvert} for more information.
	 * @param convert Controls how the {@code value} should be converted before Hibernate Search attempts to interpret it as a field value.
	 * See {@link PredicateValueConvert} for more information.
	 * @return The next step.
	 *
	 * @see PredicateValueConvert
	 */
	N matching(Object value, PredicateValueConvert convert);

}
