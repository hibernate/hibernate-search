/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.bridge.runtime.impl;

import org.hibernate.search.engine.backend.types.converter.ToDocumentValueConverter;
import org.hibernate.search.engine.backend.types.converter.runtime.ToDocumentValueConvertContext;
import org.hibernate.search.mapper.pojo.bridge.IdentifierBridge;

public final class PojoIdentifierBridgeParseConverter<T>
		implements ToDocumentValueConverter<String, String> {

	private final IdentifierBridge<T> bridge;

	public PojoIdentifierBridgeParseConverter(IdentifierBridge<T> bridge) {
		this.bridge = bridge;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[" + bridge + "]";
	}

	@Override
	public String toDocumentValue(String value, ToDocumentValueConvertContext context) {
		return bridge.toDocumentIdentifier( bridge.parseIdentifierLiteral( value ),
				context.extension( PojoIdentifierBridgeContextExtension.INSTANCE ) );
	}

	@Override
	public boolean isCompatibleWith(ToDocumentValueConverter<?, ?> other) {
		if ( other == null || !getClass().equals( other.getClass() ) ) {
			return false;
		}
		PojoIdentifierBridgeParseConverter<?> castedOther = (PojoIdentifierBridgeParseConverter<?>) other;
		return bridge.isCompatibleWith( castedOther.bridge );
	}
}
