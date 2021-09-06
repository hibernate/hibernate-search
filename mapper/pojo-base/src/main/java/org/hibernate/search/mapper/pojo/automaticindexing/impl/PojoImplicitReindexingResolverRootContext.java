/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.automaticindexing.impl;

import org.hibernate.search.mapper.pojo.automaticindexing.spi.PojoImplicitReindexingResolverSessionContext;
import org.hibernate.search.mapper.pojo.extractor.ContainerExtractionContext;
import org.hibernate.search.mapper.pojo.model.path.spi.PojoPathFilter;

/**
 * The context passed to a {@link PojoImplicitReindexingResolver}
 * and propagated to every {@link PojoImplicitReindexingResolverNode}.
 * <p>
 * This includes telling whether changes require the changed entity to be reindexed,
 * but also retrieving all entities that use the changed entity in their indexed form
 * so that they can be reindexed by Hibernate Search.
 */
public interface PojoImplicitReindexingResolverRootContext extends ContainerExtractionContext {

	/**
	 * @return The context for the current session.
	 */
	PojoImplicitReindexingResolverSessionContext sessionContext();

	/**
	 * @param filter A path filter for dirty paths.
	 * @return Whether the root is dirty according to the given filter.
	 */
	boolean isDirtyForReindexingResolution(PojoPathFilter filter);

	/**
	 * Propagates (rethrows) a {@link RuntimeException} thrown while accessing a property (getter or field access),
	 * or ignores it so that the property is skipped.
	 *
	 * @param exception A {@link RuntimeException} thrown while accessing a property.
	 * @see org.hibernate.search.util.common.reflect.spi.ValueReadHandle#get(Object)
	 */
	void propagateOrIgnorePropertyAccessException(RuntimeException exception);

}
