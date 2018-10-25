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
	private final FromIndexFieldValueConverter<? super F, ?> projectionFromIndexConverter;

	UserIndexFieldConverter(Class<F> indexFieldType, ToIndexFieldValueConverter<?,? extends F> dslToIndexConverter,
			FromIndexFieldValueConverter<? super F,?> projectionFromIndexConverter) {
		this.indexFieldType = indexFieldType;
		this.dslToIndexConverter = dslToIndexConverter;
		this.projectionFromIndexConverter = projectionFromIndexConverter;
	}

	@Override
	public String toString() {
		return getClass().getName() + "["
				+ "dslToIndexConverter=" + dslToIndexConverter
				+ ", projectionFromIndexConverter=" + projectionFromIndexConverter
				+ "]";
	}

	public F convertFromDsl(Object value) {
		return dslToIndexConverter.convertUnknown( value );
	}

	public Object convertFromProjection(F projection, FromIndexFieldValueConvertContext context) {
		if ( projectionFromIndexConverter == null ) {
			// FIXME detect this when the projection is configured and throw an exception with an explicit message instead. A converter set to null means we don't want to enable projections.
			return projection;
		}
		return projectionFromIndexConverter.convert( projection, context );
	}

	/**
	 * Determine whether another converter is DSL-compatible with this one,
	 * i.e. its {@link #convertFromDsl(Object)} method is guaranteed
	 * to always return the same value as this converter's when given the same input.
	 * <p>
	 * Note: this method is separate from {@link #equals(Object)} because it might return true for two different objects,
	 * e.g. two objects that implement {@link #convertFromProjection(Object, FromIndexFieldValueConvertContext)} differently.
	 *
	 * @param other Another {@link UserIndexFieldConverter}.
	 * @return {@code true} if the given converter is DSL-compatible.
	 * {@code false} otherwise, or when in doubt.
	 */
	public boolean isDslCompatibleWith(UserIndexFieldConverter<?> other) {
		if ( other == null ) {
			return false;
		}
		return dslToIndexConverter.isCompatibleWith( other.dslToIndexConverter );
	}


	/**
	 * Determine whether the given projection type is compatible with this converter.
	 *
	 * @param projectionType The projection type.
	 * @return {@code true} if the given projection type is compatible. {@code false} otherwise
	 */
	public boolean isProjectionCompatibleWith(Class<?> projectionType) {
		if ( projectionFromIndexConverter == null ) {
			return projectionType.isAssignableFrom( indexFieldType );
		}

		return projectionFromIndexConverter.isConvertedTypeAssignableTo( projectionType );
	}
}
