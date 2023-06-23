/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.backend.types.converter;

import org.hibernate.search.engine.backend.types.converter.runtime.ToDocumentValueConvertContext;

/**
 * A converter from a source value to a target value that should be indexed.
 *
 * @param <V> The type of source values.
 * @param <F> The type of target, index field values.
 * @deprecated Implement {@link ToDocumentValueConverter} instead.
 */
@Deprecated
public interface ToDocumentFieldValueConverter<V, F> extends ToDocumentValueConverter<V, F> {

	@Override
	default F toDocumentValue(V value, ToDocumentValueConvertContext context) {
		return convert( value, context );
	}

	@Override
	default boolean isCompatibleWith(ToDocumentValueConverter<?, ?> other) {
		return other instanceof ToDocumentFieldValueConverter
				&& isCompatibleWith( (ToDocumentFieldValueConverter<?, ?>) other );
	}

	/**
	 * @param value The source value to convert.
	 * @param context A context that can be
	 * {@link org.hibernate.search.engine.backend.types.converter.runtime.ToDocumentFieldValueConvertContext#extension(org.hibernate.search.engine.backend.types.converter.runtime.ToDocumentFieldValueConvertContextExtension) extended}
	 * to a more useful type, giving access to such things as a Hibernate ORM SessionFactory (if using the Hibernate ORM mapper).
	 * @return The converted index field value.
	 */
	F convert(V value, org.hibernate.search.engine.backend.types.converter.runtime.ToDocumentFieldValueConvertContext context);

	/**
	 * @param other Another {@link ToDocumentFieldValueConverter}, never {@code null}.
	 * @return {@code true} if the given object behaves exactly the same as this object,
	 * i.e. its {@link #convert(Object, org.hibernate.search.engine.backend.types.converter.runtime.ToDocumentFieldValueConvertContext)}
	 * method is guaranteed to always return the same value as this object's
	 * when given the same input. {@code false} otherwise, or when in doubt.
	 */
	default boolean isCompatibleWith(ToDocumentFieldValueConverter<?, ?> other) {
		return equals( other );
	}

}
