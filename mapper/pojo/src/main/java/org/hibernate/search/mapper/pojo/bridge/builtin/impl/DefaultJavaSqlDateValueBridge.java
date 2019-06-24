/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.bridge.builtin.impl;

import java.sql.Date;
import java.time.Instant;

import org.hibernate.search.engine.backend.types.converter.FromDocumentFieldValueConverter;
import org.hibernate.search.engine.backend.types.converter.runtime.FromDocumentFieldValueConvertContext;
import org.hibernate.search.engine.backend.types.dsl.StandardIndexFieldTypeOptionsStep;
import org.hibernate.search.engine.cfg.spi.ParseUtils;
import org.hibernate.search.mapper.pojo.bridge.ValueBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.ValueBridgeBindingContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.ValueBridgeToIndexedValueContext;

public final class DefaultJavaSqlDateValueBridge implements ValueBridge<Date, Instant> {

	@Override
	public String toString() {
		return getClass().getSimpleName();
	}

	@Override
	public StandardIndexFieldTypeOptionsStep<?, Instant> bind(ValueBridgeBindingContext<Date> context) {
		return context.getTypeFactory().asInstant()
				.projectionConverter( PojoDefaultSqlDateFromDocumentFieldValueConverter.INSTANCE );
	}

	@Override
	public Instant toIndexedValue(Date value, ValueBridgeToIndexedValueContext context) {
		return value == null ? null : Instant.ofEpochMilli( value.getTime() );
	}

	@Override
	public Date cast(Object value) {
		return (Date) value;
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
			implements FromDocumentFieldValueConverter<Instant, Date> {
		private static final PojoDefaultSqlDateFromDocumentFieldValueConverter INSTANCE = new PojoDefaultSqlDateFromDocumentFieldValueConverter();

		@Override
		public boolean isConvertedTypeAssignableTo(Class<?> superTypeCandidate) {
			return superTypeCandidate.isAssignableFrom( Date.class );
		}

		@Override
		public Date convert(Instant value, FromDocumentFieldValueConvertContext context) {
			return value == null ? null : new Date( value.toEpochMilli() );
		}

		@Override
		public boolean isCompatibleWith(FromDocumentFieldValueConverter<?, ?> other) {
			return INSTANCE.equals( other );
		}
	}

}