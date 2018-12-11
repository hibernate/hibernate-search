/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.backend.document.converter;

import org.hibernate.search.engine.backend.document.converter.runtime.FromDocumentFieldValueConvertContext;
import org.hibernate.search.engine.backend.document.converter.runtime.FromDocumentFieldValueConvertContextExtension;

/**
 * A converter from a source index field value to a different value.
 *
 * @param <F> The type of source, index field values.
 * @param <V> The type of target values.
 */
public interface FromDocumentFieldValueConverter<F, V> {

	/**
	 * Check whether converted values can be assigned to the given type.
	 * <p>
	 * This method is generally implemented like this:
	 * {@code return superTypeCandidate.isAssignableFrom( TheConvertedType.class )}.
	 * @param superTypeCandidate A candidate type for assignment of converted values.
	 * @return {@code true} if the converted type {@link V} is a subtype of {@code superTypeCandidate},
	 * {@code false} otherwise.
	 */
	boolean isConvertedTypeAssignableTo(Class<?> superTypeCandidate);

	/**
	 * @param value The index field value to convert.
	 * @param context A context that can be
	 * {@link FromDocumentFieldValueConvertContext#extension(FromDocumentFieldValueConvertContextExtension) extended}
	 * to a more useful type, giving access to such things as a Hibernate ORM Session (if using the Hibernate ORM mapper).
	 * @return The converted value.
	 */
	V convert(F value, FromDocumentFieldValueConvertContext context);

	/**
	 * @param other Another {@link ToDocumentFieldValueConverter}, never {@code null}.
	 * @return {@code true} if the given object behaves exactly the same as this object,
	 * i.e. its {@link #isConvertedTypeAssignableTo(Class)} and {@link #convert(Object, FromDocumentFieldValueConvertContext)}
	 * methods are guaranteed to always return the same value as this object's
	 * when given the same input. {@code false} otherwise, or when in doubt.
	 */
	default boolean isCompatibleWith(FromDocumentFieldValueConverter<?, ?> other) {
		return equals( other );
	}

}
