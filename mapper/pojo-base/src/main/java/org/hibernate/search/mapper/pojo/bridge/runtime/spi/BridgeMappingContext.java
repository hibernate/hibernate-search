/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.bridge.runtime.spi;

import org.hibernate.search.mapper.pojo.bridge.runtime.IdentifierBridgeToDocumentIdentifierContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.ValueBridgeToIndexedValueContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.ValueBridgeToIndexedValueContextExtension;

/**
 * Mapping-scoped information and operations for use in bridges.
 * <p>
 * Used in particular when extending contexts:
 * {@link ValueBridgeToIndexedValueContextExtension#extendOptional(ValueBridgeToIndexedValueContext, BridgeMappingContext)},
 * ...
 */
public interface BridgeMappingContext {

	IdentifierBridgeToDocumentIdentifierContext identifierBridgeToDocumentIdentifierContext();

	ValueBridgeToIndexedValueContext valueBridgeToIndexedValueContext();

}
