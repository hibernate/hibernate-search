/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.query;

/**
 * A base interface for subtypes of {@link SearchScroll} allowing to
 * easily override the result type for all relevant methods.
 *
 * @param <H> The type of query hits.
 * @param <R> The result type (extending {@link SearchScrollResult}).
 */
public interface ExtendedSearchScroll<H, R extends SearchScrollResult<H>>
		extends SearchScroll<H> {

	@Override
	R next();

}
