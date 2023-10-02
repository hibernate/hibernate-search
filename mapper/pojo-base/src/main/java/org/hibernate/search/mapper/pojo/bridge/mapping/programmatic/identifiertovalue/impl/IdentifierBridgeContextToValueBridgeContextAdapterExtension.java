/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.identifiertovalue.impl;

import java.util.Optional;

import org.hibernate.search.mapper.pojo.bridge.runtime.IdentifierBridgeFromDocumentIdentifierContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.IdentifierBridgeToDocumentIdentifierContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.ValueBridgeFromIndexedValueContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.ValueBridgeFromIndexedValueContextExtension;
import org.hibernate.search.mapper.pojo.bridge.runtime.ValueBridgeToIndexedValueContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.ValueBridgeToIndexedValueContextExtension;
import org.hibernate.search.mapper.pojo.bridge.runtime.spi.BridgeMappingContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.spi.BridgeSessionContext;

final class IdentifierBridgeContextToValueBridgeContextAdapterExtension
		implements ValueBridgeToIndexedValueContextExtension<IdentifierBridgeToDocumentIdentifierContext>,
		ValueBridgeFromIndexedValueContextExtension<IdentifierBridgeFromDocumentIdentifierContext> {
	static final IdentifierBridgeContextToValueBridgeContextAdapterExtension INSTANCE =
			new IdentifierBridgeContextToValueBridgeContextAdapterExtension();

	private IdentifierBridgeContextToValueBridgeContextAdapterExtension() {
	}

	@Override
	public Optional<IdentifierBridgeFromDocumentIdentifierContext> extendOptional(
			ValueBridgeFromIndexedValueContext original, BridgeSessionContext sessionContext) {
		return Optional.of( sessionContext.identifierBridgeFromDocumentIdentifierContext() );
	}

	@Override
	public Optional<IdentifierBridgeToDocumentIdentifierContext> extendOptional(
			ValueBridgeToIndexedValueContext original, BridgeMappingContext mappingContext) {
		return Optional.of( mappingContext.identifierBridgeToDocumentIdentifierContext() );
	}
}
