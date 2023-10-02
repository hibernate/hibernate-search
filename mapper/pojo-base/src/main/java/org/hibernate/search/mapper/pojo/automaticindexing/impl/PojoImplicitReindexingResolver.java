/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.automaticindexing.impl;

import org.hibernate.search.mapper.pojo.model.path.spi.PojoPathFilter;
import org.hibernate.search.util.common.spi.ToStringTreeAppendable;

/**
 * An object responsible for resolving the set of entities that should be reindexed when a given entity changes.
 * <p>
 * This includes telling whether changes require the changed entity to be reindexed,
 * but also retrieving all entities that use the changed entity in their indexed form
 * so that they can be reindexed by Hibernate Search.
 *
 * @param <T> The type of entities this object is able to handle.
 */
public interface PojoImplicitReindexingResolver<T> extends AutoCloseable, ToStringTreeAppendable {

	@Override
	void close();

	/**
	 * @return A path filter that only accepts paths whose dirtiness would require reindexing the dirty entity.
	 */
	PojoPathFilter dirtySelfFilter();

	/**
	 * @return A path filter that only accepts paths whose dirtiness would require reindexing the dirty entity
	 * OR an associated entity that contains it.
	 */
	PojoPathFilter dirtySelfOrContainingFilter();

	/**
	 * Adds all entities that should be reindexed to {@code collector},
	 * taking into account the given "dirty entity" and the context describing its "dirtiness".
	 * @param collector A collector for dirty entities that should be reindexed.
	 * @param dirty The entity whose dirtiness is to be checked.
	 * @param context A context related to the entity root
	 */
	void resolveEntitiesToReindex(PojoReindexingCollector collector,
			T dirty, PojoImplicitReindexingResolverRootContext context);

	/**
	 * @return A path filter that only accepts direct paths to associations to containing entities.
	 */
	PojoImplicitReindexingAssociationInverseSideResolver associationInverseSideResolver();

}
