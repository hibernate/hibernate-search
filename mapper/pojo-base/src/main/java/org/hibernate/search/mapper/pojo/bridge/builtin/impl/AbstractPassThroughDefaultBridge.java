/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.bridge.builtin.impl;

import org.hibernate.search.engine.backend.types.dsl.IndexFieldTypeFactory;
import org.hibernate.search.mapper.pojo.bridge.runtime.ValueBridgeFromIndexedValueContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.ValueBridgeToIndexedValueContext;

/**
 * An abstract base for default bridges that act as a pass-through,
 * i.e. a value bridge that passes the input value as-is to the underlying backend.
 * <p>
 * This bridge will not work for all types: only types supported by the backend
 * through {@link IndexFieldTypeFactory#as(Class)} will work.
 *
 * @param <F> The type of input values, as well as the type of the index field.
 */
abstract class AbstractPassThroughDefaultBridge<F> extends AbstractSimpleDefaultBridge<F, F> {

	@Override
	public final F toIndexedValue(F value, ValueBridgeToIndexedValueContext context) {
		return value;
	}

	@Override
	public final F fromIndexedValue(F value, ValueBridgeFromIndexedValueContext context) {
		return value;
	}

	@Override
	public final F parse(String value) {
		return fromString( value );
	}
}
