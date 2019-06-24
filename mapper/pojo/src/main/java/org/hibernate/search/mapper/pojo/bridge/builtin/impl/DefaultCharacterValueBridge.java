/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.bridge.builtin.impl;

import org.hibernate.search.engine.backend.types.converter.FromDocumentFieldValueConverter;
import org.hibernate.search.engine.backend.types.converter.runtime.FromDocumentFieldValueConvertContext;
import org.hibernate.search.engine.backend.types.dsl.StandardIndexFieldTypeOptionsStep;
import org.hibernate.search.engine.cfg.spi.ValidateUtils;
import org.hibernate.search.mapper.pojo.bridge.ValueBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.ValueBridgeBindingContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.ValueBridgeToIndexedValueContext;

public final class DefaultCharacterValueBridge implements ValueBridge<Character, String> {

	@Override
	public String toString() {
		return getClass().getSimpleName();
	}

	@Override
	@SuppressWarnings("unchecked") // The bridge resolver performs the checks using reflection
	public StandardIndexFieldTypeOptionsStep<?, String> bind(ValueBridgeBindingContext<Character> context) {
		return context.getTypeFactory().asString()
				.projectionConverter( PojoDefaultCharacterFromDocumentFieldValueConverter.INSTANCE );
	}

	@Override
	public String toIndexedValue(Character value, ValueBridgeToIndexedValueContext context) {
		// The character is turned into a one character String
		return value == null ? null : Character.toString( value );
	}

	@Override
	public Character cast(Object value) {
		return (Character) value;
	}

	@Override
	public String parse(String value) {
		ValidateUtils.validateCharacter( value );
		return value;
	}

	@Override
	public boolean isCompatibleWith(ValueBridge<?, ?> other) {
		return getClass().equals( other.getClass() );
	}

	private static class PojoDefaultCharacterFromDocumentFieldValueConverter
			implements FromDocumentFieldValueConverter<String, Character> {
		private static final PojoDefaultCharacterFromDocumentFieldValueConverter INSTANCE = new PojoDefaultCharacterFromDocumentFieldValueConverter();

		@Override
		public boolean isConvertedTypeAssignableTo(Class<?> superTypeCandidate) {
			return superTypeCandidate.isAssignableFrom( Character.class );
		}

		@Override
		public Character convert(String value, FromDocumentFieldValueConvertContext context) {
			return ( value == null || value.isEmpty() ) ? null : value.charAt( 0 );
		}

		@Override
		public boolean isCompatibleWith(FromDocumentFieldValueConverter<?, ?> other) {
			return INSTANCE.equals( other );
		}
	}
}
