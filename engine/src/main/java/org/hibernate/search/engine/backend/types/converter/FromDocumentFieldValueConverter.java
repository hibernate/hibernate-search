/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.backend.types.converter;

import org.hibernate.search.engine.backend.types.converter.runtime.FromDocumentValueConvertContext;

/**
 * A converter from a source index field value to a different value.
 *
 * @param <F> The type of source, index field values.
 * @param <V> The type of target values.
 * @deprecated Implement {@link FromDocumentValueConverter} instead.
 */
@Deprecated
public interface FromDocumentFieldValueConverter<F, V> extends FromDocumentValueConverter<F, V> {

	@Override
	default V fromDocumentValue(F value, FromDocumentValueConvertContext context) {
		return convert( value, context );
	}

	@Override
	default boolean isCompatibleWith(FromDocumentValueConverter<?, ?> other) {
		return other instanceof FromDocumentFieldValueConverter
				&& isCompatibleWith( (FromDocumentFieldValueConverter<?, ?>) other );
	}

	/**
	 * @param value The index field value to convert.
	 * @param context A context that can be
	 * {@link org.hibernate.search.engine.backend.types.converter.runtime.FromDocumentFieldValueConvertContext#extension(org.hibernate.search.engine.backend.types.converter.runtime.FromDocumentFieldValueConvertContextExtension) extended}
	 * to a more useful type, giving access to such things as a Hibernate ORM Session (if using the Hibernate ORM mapper).
	 * @return The converted value.
	 */
	V convert(F value,
			org.hibernate.search.engine.backend.types.converter.runtime.FromDocumentFieldValueConvertContext context);

	/**
	 * @param other Another {@link ToDocumentFieldValueConverter}, never {@code null}.
	 * @return {@code true} if the given object behaves exactly the same as this object,
	 * i.e. its {@link #convert(Object, org.hibernate.search.engine.backend.types.converter.runtime.FromDocumentFieldValueConvertContext)}
	 * method is guaranteed to always return the same value as this object's
	 * when given the same input. {@code false} otherwise, or when in doubt.
	 */
	default boolean isCompatibleWith(FromDocumentFieldValueConverter<?, ?> other) {
		return equals( other );
	}

}
