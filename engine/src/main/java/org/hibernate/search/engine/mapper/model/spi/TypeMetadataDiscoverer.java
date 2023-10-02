/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.mapper.model.spi;

import java.util.Optional;

/**
 * @param <C> The Java type of type metadata contributors
 */
public interface TypeMetadataDiscoverer<C> {

	/**
	 * A hook to discover metadata lazily during bootstrap, which can be helpful when resolving metadata
	 * from the type itself (Java annotations on a Java type, in particular).
	 *
	 * @param typeModel The type model which is about to be contributed to.
	 * @return An additional, automatically discovered contributor.
	 */
	Optional<C> discover(MappableTypeModel typeModel);

}
