/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.bridge;

import org.hibernate.search.mapper.pojo.bridge.runtime.ValueBridgeFromIndexedValueContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.ValueBridgeToIndexedValueContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.ValueBridgeToIndexedValueContextExtension;

/**
 * A bridge between a POJO-extracted value of type {@code V} and an index field of type {@code F}.
 * <p>
 * The {@code ValueBridge} interface is a simpler version of {@link PropertyBridge},
 * in which no property metadata is available
 * and a given input value can only be mapped to a single field, in particular.
 *
 * @param <V> The type of values on the POJO side of the bridge.
 * @param <F> The type of raw index field values, on the index side of the bridge.
 */
public interface ValueBridge<V, F> extends AutoCloseable {

	/**
	 * Transform the given POJO-extracted value into the value of the indexed field.
	 *
	 * @param value The POJO-extracted value to be transformed.
	 * @param context A context that can be
	 * {@link ValueBridgeToIndexedValueContext#extension(ValueBridgeToIndexedValueContextExtension) extended}
	 * to a more useful type, giving access to such things as a Hibernate ORM SessionFactory (if using the Hibernate ORM mapper).
	 * @return The value of the indexed field.
	 */
	F toIndexedValue(V value, ValueBridgeToIndexedValueContext context);

	/**
	 * Transform the given indexed field value to the corresponding POJO-extracted value.
	 *
	 * @param value The value of the indexed field to be transformed.
	 * @param context A context that can be
	 * {@link ValueBridgeToIndexedValueContext#extension(ValueBridgeToIndexedValueContextExtension) extended}
	 * to a more useful type, giving access to such things as a Hibernate ORM SessionFactory (if using the Hibernate ORM mapper).
	 * @return The POJO-extracted value.
	 * @throws UnsupportedOperationException If conversion is not supported.
	 */
	default V fromIndexedValue(F value, ValueBridgeFromIndexedValueContext context) {
		throw new UnsupportedOperationException( "Bridge " + this + " does not implement fromIndexedValue(...)." );
	}

	/**
	 * Parse an input String to the raw index field value.
	 *
	 * @param value The value to parse.
	 * @return The raw index field value.
	 * @throws RuntimeException If the value cannot be parsed to the raw index field value.
	 */
	default F parse(String value) {
		throw new UnsupportedOperationException( "Bridge " + toString()
				+ " does not support parsing a value from a String. Trying to parse the value: " + value + "." );
	}

	/**
	 * @param other Another {@link ValueBridge}, never {@code null}.
	 * @return {@code true} if the given object is also a {@link ValueBridge}
	 * that behaves exactly the same as this object, i.e. its {@link #toIndexedValue(Object, ValueBridgeToIndexedValueContext)}
	 * method is guaranteed to accept the same values as this object's
	 * and to always return the same value as this object's
	 * when given the same input.
	 * {@code false} otherwise, or when in doubt.
	 */
	default boolean isCompatibleWith(ValueBridge<?, ?> other) {
		return equals( other );
	}

	/**
	 * Close any resource before the bridge is discarded.
	 */
	@Override
	default void close() {
	}

}
