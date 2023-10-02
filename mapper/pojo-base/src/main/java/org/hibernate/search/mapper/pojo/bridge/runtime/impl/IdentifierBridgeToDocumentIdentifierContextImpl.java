/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.bridge.runtime.impl;

import org.hibernate.search.engine.common.dsl.spi.DslExtensionState;
import org.hibernate.search.mapper.pojo.bridge.runtime.IdentifierBridgeToDocumentIdentifierContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.IdentifierBridgeToDocumentIdentifierContextExtension;
import org.hibernate.search.mapper.pojo.bridge.runtime.spi.BridgeMappingContext;

public class IdentifierBridgeToDocumentIdentifierContextImpl implements IdentifierBridgeToDocumentIdentifierContext {

	private final BridgeMappingContext mappingContext;

	public IdentifierBridgeToDocumentIdentifierContextImpl(BridgeMappingContext mappingContext) {
		this.mappingContext = mappingContext;
	}

	@Override
	public <T> T extension(IdentifierBridgeToDocumentIdentifierContextExtension<T> extension) {
		return DslExtensionState.returnIfSupported(
				extension,
				extension.extendOptional( this, mappingContext )
		);
	}
}
