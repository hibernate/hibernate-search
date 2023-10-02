/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.loading.impl;

import java.util.Collection;
import java.util.Optional;

import org.hibernate.search.engine.common.timing.Deadline;
import org.hibernate.search.mapper.pojo.loading.spi.PojoLoadingTypeContext;
import org.hibernate.search.mapper.pojo.loading.spi.PojoSelectionLoadingContext;
import org.hibernate.search.mapper.pojo.loading.spi.PojoSelectionLoadingStrategy;

/**
 * A mutable plan to load POJO entities from an external source (database, ...).
 * @param <T> The exposed type of loaded entities.
 */
public interface PojoLoadingPlan<T> {

	@SuppressWarnings("unchecked")
	static <T> PojoLoadingPlan<T> create(PojoSelectionLoadingContext context,
			Collection<? extends PojoLoadingTypeContext<? extends T>> targetTypes) {
		PojoSelectionLoadingStrategy<?> strategy = null;
		for ( PojoLoadingTypeContext<? extends T> typeContext : targetTypes ) {
			Optional<? extends PojoSelectionLoadingStrategy<?>> thisTypeStrategyOptional =
					typeContext.selectionLoadingStrategyOptional();
			if ( !thisTypeStrategyOptional.isPresent() ) {
				// One of the types cannot be loaded -- something is wrong.
				// Forget about the optimization, and fail later,
				// if the backend actually attempts loading this type that cannot be loaded.
				return new PojoMultiLoaderLoadingPlan<>( context );
			}
			PojoSelectionLoadingStrategy<?> thisTypeStrategy = thisTypeStrategyOptional.get();
			if ( strategy == null ) {
				strategy = thisTypeStrategy;
			}
			else if ( !strategy.equals( thisTypeStrategy ) ) {
				return new PojoMultiLoaderLoadingPlan<>( context );
			}
		}
		// We determined that all target types use the same strategy.
		// We can safely use a single-loader loading plan.
		return new PojoSingleLoaderLoadingPlan<>( context, (PojoSelectionLoadingStrategy<T>) strategy );
	}

	/**
	 * Plans the loading of an entity instance.
	 * @param <T2> The exact expected type for the entity instance.
	 * @param expectedType The exact expected type for the entity instance.
	 * @param identifier The entity identifier.
	 * @return An ordinal to pass later to {@link #retrieve(PojoLoadingTypeContext, int)}.
	 * @see #loadBlocking(Deadline)
	 */
	<T2 extends T> int planLoading(PojoLoadingTypeContext<T2> expectedType, Object identifier);

	/**
	 * Loads the entities whose identifiers were passed to {@link #planLoading(PojoLoadingTypeContext, Object)},
	 * blocking the current thread while doing so.
	 * @param deadline The deadline for loading the entities, or null if there is no deadline.
	 */
	void loadBlocking(Deadline deadline);

	/**
	 * Retrieves a loaded entity instance.
	 * @param <T2> The exact expected type for the entity instance.
	 * @param expectedType The expected type for the entity instance.
	 * Must be the same type passed to {@link #planLoading(PojoLoadingTypeContext, Object)}.
	 * @param ordinal The ordinal returned by {@link #planLoading(PojoLoadingTypeContext, Object)}.
	 * @return The loaded entity instance, or {@code null} if it was not found.
	 * The instance is guaranteed to be an instance of the given type <strong>exactly</strong> (not a subtype).
	 */
	<T2 extends T> T2 retrieve(PojoLoadingTypeContext<T2> expectedType, int ordinal);

	void clear();
}
