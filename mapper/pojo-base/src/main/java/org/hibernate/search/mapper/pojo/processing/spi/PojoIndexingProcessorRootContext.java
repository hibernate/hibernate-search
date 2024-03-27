/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.processing.spi;

/**
 * The context passed to a {@link org.hibernate.search.mapper.pojo.processing.impl.PojoIndexingProcessor}
 * and propagated to every node.
 * <p>
 * This includes access to the session, in particular.
 */
public interface PojoIndexingProcessorRootContext {

	PojoIndexingProcessorSessionContext sessionContext();

	/**
	 * @param unproxiedObject An object that was already unproxied using {@link org.hibernate.search.mapper.pojo.model.spi.PojoRuntimeIntrospector#unproxy(Object)}.
	 * @return {@code true} if this object is an entity and is considered deleted in the current context,
	 * {@code false} otherwise.
	 */
	boolean isDeleted(Object unproxiedObject);

}
