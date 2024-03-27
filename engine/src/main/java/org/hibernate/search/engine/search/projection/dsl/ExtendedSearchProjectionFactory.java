/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.projection.dsl;

/**
 * A base interface for subtypes of {@link SearchProjectionFactory} allowing to
 * easily override the self type for all relevant methods.
 * <p>
 * <strong>Warning:</strong> Generic parameters of this type are subject to change,
 * so this type should not be referenced directly in user code.
 *
 * @param <S> The self type, i.e. the exposed type of this factory.
 */
public interface ExtendedSearchProjectionFactory<S extends ExtendedSearchProjectionFactory<?, R, E>, R, E>
		extends SearchProjectionFactory<R, E> {

	@Override
	S withRoot(String objectFieldPath);

}
