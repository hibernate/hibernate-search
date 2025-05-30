/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.projection.dsl;

import org.hibernate.search.util.common.annotation.Incubating;

/**
 * A base interface for subtypes of {@link TypedSearchProjectionFactory} allowing to
 * easily override the self type for all relevant methods.
 * <p>
 * <strong>Warning:</strong> Generic parameters of this type are subject to change,
 * so this type should not be referenced directly in user code.
 *
 * @param <SR> Scope root type.
 * @param <S> The self type, i.e. the exposed type of this factory.
 */
public interface ExtendedSearchProjectionFactory<SR, S extends ExtendedSearchProjectionFactory<SR, ?, R, E>, R, E>
		extends TypedSearchProjectionFactory<SR, R, E> {

	@Override
	S withRoot(String objectFieldPath);

	/**
	 * @throws org.hibernate.search.util.common.SearchException In case the current factory cannot be rescoped for the {@code scopeRootType}.
	 */
	@Incubating
	<SR2> ExtendedSearchProjectionFactory<SR2, ?, R, E> withScopeRoot(Class<SR2> scopeRootType);

}
