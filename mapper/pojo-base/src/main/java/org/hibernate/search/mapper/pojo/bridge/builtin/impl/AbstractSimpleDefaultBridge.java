/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.bridge.builtin.impl;

import org.hibernate.search.mapper.pojo.bridge.IdentifierBridge;
import org.hibernate.search.mapper.pojo.bridge.ValueBridge;
import org.hibernate.search.mapper.pojo.bridge.runtime.IdentifierBridgeFromDocumentIdentifierContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.IdentifierBridgeToDocumentIdentifierContext;

/**
 * An abstract base for simple default bridges implementing both {@link ValueBridge} and {@link IdentifierBridge}
 * where instances do not rely on the provided context.
 * <p>
 * By default, implementations are expected to not rely on any internal fields,
 * so that two instances of the same class always behave the exact same way.
 * If an implementation does rely on internal fields, it should override
 * the {@link #isCompatibleWith(IdentifierBridge)} and {@link #isCompatibleWith(ValueBridge)}
 * methods accordingly.
 *
 * @param <V> The type of values on the POJO side of the bridge.
 * @param <F> The type of raw index field values, on the index side of the bridge.
 */
abstract class AbstractSimpleDefaultBridge<V, F> implements ValueBridge<V, F>, IdentifierBridge<V> {

	@Override
	public String toString() {
		return getClass().getSimpleName();
	}

	@Override
	public final void close() {
		// Nothing to do
	}

	@Override
	public final String toDocumentIdentifier(V propertyValue, IdentifierBridgeToDocumentIdentifierContext context) {
		return toString( propertyValue );
	}

	@Override
	public final V fromDocumentIdentifier(String documentIdentifier, IdentifierBridgeFromDocumentIdentifierContext context) {
		return fromString( documentIdentifier );
	}

	@Override
	public boolean isCompatibleWith(IdentifierBridge<?> other) {
		return other == this || getClass().equals( other.getClass() );
	}

	@Override
	public boolean isCompatibleWith(ValueBridge<?, ?> other) {
		return other == this || getClass().equals( other.getClass() );
	}

	protected abstract V fromString(String value);

	protected abstract String toString(V value);
}
