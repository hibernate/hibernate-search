/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.backend.document.spi;

import org.hibernate.search.engine.backend.document.converter.FromIndexFieldValueConverter;
import org.hibernate.search.engine.backend.document.converter.ToIndexFieldValueConverter;
import org.hibernate.search.engine.backend.document.converter.runtime.FromIndexFieldValueConvertContext;
import org.hibernate.search.engine.backend.document.converter.runtime.ToIndexFieldValueConvertContext;

/**
 * A helper class allowing to convert values between the type expected by the user
 * and the type expected by the backend (the type of the "raw" field).
 * <p>
 * Used to convert values passed to the predicate/sort DSL to the "backend" type,
 * and also to convert projected values to the "user" type.
 *
 * @see IndexSchemaFieldDefinitionHelper#createUserIndexFieldConverter()
 *
 * @param <F> The type of the "raw" field.
 */
public final class UserIndexFieldConverter<F> {

	private final Class<F> indexFieldType;
	private final ToIndexFieldValueConverter<?, ? extends F> dslToIndexConverter;
	private final FromIndexFieldValueConverter<? super F, ?> indexToProjectionConverter;

	UserIndexFieldConverter(Class<F> indexFieldType, ToIndexFieldValueConverter<?,? extends F> dslToIndexConverter,
			FromIndexFieldValueConverter<? super F,?> indexToProjectionConverter) {
		this.indexFieldType = indexFieldType;
		this.dslToIndexConverter = dslToIndexConverter;
		this.indexToProjectionConverter = indexToProjectionConverter;
	}

	@Override
	public String toString() {
		return getClass().getName() + "["
				+ "dslToIndexConverter=" + dslToIndexConverter
				+ ", indexToProjectionConverter=" + indexToProjectionConverter
				+ "]";
	}

	public F convertDslToIndex(Object value, ToIndexFieldValueConvertContext context) {
		return dslToIndexConverter.convertUnknown( value, context );
	}

	public Object convertIndexToProjection(F indexValue, FromIndexFieldValueConvertContext context) {
		if ( indexToProjectionConverter == null ) {
			// FIXME detect this when the projection is configured and throw an exception with an explicit message instead. A converter set to null means we don't want to enable projections.
			return indexValue;
		}
		return indexToProjectionConverter.convert( indexValue, context );
	}

	/**
	 * Determine whether another converter's {@link #convertDslToIndex(Object, ToIndexFieldValueConvertContext)}
	 * method is compatible with this one's,
	 * i.e. the method is guaranteed to always return the same value as this converter's when given the same input.
	 * <p>
	 * Note: this method is separate from {@link #equals(Object)} because it might return {@code true} for two different objects,
	 * e.g. two objects that implement {@link #convertIndexToProjection(Object, FromIndexFieldValueConvertContext)} differently,
	 * but still implement {@link #convertDslToIndex(Object, ToIndexFieldValueConvertContext)} in a compatible way.
	 *
	 * @param other Another {@link UserIndexFieldConverter}.
	 * @return {@code true} if the given converter's
	 * {@link #convertDslToIndex(Object, ToIndexFieldValueConvertContext)} method is compatible.
	 * {@code false} otherwise, or when in doubt.
	 */
	public boolean isConvertDslToIndexCompatibleWith(UserIndexFieldConverter<?> other) {
		if ( other == null ) {
			return false;
		}
		return dslToIndexConverter.isCompatibleWith( other.dslToIndexConverter );
	}

	/**
	 * Determine whether another converter's {@link #convertIndexToProjection(Object, FromIndexFieldValueConvertContext)}
	 * method is compatible with this one's,
	 * i.e. the method is guaranteed to always return the same value as this converter's when given the same input.
	 * <p>
	 * Note: this method is separate from {@link #equals(Object)} because it might return {@code true} for two different objects,
	 * e.g. two objects that implement {@link #convertDslToIndex(Object, ToIndexFieldValueConvertContext)} differently,
	 * but still implement {@link #convertIndexToProjection(Object, FromIndexFieldValueConvertContext)} in a compatible way.
	 *
	 * @param other Another {@link UserIndexFieldConverter}.
	 * @return {@code true} if the given converter's
	 * {@link #convertIndexToProjection(Object, FromIndexFieldValueConvertContext)} method is compatible.
	 * {@code false} otherwise, or when in doubt.
	 */
	public boolean isConvertIndexToProjectionCompatibleWith(UserIndexFieldConverter<?> other) {
		if ( other == null ) {
			return false;
		}
		if ( indexToProjectionConverter == null || other.indexToProjectionConverter == null ) {
			// If one projection converter is null, then both must be null in order to be compatible
			return indexToProjectionConverter == null && other.indexToProjectionConverter == null;
		}

		return indexToProjectionConverter.isCompatibleWith( other.indexToProjectionConverter );
	}

	/**
	 * Determine whether the given projection type is compatible with this converter.
	 *
	 * @param projectionType The projection type.
	 * @return {@code true} if the given projection type is compatible. {@code false} otherwise
	 */
	public boolean isProjectionCompatibleWith(Class<?> projectionType) {
		if ( indexToProjectionConverter == null ) {
			return projectionType.isAssignableFrom( indexFieldType );
		}

		return indexToProjectionConverter.isConvertedTypeAssignableTo( projectionType );
	}
}
