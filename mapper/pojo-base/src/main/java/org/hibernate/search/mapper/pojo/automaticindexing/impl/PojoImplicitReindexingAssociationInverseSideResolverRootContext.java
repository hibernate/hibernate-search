/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.automaticindexing.impl;

import org.hibernate.search.mapper.pojo.extractor.ContainerExtractionContext;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeIdentifier;
import org.hibernate.search.mapper.pojo.model.spi.PojoRuntimeIntrospector;

/**
 * The context passed to a {@link PojoImplicitReindexingAssociationInverseSideResolver}
 * and propagated to every {@link PojoImplicitReindexingAssociationInverseSideResolverNode}.
 */
public interface PojoImplicitReindexingAssociationInverseSideResolverRootContext extends ContainerExtractionContext {

	PojoRuntimeIntrospector runtimeIntrospector();

	PojoRawTypeIdentifier<?> detectContainingEntityType(Object containingEntity);

	/**
	 * Propagates (rethrows) a {@link RuntimeException} thrown while accessing a property (getter or field access),
	 * or ignores it so that the property is skipped.
	 *
	 * @param exception A {@link RuntimeException} thrown while accessing a property.
	 * @see org.hibernate.search.util.common.reflect.spi.ValueReadHandle#get(Object)
	 */
	void propagateOrIgnorePropertyAccessException(RuntimeException exception);

}
