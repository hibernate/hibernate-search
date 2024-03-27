/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.query;

/**
 * A base interface for subtypes of {@link SearchFetchable} allowing to
 * easily override the result type for all relevant methods.
 *
 * @param <H> The type of query hits.
 * @param <R> The result type (extending {@link SearchResult}).
 * @param <SC> The scroll type (extending {@link SearchScroll}).
 */
public interface ExtendedSearchFetchable<H, R extends SearchResult<H>, SC extends SearchScroll<H>>
		extends SearchFetchable<H> {

	@Override
	R fetch(Integer limit);

	@Override
	R fetch(Integer offset, Integer limit);

	@Override
	R fetchAll();

	@Override
	SC scroll(int chunkSize);

}
