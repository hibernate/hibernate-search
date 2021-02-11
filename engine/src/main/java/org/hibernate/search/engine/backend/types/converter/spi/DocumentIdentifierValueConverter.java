/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.backend.types.converter.spi;

import org.hibernate.search.engine.backend.types.converter.runtime.spi.ToDocumentIdentifierValueConvertContext;
import org.hibernate.search.engine.backend.types.converter.runtime.spi.ToDocumentIdentifierValueConvertContextExtension;
import org.hibernate.search.util.common.SearchException;

/**
 * A converter from a source identifier value to a target value that should be used as a document identifier
 * and back to the source identifier from the document identifier.
 *
 * @param <I> The type of source identifier values.
 */
public interface DocumentIdentifierValueConverter<I> {

	/**
	 * @param value The source value to convert.
	 * @param context A context that can be
	 * {@link ToDocumentIdentifierValueConvertContext#extension(ToDocumentIdentifierValueConvertContextExtension) extended}
	 * to a more useful type, giving access to such things as a Hibernate ORM SessionFactory (if using the Hibernate ORM mapper).
	 * @return The converted index field value.
	 */
	String convertToDocument(I value, ToDocumentIdentifierValueConvertContext context);

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
	String convertToDocumentUnknown(Object value, ToDocumentIdentifierValueConvertContext context);

	/**
	 * @param requiredType the required type
	 * @throws SearchException if the expected actual type is not a subclass of the required type
	 */
	default void requiresType(Class<?> requiredType) {
		// no check by default
	}

	/**
	 * @param other Another {@link DocumentIdentifierValueConverter}, never {@code null}.
	 * @return {@code true} if the given object behaves exactly the same as this object, i.e. its
	 * {@link #convertToDocumentUnknown(Object, ToDocumentIdentifierValueConvertContext)} method is guaranteed to always return the
	 * same value as this object's when given the same input. {@code false} otherwise, or when in doubt.
	 */
	default boolean isCompatibleWith(DocumentIdentifierValueConverter<?> other) {
		return equals( other );
	}

}
