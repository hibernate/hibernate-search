/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.backend.types.converter.spi;

import org.hibernate.search.engine.backend.types.converter.runtime.spi.ToDocumentIdentifierValueConvertContext;
import org.hibernate.search.engine.backend.types.converter.runtime.spi.ToDocumentIdentifierValueConvertContextExtension;

/**
 * A converter from a source identifier value to a target value that should be used as a document identifier.
 *
 * @param <I> The type of source identifier values.
 */
public interface ToDocumentIdentifierValueConverter<I> {

	/**
	 * @param value The source value to convert.
	 * @param context A context that can be
	 * {@link ToDocumentIdentifierValueConvertContext#extension(ToDocumentIdentifierValueConvertContextExtension) extended}
	 * to a more useful type, giving access to such things as a Hibernate ORM SessionFactory (if using the Hibernate ORM mapper).
	 * @return The converted index field value.
	 */
	String convert(I value, ToDocumentIdentifierValueConvertContext context);

	/**
	 * Convert an input value of unknown type that may not have the required type {@code I}.
	 * <p>
	 * Called when passing values to the predicate DSL in particular.
	 *
	 * @param value The source value to convert.
	 * @param context A context that can be
	 * {@link ToDocumentIdentifierValueConvertContext#extension(ToDocumentIdentifierValueConvertContextExtension) extended}
	 * to a more useful type, giving access to such things as a Hibernate ORM SessionFactory (if using the Hibernate ORM mapper).
	 * @return The converted index field value.
	 */
	String convertUnknown(Object value, ToDocumentIdentifierValueConvertContext context);

	/**
	 * @param other Another {@link ToDocumentIdentifierValueConverter}, never {@code null}.
	 * @return {@code true} if the given object behaves exactly the same as this object, i.e. its
	 * {@link #convertUnknown(Object, ToDocumentIdentifierValueConvertContext)} method is guaranteed to always return the
	 * same value as this object's when given the same input. {@code false} otherwise, or when in doubt.
	 */
	default boolean isCompatibleWith(ToDocumentIdentifierValueConverter<?> other) {
		return equals( other );
	}

}
