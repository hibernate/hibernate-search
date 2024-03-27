/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.loading.spi;

/**
 * An execution context for queries,
 * providing components allowing to load data from an external source (relational database, ...).
 *
 * @param <E> The type of loaded entities.
 */
public interface SearchLoadingContext<E> {

	Object unwrap();

	ProjectionHitMapper<E> createProjectionHitMapper();

}
