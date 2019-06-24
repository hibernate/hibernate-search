/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.bridge.builtin.impl;

import java.util.UUID;

import org.hibernate.search.engine.backend.types.converter.FromDocumentFieldValueConverter;
import org.hibernate.search.engine.backend.types.converter.runtime.FromDocumentFieldValueConvertContext;
import org.hibernate.search.engine.backend.types.dsl.StandardIndexFieldTypeOptionsStep;
import org.hibernate.search.engine.cfg.spi.ValidateUtils;
import org.hibernate.search.mapper.pojo.bridge.ValueBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.ValueBridgeBindingContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.ValueBridgeToIndexedValueContext;

public final class DefaultUUIDValueBridge implements ValueBridge<UUID, String> {

	@Override
	public String toString() {
		return getClass().getSimpleName();
	}

	@Override
	@SuppressWarnings("unchecked") // The bridge resolver performs the checks using reflection
	public StandardIndexFieldTypeOptionsStep<?, String> bind(ValueBridgeBindingContext<UUID> context) {
		return context.getTypeFactory().asString()
				.projectionConverter( PojoDefaultUUIDFromDocumentFieldValueConverter.INSTANCE );
	}

	@Override
	public String toIndexedValue(UUID value, ValueBridgeToIndexedValueContext context) {
		return value == null ? null : value.toString();
	}

	@Override
	public UUID cast(Object value) {
		return (UUID) value;
	}

	@Override
	public String parse(String value) {
		ValidateUtils.validateUUID( value );
		return value;
	}

	@Override
	public boolean isCompatibleWith(ValueBridge<?, ?> other) {
		return getClass().equals( other.getClass() );
	}

	private static class PojoDefaultUUIDFromDocumentFieldValueConverter
			implements FromDocumentFieldValueConverter<String, UUID> {
		private static final PojoDefaultUUIDFromDocumentFieldValueConverter INSTANCE = new PojoDefaultUUIDFromDocumentFieldValueConverter();

		@Override
		public boolean isConvertedTypeAssignableTo(Class<?> superTypeCandidate) {
			return superTypeCandidate.isAssignableFrom( UUID.class );
		}

		@Override
		public UUID convert(String value, FromDocumentFieldValueConvertContext context) {
			return value == null ? null : UUID.fromString( value );
		}

		@Override
		public boolean isCompatibleWith(FromDocumentFieldValueConverter<?, ?> other) {
			return INSTANCE.equals( other );
		}
	}
}
