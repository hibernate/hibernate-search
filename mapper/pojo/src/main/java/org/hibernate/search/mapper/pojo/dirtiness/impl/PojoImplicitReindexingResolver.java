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
 * @param <T> The type of "dirty" objects for which this class is able to
 * {@link #resolveEntitiesToReindex(PojoReindexingCollector, PojoRuntimeIntrospector, Object)} resolve entities to reindex}.
 * This type may be an entity type, a collection type, ...
 */
public abstract class PojoImplicitReindexingResolver<T> implements ToStringTreeAppendable {

	@Override
	public String toString() {
		return new ToStringTreeBuilder().value( this ).toString();
	}

	/**
	 * Assuming {@code dirty} is dirty, add all entities that should be reindexed to {@code collector}.
	 * @param collector A collector for dirty entities that should be reindexed.
	 * @param dirty A value that can be assumed dirty.
	 */
	// TODO pass dirty properties to only reindex containing entities
	// that are affected by the changes in the contained entity
	public abstract void resolveEntitiesToReindex(PojoReindexingCollector collector,
			PojoRuntimeIntrospector runtimeIntrospector, T dirty);

	public static <T> PojoImplicitReindexingResolver<T> noOp() {
		return NoOpPojoImplicitReindexingResolver.get();
	}

}
