/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.backend.types.converter;

import org.hibernate.search.engine.backend.types.converter.runtime.ToDocumentFieldValueConvertContext;
import org.hibernate.search.engine.backend.types.converter.runtime.ToDocumentFieldValueConvertContextExtension;

/**
 * A converter from a source value to a target value that should be indexed.
 *
 * @param <V> The type of source values.
 * @param <F> The type of target, index field values.
 */
public interface ToDocumentFieldValueConverter<V, F> {

	/**
	 * Check whether the given type is a valid type for values passed
	 * to {@link #convert(Object, ToDocumentFieldValueConvertContext)},
	 * which generally means the given type is a subtype of {@link V}.
	 * <p>
	 * This method is generally implemented like this:
	 * {@code return TheInputType.class.isAssignableFrom( inputTypeCandidate )}.
	 * @param inputTypeCandidate A candidate type for the input of {@link #convertUnknown(Object, ToDocumentFieldValueConvertContext)}.
	 * @return {@code true} if values of type {@code inputTypeCandidate}
	 * may be accepted by {@link #convertUnknown(Object, ToDocumentFieldValueConvertContext)},
	 * {@code false} otherwise.
	 */
	boolean isValidInputType(Class<?> inputTypeCandidate);

	/**
	 * @param value The source value to convert.
	 * @param context A context that can be
	 * {@link ToDocumentFieldValueConvertContext#extension(ToDocumentFieldValueConvertContextExtension) extended}
	 * to a more useful type, giving access to such things as a Hibernate ORM SessionFactory (if using the Hibernate ORM mapper).
	 * @return The converted index field value.
	 */
	F convert(V value, ToDocumentFieldValueConvertContext context);

	/**
	 * Convert an input value of unknown type that may not have the required type {@code V}.
	 * <p>
	 * Called when passing values to the predicate DSL in particular.
	 *
	 * @param value The value to convert.
	 * @param context A context that can be
	 * {@link ToDocumentFieldValueConvertContext#extension(ToDocumentFieldValueConvertContextExtension) extended}
	 * to a more useful type, giving access to such things as a Hibernate ORM SessionFactory (if using the Hibernate ORM mapper).
	 * @return The converted index field value.
	 * @throws RuntimeException If the value does not match the expected type.
	 */
	F convertUnknown(Object value, ToDocumentFieldValueConvertContext context);

	/**
	 * @param other Another {@link ToDocumentFieldValueConverter}, never {@code null}.
	 * @return {@code true} if the given object behaves exactly the same as this object,
	 * i.e. its {@link #convert(Object, ToDocumentFieldValueConvertContext)} and {@link #convertUnknown(Object, ToDocumentFieldValueConvertContext)}
	 * methods are guaranteed to always return the same value as this object's
	 * when given the same input. {@code false} otherwise, or when in doubt.
	 */
	default boolean isCompatibleWith(ToDocumentFieldValueConverter<?, ?> other) {
		return equals( other );
	}

}
