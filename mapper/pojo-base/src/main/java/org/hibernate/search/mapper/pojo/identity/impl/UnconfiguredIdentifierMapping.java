/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.identity.impl;

import java.util.function.Supplier;

import org.hibernate.search.mapper.pojo.bridge.runtime.spi.BridgeMappingContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.spi.BridgeSessionContext;
import org.hibernate.search.mapper.pojo.logging.impl.MappingLog;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeIdentifier;

public final class UnconfiguredIdentifierMapping<E> implements IdentifierMappingImplementor<Object, E> {

	private final PojoRawTypeIdentifier<E> typeIdentifier;

	public UnconfiguredIdentifierMapping(PojoRawTypeIdentifier<E> typeIdentifier) {
		this.typeIdentifier = typeIdentifier;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[entityType = " + typeIdentifier + "]";
	}

	@Override
	public void close() {
		// Nothing to close
	}

	@Override
	public Object getIdentifier(Object providedId, Supplier<? extends E> entitySupplierOrNull) {
		if ( providedId != null ) {
			return providedId;
		}
		else {
			throw MappingLog.INSTANCE.cannotWorkWithIdentifierBecauseUnconfiguredIdentifierMapping( typeIdentifier );
		}
	}

	@Override
	public Object getIdentifierOrNull(E entity) {
		return null;
	}

	@Override
	public String toDocumentIdentifier(Object identifier, BridgeMappingContext context) {
		throw MappingLog.INSTANCE.cannotWorkWithIdentifierBecauseUnconfiguredIdentifierMapping( typeIdentifier );
	}

	@Override
	public Object fromDocumentIdentifier(String documentId, BridgeSessionContext sessionContext) {
		throw MappingLog.INSTANCE.cannotWorkWithIdentifierBecauseUnconfiguredIdentifierMapping( typeIdentifier );
	}
}
