/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.predicate.dsl;

import java.util.Collection;

import org.hibernate.search.engine.search.common.ValueModel;

/**
 * The step in a "match id" predicate definition where the IDs to match can be set.
 *
 * @param <N> The type of the next step.
 */
public interface MatchIdPredicateMatchingStep<N extends MatchIdPredicateMatchingMoreStep<?, ?>> {

	/**
	 * Target the identifier with the given id.
	 * <p>
	 * If used multiple times, it will target any of the specified values.
	 * @see #matchingAny(Collection)
	 * @param value the value of the id we want to match.
	 * @return The next step.
	 */
	default N matching(Object value) {
		return matching( value, ValueModel.MAPPING );
	}

	/**
	 * Target the identifier with the given id.
	 * <p>
	 * If used multiple times, it will target any of the specified values.
	 * @see #matchingAny(Collection)
	 * @param value the value of the id we want to match.
	 * @param convert Controls how the {@code value} should be converted
	 * before Hibernate Search attempts to interpret it as an identifier value.
	 * See {@link org.hibernate.search.engine.search.common.ValueConvert} for more information.
	 * @return The next step.
	 * @deprecated Use {@link #matching(Object, ValueModel)} instead.
	 */
	@Deprecated(since = "7.2")
	default N matching(Object value, org.hibernate.search.engine.search.common.ValueConvert convert) {
		return matching( value, org.hibernate.search.engine.search.common.ValueConvert.toValueModel( convert ) );
	}

	/**
	 * Target the identifier with the given id.
	 * <p>
	 * If used multiple times, it will target any of the specified values.
	 * @see #matchingAny(Collection)
	 * @param value the value of the id we want to match.
	 * @param valueModel The model value, determines how the {@code value} should be converted
	 * before Hibernate Search attempts to interpret it as an identifier value.
	 * See {@link ValueModel} for more information.
	 * @return The next step.
	 */
	N matching(Object value, ValueModel valueModel);

	/**
	 * Target the identifiers matching any of the values in a collection.
	 * @param values the collection of identifiers to match.
	 * @return The next step.
	 */
	default N matchingAny(Collection<?> values) {
		return matchingAny( values, ValueModel.MAPPING );
	}

	/**
	 * Target the identifiers matching any of the values in a collection.
	 * @param values the collection of identifiers to match.
	 * @param convert Controls how the {@code value} should be converted
	 * before Hibernate Search attempts to interpret it as an identifier value.
	 * See {@link org.hibernate.search.engine.search.common.ValueConvert} for more information.
	 * @return The next step.
	 * @deprecated Use {@link #matchingAny(Collection, ValueModel)} instead.
	 */
	@Deprecated(since = "7.2")
	default N matchingAny(Collection<?> values, org.hibernate.search.engine.search.common.ValueConvert convert) {
		return matching( values, org.hibernate.search.engine.search.common.ValueConvert.toValueModel( convert ) );
	}

	/**
	 * Target the identifiers matching any of the values in a collection.
	 * @param values the collection of identifiers to match.
	 * @param valueModel The model value, determines how the {@code value} should be converted
	 * before Hibernate Search attempts to interpret it as an identifier value.
	 * See {@link ValueModel} for more information.
	 * @return The next step.
	 */
	default N matchingAny(Collection<?> values, ValueModel valueModel) {
		N next = null;
		for ( Object value : values ) {
			next = matching( value, valueModel );
		}
		return next;
	}
}
