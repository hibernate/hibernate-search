/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.bridge.mapping.impl;

import org.hibernate.search.mapper.pojo.bridge.ValueBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.ValueBindingContext;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.ValueBinder;

class StaticValueBinder<T> implements ValueBinder {
	private final Class<T> expectedValueType;
	private final ValueBridge<T, ?> bridge;

	StaticValueBinder(Class<T> expectedValueType, ValueBridge<T, ?> bridge) {
		this.expectedValueType = expectedValueType;
		this.bridge = bridge;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "["
				+ "expectedValueType=" + expectedValueType
				+ "bridge=" + bridge
				+ "]";
	}

	@Override
	public void bind(ValueBindingContext<?> context) {
		context.bridge( expectedValueType, bridge );
	}
}
