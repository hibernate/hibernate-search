/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.automaticindexing.impl;

import org.hibernate.search.util.common.spi.ToStringTreeAppendable;

/**
 * An node within a {@link PojoImplicitReindexingResolver}.
 *
 * @param <T> The type of "dirty" objects for which this resolver is able to
 * {@link #resolveEntitiesToReindex(PojoReindexingCollector, Object, PojoImplicitReindexingResolverRootContext)
 * resolve entities to reindex}.
 * This type may be an entity type, an embeddable type, a collection type, ...
 */
public abstract class PojoImplicitReindexingResolverNode<T> implements AutoCloseable, ToStringTreeAppendable {

	@Override
	public String toString() {
		return toStringTree();
	}

	@Override
	public abstract void close();

	/**
	 * Add all entities that should be reindexed to {@code collector},
	 * taking into account the given "dirtiness state".
	 *  @param collector A collector for entities that should be reindexed.
	 * @param dirty A value that is dirty to some extent.
	 * @param context A context representing the root entity, and including in particular information about dirty paths.
	 * Resolvers should always pass this context as-is when delegating to other resolvers.
	 */
	public abstract void resolveEntitiesToReindex(PojoReindexingCollector collector,
			T dirty, PojoImplicitReindexingResolverRootContext context);

	public static <T> PojoImplicitReindexingResolverNode<T> noOp() {
		return NoOpPojoImplicitReindexingResolverNode.get();
	}

}
