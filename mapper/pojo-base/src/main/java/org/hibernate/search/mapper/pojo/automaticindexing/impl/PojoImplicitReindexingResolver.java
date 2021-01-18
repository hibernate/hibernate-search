/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.automaticindexing.impl;

import java.util.Set;

import org.hibernate.search.util.common.impl.ToStringTreeAppendable;

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
	 * @param dirtinessState A set of dirty paths.
	 * {@code null} can be passed to mean "no information", in which case all paths are considered dirty.
	 * @return {@code true} if the given dirty paths would require to reindex an entity
	 * of the type handled by this resolver, {@code false} if no reindexing is required.
	 */
	boolean requiresSelfReindexing(Set<String> dirtinessState);

	/**
	 * Add all entities that should be reindexed to {@code collector},
	 * taking into account the given "dirtiness state".
	 *  @param collector A collector for dirty entities that should be reindexed.
	 * @param dirty The entity whose dirtiness is to be checked.
	 * @param context The set of dirty paths in the given entity.
 * {@code null} can be passed to mean "no information", in which case all paths are considered dirty.
	 */
	void resolveEntitiesToReindex(PojoReindexingCollector collector,
			T dirty, PojoImplicitReindexingResolverRootContext context);

}
