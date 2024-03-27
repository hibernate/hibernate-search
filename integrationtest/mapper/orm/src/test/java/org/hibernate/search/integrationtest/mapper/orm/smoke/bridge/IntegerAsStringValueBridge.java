/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.orm.smoke.bridge;

import org.hibernate.search.mapper.pojo.bridge.ValueBridge;
import org.hibernate.search.mapper.pojo.bridge.runtime.ValueBridgeToIndexedValueContext;

public final class IntegerAsStringValueBridge implements ValueBridge<Integer, String> {

	@Override
	public String toIndexedValue(Integer value,
			ValueBridgeToIndexedValueContext context) {
		return value == null ? null : value.toString();
	}

	@Override
	public boolean isCompatibleWith(ValueBridge<?, ?> other) {
		return getClass().equals( other.getClass() );
	}

	@Override
	public void close() {
		// Nothing to do
	}
}
