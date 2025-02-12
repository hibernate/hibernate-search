/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.predicate.dsl;

/**
 * A base interface for subtypes of {@link SearchPredicateFactory} allowing to
 * easily override the self type for all relevant methods.
 * <p>
 * <strong>Warning:</strong> Generic parameters of this type are subject to change,
 * so this type should not be referenced directly in user code.
 *
 * @param <SR> Scope root type.
 * @param <S> The self type, i.e. the exposed type of this factory.
 */
public interface ExtendedSearchPredicateFactory<SR, S extends ExtendedSearchPredicateFactory<SR, ?>>
		extends SearchPredicateFactory<SR> {

	@Override
	S withRoot(String objectFieldPath);

}
