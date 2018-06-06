/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.dirtiness.impl;

import org.hibernate.search.mapper.pojo.model.spi.PojoRuntimeIntrospector;
import org.hibernate.search.util.impl.common.ToStringTreeAppendable;
import org.hibernate.search.util.impl.common.ToStringTreeBuilder;

/**
 * An object responsible for resolving the set of entities that should be reindexed when a given entity changes.
 *
 * @param <T> The type of "dirty" objects for which this resolver is able to
 * {@link #resolveEntitiesToReindex(PojoReindexingCollector, PojoRuntimeIntrospector, Object, PojoDirtinessState)
 * resolve entities to reindex}.
 * This type may be an entity type, an embeddable type, a collection type, ...
 */
public abstract class PojoImplicitReindexingResolver<T> implements ToStringTreeAppendable {

	@Override
	public String toString() {
		return new ToStringTreeBuilder().value( this ).toString();
	}

	/**
	 * Add all entities that should be reindexed to {@code collector},
	 * taking into account the given "dirtiness state".
	 *
	 * @param collector A collector for entities that should be reindexed.
	 * @param dirty A value that is dirty to some extent.
	 * @param dirtinessState The dirtiness state of the object passed to the root reindexing resolver
	 * (resolvers may delegate to other resolvers, but they will always pass the same dirtiness state to delegates).
	 */
	public abstract void resolveEntitiesToReindex(PojoReindexingCollector collector,
			PojoRuntimeIntrospector runtimeIntrospector, T dirty, PojoDirtinessState dirtinessState);

	public static <T> PojoImplicitReindexingResolver<T> noOp() {
		return NoOpPojoImplicitReindexingResolver.get();
	}

}
