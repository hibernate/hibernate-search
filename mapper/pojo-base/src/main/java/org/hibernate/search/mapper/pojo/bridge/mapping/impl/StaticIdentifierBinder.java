/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.bridge.mapping.impl;

import org.hibernate.search.mapper.pojo.bridge.IdentifierBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.IdentifierBindingContext;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.IdentifierBinder;

class StaticIdentifierBinder<T> implements IdentifierBinder {
	private final Class<T> expectedIdentifierType;
	private final IdentifierBridge<T> bridge;

	StaticIdentifierBinder(Class<T> expectedIdentifierType, IdentifierBridge<T> bridge) {
		this.expectedIdentifierType = expectedIdentifierType;
		this.bridge = bridge;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "["
				+ "expectedIdentifierType=" + expectedIdentifierType
				+ "bridge=" + bridge
				+ "]";
	}

	@Override
	public void bind(IdentifierBindingContext<?> context) {
		context.bridge( expectedIdentifierType, bridge );
	}
}
