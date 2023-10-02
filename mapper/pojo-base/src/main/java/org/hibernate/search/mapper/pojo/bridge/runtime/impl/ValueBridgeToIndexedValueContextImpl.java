/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.bridge.runtime.impl;

import org.hibernate.search.engine.common.dsl.spi.DslExtensionState;
import org.hibernate.search.mapper.pojo.bridge.runtime.ValueBridgeToIndexedValueContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.ValueBridgeToIndexedValueContextExtension;
import org.hibernate.search.mapper.pojo.bridge.runtime.spi.BridgeMappingContext;

public class ValueBridgeToIndexedValueContextImpl implements ValueBridgeToIndexedValueContext {

	private final BridgeMappingContext mappingContext;

	public ValueBridgeToIndexedValueContextImpl(BridgeMappingContext mappingContext) {
		this.mappingContext = mappingContext;
	}

	@Override
	public <T> T extension(ValueBridgeToIndexedValueContextExtension<T> extension) {
		return DslExtensionState.returnIfSupported(
				extension,
				extension.extendOptional( this, mappingContext )
		);
	}
}
