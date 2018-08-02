/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.bridge;

import org.hibernate.search.engine.backend.document.model.dsl.StandardIndexSchemaFieldTypedContext;
import org.hibernate.search.mapper.pojo.bridge.binding.ValueBridgeBindingContext;

/**
 * A bridge between a POJO-extracted value of type {@code V} and an index field of type {@code F}.
 * <p>
 * The {@code ValueBridge} interface is a simpler version of {@link PropertyBridge},
 * in which no property metadata is available
 * and a given input value can only be mapped to a single field, in particular.
 *
 * @param <V> The type of values on the POJO side of the bridge.
 * @param <F> The type of raw index field values, on the index side of the bridge.
 *
 * @author Yoann Rodiere
 */
public interface ValueBridge<V, F> extends AutoCloseable {

	/**
	 * Bind this bridge instance to the given index field model and the given POJO model element.
	 * <p>
	 * This method is called exactly once for each bridge instance, before any other method.
	 * It allows the bridge to:
	 * <ul>
	 *     <li>Declare its expectations regarding the index field (type, storage options, ...)
	 *     using {@link ValueBridgeBindingContext#getIndexSchemaFieldContext()}.
	 *     <li>Inspect the type of values extracted from the POJO model that will be passed to this bridge
	 *     using {@link ValueBridgeBindingContext#getBridgedElement()}.
	 * </ul>
	 *
	 * @param context An entry point allowing to perform the operations listed above.
	 * @return The result provided by {@code fieldContext} after setting the expectations regarding the index field
	 * (for instance {@code return fieldContext.asString()}). {@code null} to let Hibernate Search derive the expectations
	 * from the {@code ValueBridge}'s generic type parameters.
	 */
	default StandardIndexSchemaFieldTypedContext<F> bind(ValueBridgeBindingContext context) {
		return null; // Auto-detect the return type and use default encoding
	}

	/**
	 * Transform the given POJO-extracted value to the value of the indexed field.
	 *
	 * @param value The POJO-extracted value to be transformed.
	 * @return The value of the indexed field.
	 */
	F toIndexedValue(V value);

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
	 * @param other Another {@link ValueBridge}, never {@code null}.
	 * @return {@code true} if the given object is also a {@link ValueBridge}
	 * that behaves exactly the same as this object, i.e. its {@link #cast(Object)} and {@link #toIndexedValue(Object)}
	 * methods are guaranteed to always return the same value as this object's
	 * when given the same input. {@code false} otherwise, or when in doubt.
	 */
	default boolean isCompatibleWith(ValueBridge<?, ?> other) {
		return equals( other );
	}

	/**
	 * Transform the given indexed field value back to the value initially extracted from the POJO,
	 * or to any implementation-defined value to be returned in projections on the indexed field.
	 * <p>
	 * For instance, a {@code ValueBridge} indexing JPA entities by putting their identifier in a field
	 * might not be able to resolve the identifier back to an entity, so it could just return the identifier as-is.
	 *
	 * @param indexedValue The field value to be transformed.
	 * @return The value returned in projections on the POJO property.
	 */
	default Object fromIndexedValue(F indexedValue) {
		return indexedValue;
	}

	/**
	 * Close any resource before the bridge is discarded.
	 */
	@Override
	default void close() {
	}

}
