/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.backend.document.converter;

import org.hibernate.search.engine.backend.document.converter.runtime.ToIndexFieldValueConvertContext;
import org.hibernate.search.engine.backend.document.converter.runtime.ToIndexIdValueConvertContext;
import org.hibernate.search.engine.backend.document.converter.runtime.ToIndexIdValueConvertContextExtension;

/**
 * A converter from a source value to a target value that should be indexed.
 *
 * @param <V> The type of source values.
 */
public interface ToIndexIdValueConverter<I> {

	/**
	 * @param value The source value to convert.
	 * @param context A context that can be
	 * {@link ToIndexIdValueConvertContext#extension(ToIndexIdValueConvertContextExtension) extended}
	 * to a more useful type, giving access to such things as a Hibernate ORM SessionFactory (if using the Hibernate ORM mapper).
	 * @return The converted index field value.
	 */
	default String convert(I value, ToIndexIdValueConvertContext context) {
		if ( value == null ) {
			return null;
		}
		return String.valueOf( value );
	}

	/**
	 * @param other Another {@link ToIndexIdValueConverter}, never {@code null}.
	 * @return {@code true} if the given object behaves exactly the same as this object, i.e. its
	 *         {@link #convert(Object, ToIndexFieldValueConvertContext)} and
	 *         {@link #convertUnknown(Object, ToIndexFieldValueConvertContext)} methods are guaranteed to always return the
	 *         same value as this object's when given the same input. {@code false} otherwise, or when in doubt.
	 */
	default boolean isCompatibleWith(ToIndexIdValueConverter<?> other) {
		return equals( other );
	}

}
