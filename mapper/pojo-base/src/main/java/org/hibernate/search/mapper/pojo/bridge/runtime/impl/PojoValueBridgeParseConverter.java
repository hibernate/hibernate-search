/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.bridge.runtime.impl;

import org.hibernate.search.engine.backend.types.converter.ToDocumentValueConverter;
import org.hibernate.search.engine.backend.types.converter.runtime.ToDocumentValueConvertContext;
import org.hibernate.search.mapper.pojo.bridge.ValueBridge;

public final class PojoValueBridgeParseConverter<F>
		implements ToDocumentValueConverter<String, F> {

	private final ValueBridge<?, F> bridge;

	public PojoValueBridgeParseConverter(ValueBridge<?, F> bridge) {
		this.bridge = bridge;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[" + bridge + "]";
	}

	@Override
	public F toDocumentValue(String value, ToDocumentValueConvertContext context) {
		return bridge.parse( value );
	}

	@Override
	public boolean isCompatibleWith(ToDocumentValueConverter<?, ?> other) {
		if ( other == null || !getClass().equals( other.getClass() ) ) {
			return false;
		}
		PojoValueBridgeParseConverter<?> castedOther = (PojoValueBridgeParseConverter<?>) other;
		return bridge.isCompatibleWith( castedOther.bridge );
	}
}
