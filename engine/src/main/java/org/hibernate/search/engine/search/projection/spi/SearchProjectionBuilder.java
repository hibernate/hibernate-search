/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.projection.spi;

import org.hibernate.search.engine.search.projection.SearchProjection;

/**
 * A search projection builder, i.e. an object responsible for collecting parameters
 * and then building a search projection.
 */
public interface SearchProjectionBuilder<T> {

	SearchProjection<T> build();
}
