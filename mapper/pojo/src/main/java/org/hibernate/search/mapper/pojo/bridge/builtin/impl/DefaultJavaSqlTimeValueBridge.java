/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.bridge.builtin.impl;

import java.sql.Time;
import java.time.Instant;

import org.hibernate.search.engine.backend.types.converter.FromDocumentFieldValueConverter;
import org.hibernate.search.engine.backend.types.converter.runtime.FromDocumentFieldValueConvertContext;
import org.hibernate.search.engine.backend.types.dsl.StandardIndexFieldTypeContext;
import org.hibernate.search.mapper.pojo.bridge.ValueBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.ValueBridgeBindingContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.ValueBridgeToIndexedValueContext;

public final class DefaultJavaSqlTimeValueBridge implements ValueBridge<Time, Instant> {

	@Override
	public String toString() {
		return getClass().getSimpleName();
	}

	@Override
	public StandardIndexFieldTypeContext<?, Instant> bind(ValueBridgeBindingContext<Time> context) {
		return context.getTypeFactory().asInstant()
				.projectionConverter( PojoDefaultSqlDateFromDocumentFieldValueConverter.INSTANCE );
	}

	@Override
	public Instant toIndexedValue(Time value, ValueBridgeToIndexedValueContext context) {
		return value == null ? null : Instant.ofEpochMilli( value.getTime() );
	}

	@Override
	public Time cast(Object value) {
		return (Time) value;
	}

	@Override
	public boolean isCompatibleWith(ValueBridge<?, ?> other) {
		return getClass().equals( other.getClass() );
	}

	private static class PojoDefaultSqlDateFromDocumentFieldValueConverter
			implements FromDocumentFieldValueConverter<Instant, Time> {
		private static final PojoDefaultSqlDateFromDocumentFieldValueConverter INSTANCE = new PojoDefaultSqlDateFromDocumentFieldValueConverter();

		@Override
		public boolean isConvertedTypeAssignableTo(Class<?> superTypeCandidate) {
			return superTypeCandidate.isAssignableFrom( Time.class );
		}

		@Override
		public Time convert(Instant value, FromDocumentFieldValueConvertContext context) {
			return value == null ? null : new Time( value.toEpochMilli() );
		}

		@Override
		public boolean isCompatibleWith(FromDocumentFieldValueConverter<?, ?> other) {
			return INSTANCE.equals( other );
		}
	}

}