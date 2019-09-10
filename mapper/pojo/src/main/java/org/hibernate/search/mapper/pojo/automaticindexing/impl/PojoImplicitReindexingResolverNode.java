/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.automaticindexing.impl;

import org.hibernate.search.mapper.pojo.model.spi.PojoRuntimeIntrospector;
import org.hibernate.search.util.common.impl.ToStringTreeAppendable;
import org.hibernate.search.util.common.impl.ToStringTreeBuilder;

/**
 * An node within a {@link PojoImplicitReindexingResolver}.
 *
 * @param <T> The type of "dirty" objects for which this resolver is able to
 * {@link #resolveEntitiesToReindex(PojoReindexingCollector, PojoRuntimeIntrospector, Object, Object)
 * resolve entities to reindex}.
 * This type may be an entity type, an embeddable type, a collection type, ...
 * @param <S> The expected type of the object describing the "dirtiness state".
 */
public abstract class PojoImplicitReindexingResolverNode<T, S> implements AutoCloseable, ToStringTreeAppendable {

	@Override
	public String toString() {
		return new ToStringTreeBuilder().value( this ).toString();
	}

	@Override
	public abstract void close();

	/**
	 * Add all entities that should be reindexed to {@code collector},
	 * taking into account the given "dirtiness state".
	 *
	 * @param collector A collector for entities that should be reindexed.
	 * @param dirty A value that is dirty to some extent.
	 * @param dirtinessState The set of dirty paths in the object passed to the root reindexing resolver
	 * (resolvers may delegate to other resolvers, but they will always pass the same dirtiness state to delegates).
	 * {@code null} can be passed to mean "no information", in which case all paths are considered dirty.
	 */
	public abstract void resolveEntitiesToReindex(PojoReindexingCollector collector,
			PojoRuntimeIntrospector runtimeIntrospector, T dirty, S dirtinessState);

	public static <T, D> PojoImplicitReindexingResolverNode<T, D> noOp() {
		return NoOpPojoImplicitReindexingResolverNode.get();
	}

}
