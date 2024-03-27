/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.loading.spi;

import org.hibernate.search.mapper.pojo.model.spi.PojoRuntimeIntrospector;

/**
 * Context passed {@link PojoMassLoadingStrategy}.
 * <p>
 * Mappers will generally need to cast this type to the mapper-specific subtype.
 */
public interface PojoSelectionLoadingContext {

	/**
	 * Check whether this context is still open, throwing an exception if it is not.
	 */
	void checkOpen();

	PojoRuntimeIntrospector runtimeIntrospector();

}
