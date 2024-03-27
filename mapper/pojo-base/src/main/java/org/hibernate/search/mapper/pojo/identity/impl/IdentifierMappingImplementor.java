/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.identity.impl;

import java.util.function.Supplier;

import org.hibernate.search.mapper.pojo.bridge.runtime.spi.BridgeMappingContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.spi.BridgeSessionContext;
import org.hibernate.search.mapper.pojo.identity.spi.IdentifierMapping;

/**
 * @param <I> The identifier type for the mapped entity type.
 * @param <E> The entity type mapped to an index.
 */
public interface IdentifierMappingImplementor<I, E> extends IdentifierMapping, AutoCloseable {

	@Override
	default void close() {
	}

	@Override
	I fromDocumentIdentifier(String documentId, BridgeSessionContext sessionContext);

	I getIdentifier(Object providedId, Supplier<? extends E> entitySupplier);

	I getIdentifierOrNull(E entity);

	String toDocumentIdentifier(I identifier, BridgeMappingContext context);

}
