/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.backend.document.spi;

import org.hibernate.search.engine.backend.document.converter.ToIndexFieldValueConverter;

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

	private final ToIndexFieldValueConverter<?, ? extends F> dslToIndexConverter;

	UserIndexFieldConverter(ToIndexFieldValueConverter<?, ? extends F> dslToIndexConverter) {
		this.dslToIndexConverter = dslToIndexConverter;
	}

	@Override
	public String toString() {
		return getClass().getName() + "["
				+ "dslToIndexConverter=" + dslToIndexConverter
				+ "]";
	}

	public F convertFromDsl(Object value) {
		return dslToIndexConverter.convertUnknown( value );
	}

	/**
	 * Determine whether another converter is DSL-compatible with this one,
	 * i.e. its {@link #convertFromDsl(Object)} method is guaranteed
	 * to always return the same value as this converter's when given the same input.
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

}
