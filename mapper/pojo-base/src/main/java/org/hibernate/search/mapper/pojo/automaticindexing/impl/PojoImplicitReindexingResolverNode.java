/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.automaticindexing.impl;

import org.hibernate.search.util.common.impl.ToStringTreeAppendable;
import org.hibernate.search.util.common.impl.ToStringTreeBuilder;

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
		return new ToStringTreeBuilder().value( this ).toString();
	}

	@Override
	public abstract void close();

	/**
	 * Add all entities that should be reindexed to {@code collector},
	 * taking into account the given "dirtiness state".
	 *  @param collector A collector for entities that should be reindexed.
	 * @param dirty A value that is dirty to some extent.
	 * @param context The set of dirty paths in the object passed to the root reindexing resolver
 * (resolvers may delegate to other resolvers, but they will always pass the same dirtiness state to delegates).
 * {@code null} can be passed to mean "no information", in which case all paths are considered dirty.
	 */
	public abstract void resolveEntitiesToReindex(PojoReindexingCollector collector,
			T dirty, PojoImplicitReindexingResolverRootContext context);

	public static <T> PojoImplicitReindexingResolverNode<T> noOp() {
		return NoOpPojoImplicitReindexingResolverNode.get();
	}

}
