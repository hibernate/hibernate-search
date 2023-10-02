/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.standalone.loading;

import java.util.Map;

import org.hibernate.search.mapper.pojo.standalone.loading.impl.MapSelectionLoadingStrategy;
import org.hibernate.search.util.common.annotation.Incubating;

/**
 * A strategy for selection loading, used in particular during search.
 *
 * @param <E> The type of loaded entities.
 */
@Incubating
public interface SelectionLoadingStrategy<E> {

	/**
	 * Creates a simple map-based loading strategy.
	 * <p>
	 * Generally only useful for tests.
	 *
	 * @param map A map from containing all entity identifiers as keys, and the corresponding entity as value.
	 * @return A loading strategy that loads entities from the given map.
	 * @param <E> The type of loaded entities.
	 * @param <I> The type of entity identifiers.
	 */
	static <E, I> SelectionLoadingStrategy<E> fromMap(Map<I, E> map) {
		return new MapSelectionLoadingStrategy<>( map );
	}

	/**
	 * @param obj Another strategy
	 * @return {@code true} if the other strategy targets the same entity hierarchy
	 * and can be used as a replacement for this one.
	 * {@code false} otherwise or when unsure.
	 */
	@Override
	boolean equals(Object obj);

	/*
	 * Hashcode must be overridden to be consistent with equals.
	 */
	@Override
	int hashCode();

	/**
	 * @param includedTypes A representation of all entity types that will have to be loaded.
	 * @param options Loading options configured by the requester (who created the session, requested the search, ...).
	 * @return An entity loader.
	 */
	SelectionEntityLoader<E> createEntityLoader(LoadingTypeGroup<E> includedTypes, SelectionLoadingOptions options);

}
