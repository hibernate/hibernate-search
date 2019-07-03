/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.bridge;

import org.hibernate.search.engine.backend.types.converter.FromDocumentFieldValueConverter;
import org.hibernate.search.engine.backend.types.converter.ToDocumentFieldValueConverter;
import org.hibernate.search.engine.backend.types.dsl.IndexFieldTypeConverterStep;
import org.hibernate.search.engine.backend.types.dsl.StandardIndexFieldTypeOptionsStep;
import org.hibernate.search.mapper.pojo.bridge.binding.ValueBridgeBindingContext;
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
	 * Bind this bridge instance to the given index field model and the given POJO model element.
	 * <p>
	 * This method is called exactly once for each bridge instance, before any other method.
	 * It allows the bridge to:
	 * <ul>
	 *     <li>Declare its expectations regarding the index field type
	 *     using {@link ValueBridgeBindingContext#getTypeFactory()}.
	 *     <p>
	 *     Note the {@link IndexFieldTypeConverterStep#dslConverter(ToDocumentFieldValueConverter) DSL converter}
	 *     and {@link IndexFieldTypeConverterStep#projectionConverter(FromDocumentFieldValueConverter) projection converter}
	 *     will be ignored, since they are already implemented by the value bridge itself
	 *     through its {@link #toIndexedValue(Object, ValueBridgeToIndexedValueContext)}
	 *     and {@link #fromIndexedValue(Object, ValueBridgeFromIndexedValueContext)} methods.
	 *     <li>Inspect the type of values extracted from the POJO model that will be passed to this bridge
	 *     using {@link ValueBridgeBindingContext#getBridgedElement()}.
	 * </ul>
	 *
	 * @param context An entry point allowing to perform the operations listed above.
	 * @return The result of calling {@code context.getTypeFactory()} and setting the expectations regarding the index field type
	 * (for instance {@code return context.getTypeFactory().asString()}).
	 * {@code null} to let Hibernate Search derive the expectations
	 * from the {@code ValueBridge}'s generic type parameters.
	 */
	default StandardIndexFieldTypeOptionsStep<?, F> bind(ValueBridgeBindingContext<V> context) {
		return null; // Auto-detect the return type and use default encoding
	}

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
	 * Cast an input value to the expected type {@link V}.
	 * <p>
	 * Called for values passed to the predicate DSL in particular.
	 *
	 * @param value The value to convert.
	 * @return The checked value.
	 * @throws RuntimeException If the value does not match the expected type.
	 */
	V cast(Object value);

	/**
	 * Parse an input String to the raw index field value.
	 *
	 * @param value The value to parse.
	 * @return The raw index field value.
	 * @throws RuntimeException If the value cannot be parsed to the raw index field value.
	 */
	default F parse(String value) {
		throw new UnsupportedOperationException( "Bridge " + toString() + " does not support parsing a value from a String. Trying to parse the value: " + value + "." );
	}

	/**
	 * @param other Another {@link ValueBridge}, never {@code null}.
	 * @return {@code true} if the given object is also a {@link ValueBridge}
	 * that behaves exactly the same as this object, i.e. its {@link #cast(Object)} and {@link #toIndexedValue(Object, ValueBridgeToIndexedValueContext)}
	 * methods are guaranteed to always return the same value as this object's
	 * when given the same input. {@code false} otherwise, or when in doubt.
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
