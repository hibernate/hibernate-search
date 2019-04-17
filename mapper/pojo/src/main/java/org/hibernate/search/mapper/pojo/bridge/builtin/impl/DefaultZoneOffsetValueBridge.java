/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.bridge.builtin.impl;

import java.time.ZoneOffset;

import org.hibernate.search.engine.backend.types.converter.FromDocumentFieldValueConverter;
import org.hibernate.search.engine.backend.types.converter.runtime.FromDocumentFieldValueConvertContext;
import org.hibernate.search.engine.backend.types.dsl.StandardIndexFieldTypeContext;
import org.hibernate.search.engine.cfg.spi.ConvertUtils;
import org.hibernate.search.mapper.pojo.bridge.ValueBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.ValueBridgeBindingContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.ValueBridgeToIndexedValueContext;

public final class DefaultZoneOffsetValueBridge implements ValueBridge<ZoneOffset, Integer> {

	@Override
	public String toString() {
		return getClass().getSimpleName();
	}

	@Override
	@SuppressWarnings("unchecked") // The bridge resolver performs the checks using reflection
	public StandardIndexFieldTypeContext<?, Integer> bind(ValueBridgeBindingContext<ZoneOffset> context) {
		return context.getTypeFactory().asInteger()
				.projectionConverter( PojoDefaultZoneOffsetFromDocumentFieldValueConverter.INSTANCE );
	}

	@Override
	public Integer toIndexedValue(ZoneOffset value, ValueBridgeToIndexedValueContext context) {
		return value == null ? null : value.getTotalSeconds();
	}

	@Override
	public ZoneOffset cast(Object value) {
		return (ZoneOffset) value;
	}

	@Override
	public Integer parse(String value) {
		return ConvertUtils.convertInteger( value );
	}

	@Override
	public boolean isCompatibleWith(ValueBridge<?, ?> other) {
		return getClass().equals( other.getClass() );
	}

	private static class PojoDefaultZoneOffsetFromDocumentFieldValueConverter
			implements FromDocumentFieldValueConverter<Integer, ZoneOffset> {
		private static final PojoDefaultZoneOffsetFromDocumentFieldValueConverter INSTANCE = new PojoDefaultZoneOffsetFromDocumentFieldValueConverter();

		@Override
		public boolean isConvertedTypeAssignableTo(Class<?> superTypeCandidate) {
			return superTypeCandidate.isAssignableFrom( ZoneOffset.class );
		}

		@Override
		public ZoneOffset convert(Integer value, FromDocumentFieldValueConvertContext context) {
			return value == null ? null : ZoneOffset.ofTotalSeconds( value );
		}

		@Override
		public boolean isCompatibleWith(FromDocumentFieldValueConverter<?, ?> other) {
			return INSTANCE.equals( other );
		}
	}
}
