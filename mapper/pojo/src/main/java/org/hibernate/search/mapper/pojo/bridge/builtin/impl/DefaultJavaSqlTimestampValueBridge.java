/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.bridge.builtin.impl;

import java.sql.Timestamp;
import java.time.Instant;

import org.hibernate.search.engine.backend.types.converter.FromDocumentFieldValueConverter;
import org.hibernate.search.engine.backend.types.converter.runtime.FromDocumentFieldValueConvertContext;
import org.hibernate.search.engine.backend.types.dsl.StandardIndexFieldTypeOptionsStep;
import org.hibernate.search.engine.cfg.spi.ParseUtils;
import org.hibernate.search.mapper.pojo.bridge.ValueBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.ValueBridgeBindingContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.ValueBridgeToIndexedValueContext;

public final class DefaultJavaSqlTimestampValueBridge implements ValueBridge<Timestamp, Instant> {

	@Override
	public String toString() {
		return getClass().getSimpleName();
	}

	@Override
	public StandardIndexFieldTypeOptionsStep<?, Instant> bind(ValueBridgeBindingContext<Timestamp> context) {
		return context.getTypeFactory().asInstant()
				.projectionConverter( PojoDefaultSqlDateFromDocumentFieldValueConverter.INSTANCE );
	}

	@Override
	public Instant toIndexedValue(Timestamp value, ValueBridgeToIndexedValueContext context) {
		return value == null ? null : Instant.ofEpochMilli( value.getTime() );
	}

	@Override
	public Timestamp cast(Object value) {
		return (Timestamp) value;
	}

	@Override
	public Instant parse(String value) {
		return ParseUtils.parseInstant( value );
	}

	@Override
	public boolean isCompatibleWith(ValueBridge<?, ?> other) {
		return getClass().equals( other.getClass() );
	}

	private static class PojoDefaultSqlDateFromDocumentFieldValueConverter
			implements FromDocumentFieldValueConverter<Instant, Timestamp> {
		private static final PojoDefaultSqlDateFromDocumentFieldValueConverter INSTANCE = new PojoDefaultSqlDateFromDocumentFieldValueConverter();

		@Override
		public boolean isConvertedTypeAssignableTo(Class<?> superTypeCandidate) {
			return superTypeCandidate.isAssignableFrom( Timestamp.class );
		}

		@Override
		public Timestamp convert(Instant value, FromDocumentFieldValueConvertContext context) {
			return value == null ? null : new Timestamp( value.toEpochMilli() );
		}

		@Override
		public boolean isCompatibleWith(FromDocumentFieldValueConverter<?, ?> other) {
			return INSTANCE.equals( other );
		}
	}

}