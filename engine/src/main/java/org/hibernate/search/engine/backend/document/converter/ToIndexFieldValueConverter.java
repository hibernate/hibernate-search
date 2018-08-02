/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.backend.document.converter;

/**
 * A converter from a source value to a target value that should be indexed.
 *
 * @param <V> The type of source values.
 * @param <F> The type of target, index field values.
 */
public interface ToIndexFieldValueConverter<V, F> {

	/**
	 * @param value The source value to convert.
	 * @return The converted index field value.
	 */
	F convert(V value);

	/**
	 * Convert an input value of unknown type that may not have the required type {@code V}.
	 * <p>
	 * Called when passing values to the predicate DSL in particular.
	 *
	 * @param value The value to convert.
	 * @return The converted index field value.
	 * @throws RuntimeException If the value does not match the expected type.
	 */
	F convertUnknown(Object value);

	/**
	 * @param other Another {@link ToIndexFieldValueConverter}, never {@code null}.
	 * @return {@code true} if the given object behaves exactly the same as this object,
	 * i.e. its {@link #convert(Object)} and {@link #convertUnknown(Object)}
	 * methods are guaranteed to always return the same value as this object's
	 * when given the same input. {@code false} otherwise, or when in doubt.
	 */
	default boolean isCompatibleWith(ToIndexFieldValueConverter<?, ?> other) {
		return equals( other );
	}

}
