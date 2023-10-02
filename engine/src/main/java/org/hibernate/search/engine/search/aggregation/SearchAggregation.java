/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.aggregation;

/**
 * A search aggregation, i.e. a way to turn search query hits into one or more summarizing metric(s).
 * <p>
 * Implementations of this interface are provided to users by Hibernate Search.
 * Users must not try to implement this interface.
 *
 * @param <A> The type of result for this aggregation.
 */
public interface SearchAggregation<A> {
}
