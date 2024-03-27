/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.loading.spi;

/**
 * The context passed to {@link PojoMassLoadingStrategy#createIdentifierLoader(java.util.Set, PojoMassIdentifierLoadingContext)}.
 *
 * @param <I> The type of entity identifiers.
 */
public interface PojoMassIdentifierLoadingContext<I> {

	/**
	 * @return The parent, mapper-specific loading context.
	 */
	PojoMassLoadingContext parent();

	/**
	 * @return A sink that the loader will add loaded entities to.
	 */
	PojoMassIdentifierSink<I> createSink();

	/**
	 * @return The tenant identifier to use ({@code null} if none).
	 */
	String tenantIdentifier();

}
