/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.bridge;

import org.hibernate.search.engine.backend.document.model.IndexSchemaFieldContext;
import org.hibernate.search.engine.backend.document.model.IndexSchemaFieldTypedContext;

/**
 * A bridge between a POJO-extracted value of type {@code T} and an index field of type {@code R}.
 * <p>
 * The {@code ValueBridge} interface is a simpler version of {@link PropertyBridge},
 * in which no property metadata is available
 * and a given input value can only be mapped to a single field, in particular.
 *
 * @param <T> The type of values on the POJO side of the bridge.
 * @param <R> The type of values on the index side of the bridge.
 *
 * @author Yoann Rodiere
 */
public interface ValueBridge<T, R> extends AutoCloseable {

	/**
	 * Bind this bridge instance to the given index field model.
	 * <p>
	 * This method is called exactly once for each bridge instance, before any other method.
	 * It allows the bridge to declare its expectations regarding the index field (type, storage options, ...).
	 *
	 * @param context An entry point to declaring expectations and retrieving accessors to the index schema.
	 * @return The result provided by {@code context} after setting the expectations regarding the index field
	 * (for instance {@code return context.asString()}). {@code null} to let Hibernate Search derive the expectations
	 * from the {@code ValueBridge}'s generic type parameters.
	 */
	default IndexSchemaFieldTypedContext<R> bind(IndexSchemaFieldContext context) {
		return null; // Auto-detect the return type and use default encoding
	}

	/**
	 * Transform the given POJO-extracted value to the value of the indexed field.
	 *
	 * @param value The POJO-extracted value to be transformed.
	 * @return The value of the indexed field.
	 */
	R toIndexedValue(T value);

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
	default Object fromIndexedValue(R indexedValue) {
		return indexedValue;
	}

	/**
	 * Close any resource before the bridge is discarded.
	 */
	@Override
	default void close() {
	}

}
