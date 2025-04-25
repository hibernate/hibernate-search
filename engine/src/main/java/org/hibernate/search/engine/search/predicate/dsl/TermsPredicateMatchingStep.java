/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.predicate.dsl;

import java.util.Collection;

import org.hibernate.search.engine.search.common.ValueModel;
import org.hibernate.search.util.common.impl.CollectionHelper;

/**
 * The step in a "terms" predicate definition where the terms to match can be set.
 *
 * @param <N> The type of the next step.
 */
public interface TermsPredicateMatchingStep<N extends TermsPredicateOptionsStep<?>> {

	/**
	 * Require at least one of the targeted fields to match <b>any</b> of the provided terms.
	 *
	 * @param firstTerm The (first) term to match.
	 * @param otherTerms The others (optional) terms to match.
	 * The signature of this method defines these parameter as a {@link Object},
	 * but a specific type is expected depending on the targeted field.
	 * See {@link ValueModel#MAPPING} for more information.
	 * @return The next step.
	 */
	default N matchingAny(Object firstTerm, Object... otherTerms) {
		return matchingAny( CollectionHelper.asList( firstTerm, otherTerms ) );
	}

	/**
	 * Require at least one of the targeted fields to match <b>any</b> of the provided terms.
	 *
	 * @param terms The terms to match.
	 * The signature of this method defines this parameter as a {@link Collection} of any {@link Object},
	 * but a specific type is expected depending on the targeted field.
	 * See {@link ValueModel#MAPPING} for more information.
	 * @return The next step.
	 */
	default N matchingAny(Collection<?> terms) {
		return matchingAny( terms, ValueModel.MAPPING );
	}

	/**
	 * Require at least one of the targeted fields to match <b>any</b> of the provided terms.
	 *
	 * @param terms The terms to match.
	 * The signature of this method defines this parameter as a {@link Collection} of any {@link Object},
	 * but a specific type is expected depending on the targeted field and on the {@code convert} parameter.
	 * See {@link org.hibernate.search.engine.search.common.ValueConvert} for more information.
	 * @param convert Controls how the {@code value} should be converted before Hibernate Search attempts to interpret it as a field value.
	 * See {@link org.hibernate.search.engine.search.common.ValueConvert} for more information.
	 * @return The next step.
	 * @deprecated Use {@link #matchingAny(Collection, ValueModel)} instead.
	 */
	@Deprecated(since = "7.2")
	default N matchingAny(Collection<?> terms, org.hibernate.search.engine.search.common.ValueConvert convert) {
		return matchingAny( terms, org.hibernate.search.engine.search.common.ValueConvert.toValueModel( convert ) );
	}

	/**
	 * Require at least one of the targeted fields to match <b>any</b> of the provided terms.
	 *
	 * @param terms The terms to match.
	 * The signature of this method defines this parameter as a {@link Collection} of any {@link Object},
	 * but a specific type is expected depending on the targeted field and on the {@code valueModel} parameter.
	 * See {@link ValueModel} for more information.
	 * @param valueModel The model value, determines how the {@code value} should be converted before Hibernate Search attempts to interpret it as a field value.
	 * See {@link ValueModel} for more information.
	 * @return The next step.
	 */
	N matchingAny(Collection<?> terms, ValueModel valueModel);

	/**
	 * Require at least one of the targeted fields to match <b>all</b> of the provided terms.
	 *
	 * @param firstTerm The (first) term to match.
	 * @param otherTerms The others (optional) terms to match.
	 * The signature of this method defines these parameter as a {@link Object},
	 * but a specific type is expected depending on the targeted field.
	 * See {@link ValueModel#MAPPING} for more information.
	 * @return The next step.
	 */
	default N matchingAll(Object firstTerm, Object... otherTerms) {
		return matchingAll( CollectionHelper.asList( firstTerm, otherTerms ) );
	}

	/**
	 * Require at least one of the targeted fields to match <b>all</b> of the provided terms.
	 *
	 * @param terms The terms to match.
	 * The signature of this method defines this parameter as a {@link Collection} of any {@link Object},
	 * but a specific type is expected depending on the targeted field.
	 * See {@link ValueModel#MAPPING} for more information.
	 * @return The next step.
	 */
	default N matchingAll(Collection<?> terms) {
		return matchingAll( terms, ValueModel.MAPPING );
	}

	/**
	 * Require at least one of the targeted fields to match <b>all</b> of the provided terms.
	 *
	 * @param terms The terms to match.
	 * The signature of this method defines this parameter as a {@link Collection} of any {@link Object},
	 * but a specific type is expected depending on the targeted field and on the {@code convert} parameter.
	 * See {@link org.hibernate.search.engine.search.common.ValueConvert} for more information.
	 * @param convert Controls how the {@code value} should be converted before Hibernate Search attempts to interpret it as a field value.
	 * See {@link org.hibernate.search.engine.search.common.ValueConvert} for more information.
	 * @return The next step.
	 * @deprecated Use {@link #matchingAll(Collection, ValueModel)} instead.
	 */
	@Deprecated(since = "7.2")
	default N matchingAll(Collection<?> terms, org.hibernate.search.engine.search.common.ValueConvert convert) {
		return matchingAll( terms, org.hibernate.search.engine.search.common.ValueConvert.toValueModel( convert ) );
	}

	/**
	 * Require at least one of the targeted fields to match <b>all</b> of the provided terms.
	 *
	 * @param terms The terms to match.
	 * The signature of this method defines this parameter as a {@link Collection} of any {@link Object},
	 * but a specific type is expected depending on the targeted field and on the {@code valueModel} parameter.
	 * See {@link ValueModel} for more information.
	 * @param valueModel The model of term values, determines how the {@code terms} should be converted before Hibernate Search attempts to interpret them as field values.
	 * See {@link ValueModel} for more information.
	 * @return The next step.
	 */
	N matchingAll(Collection<?> terms, ValueModel valueModel);

}
