/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.sort.dsl;

import org.hibernate.search.engine.search.common.SortMode;

/**
 * The step in a sort definition where the {@link SortMode} can be set.
 *
 * @param <S> The "self" type (the actual exposed type of this step)
 * @author Emmanuel Bernard emmanuel@hibernate.org
 */
public interface SortModeStep<S> {

	/**
	 * Start describing the behavior of this sort when a document has multiple values for the targeted field.
	 *
	 * @param mode The mode.
	 * @return {@code this}, for method chaining.
	 */
	S mode(SortMode mode);

}
