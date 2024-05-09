/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.predicate.dsl;

import org.hibernate.search.engine.search.common.ValueModel;

/**
 * The step in a "match" predicate definition where the value to match can be set.
 *
 * @param <N> The type of the next step.
 */
public interface MatchPredicateMatchingStep<N extends MatchPredicateOptionsStep<?>>
		extends MatchPredicateMatchingGenericStep<N, Object> {

	/**
	 * Require at least one of the targeted fields to match the given value.
	 * <p>
	 * This method will apply DSL converters to {@code value} before Hibernate Search attempts to interpret it as a field value.
	 * See {@link ValueModel#MAPPING}.
	 *
	 * @param value The value to match.
	 * The signature of this method defines this parameter as an {@code T},
	 * but a specific type is expected depending on the targeted field.
	 * See {@link ValueModel#MAPPING} for more information.
	 * @return The next step.
	 *
	 * @see #matching(Object, ValueModel)
	 */
	default N matching(Object value) {
		return matching( value, ValueModel.MAPPING );
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
	 * @deprecated Use {@link #matching(Object, ValueModel)} instead.
	 *
	 * @see org.hibernate.search.engine.search.common.ValueConvert
	 */
	@Deprecated(since = "7.2")
	default N matching(Object value, org.hibernate.search.engine.search.common.ValueConvert convert) {
		return matching( value, org.hibernate.search.engine.search.common.ValueConvert.toValueModel( convert ) );
	}

	/**
	 * Require at least one of the targeted fields to match the given value.
	 *
	 * @param value The value to match.
	 * The signature of this method defines this parameter as an {@link Object},
	 * but a specific type is expected depending on the targeted field and on the {@code dslConverter} parameter.
	 * See {@link ValueModel} for more information.
	 * @param valueModel The model value, determines how the {@code value} should be converted before Hibernate Search attempts to interpret it as a field value.
	 * See {@link ValueModel} for more information.
	 * @return The next step.
	 *
	 * @see ValueModel
	 */
	N matching(Object value, ValueModel valueModel);

}
