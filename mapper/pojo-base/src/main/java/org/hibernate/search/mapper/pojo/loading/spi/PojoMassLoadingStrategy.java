/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.loading.spi;

import java.util.Set;

/**
 * A strategy for entity loading during mass indexing.
 *
 * @param <E> The type of loaded entities.
 * @param <I> The type of entity identifiers.
 */
public interface PojoMassLoadingStrategy<E, I> {

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
	 * @param type A type that Hibernate Search would like to load together with another one that uses the same strategy.
	 * @param context Mapper-specific loading context.
	 * @return {@code true} if this type
	 */
	boolean groupingAllowed(PojoLoadingTypeContext<? extends E> type, PojoMassLoadingContext context);

	/**
	 * @param expectedTypes The expected types of loaded entities.
	 * The types are guaranteed to be {@link PojoLoadingTypeContext#massLoadingStrategy() assigned this strategy}.
	 * @param context A context, used to retrieve information about the loading environment and options.
	 * @return An entity identifier loader.
	 */
	PojoMassIdentifierLoader createIdentifierLoader(Set<? extends PojoLoadingTypeContext<? extends E>> expectedTypes,
			PojoMassIdentifierLoadingContext<I> context);

	/**
	 * @param expectedTypes The expected types of loaded entities.
	 * The types are guaranteed to be {@link PojoLoadingTypeContext#massLoadingStrategy() assigned this strategy}.
	 * @param context A context, used to retrieve information about the loading environment and options.
	 * @return An entity loader.
	 */
	PojoMassEntityLoader<I> createEntityLoader(Set<? extends PojoLoadingTypeContext<? extends E>> expectedTypes,
			PojoMassEntityLoadingContext<E> context);

}
