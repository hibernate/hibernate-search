/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.bridge.builtin.impl;

import java.time.Instant;
import java.util.Date;

import org.hibernate.search.engine.backend.document.converter.FromIndexFieldValueConverter;
import org.hibernate.search.engine.backend.document.converter.runtime.FromIndexFieldValueConvertContext;
import org.hibernate.search.engine.backend.document.model.dsl.StandardIndexSchemaFieldTypedContext;
import org.hibernate.search.mapper.pojo.bridge.ValueBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.ValueBridgeBindingContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.ValueBridgeToIndexedValueContext;

public final class DefaultJavaUtilDateValueBridge implements ValueBridge<Date, Instant> {

	@Override
	public String toString() {
		return getClass().getSimpleName();
	}

	@Override
	@SuppressWarnings("unchecked") // The bridge resolver performs the checks using reflection
	public StandardIndexSchemaFieldTypedContext<?, Instant> bind(ValueBridgeBindingContext<Date> context) {
		return context.getIndexSchemaFieldContext().asInstant()
				.projectionConverter( DefaultDateFromIndexFieldValueConverter.INSTANCE );
	}

	@Override
	public Instant toIndexedValue(Date value, ValueBridgeToIndexedValueContext context) {
		return value == null ? null : value.toInstant();
	}

	@Override
	public Date cast(Object value) {
		return (Date) value;
	}

	@Override
	public boolean isCompatibleWith(ValueBridge<?, ?> other) {
		if ( !getClass().equals( other.getClass() ) ) {
			return false;
		}
		return true;
	}

	private static class DefaultDateFromIndexFieldValueConverter implements FromIndexFieldValueConverter<Instant, Date> {
		private static final DefaultDateFromIndexFieldValueConverter INSTANCE = new DefaultDateFromIndexFieldValueConverter();

		@Override
		public boolean isConvertedTypeAssignableTo(Class<?> superTypeCandidate) {
			return superTypeCandidate.isAssignableFrom( Date.class );
		}

		@Override
		public Date convert(Instant value, FromIndexFieldValueConvertContext context) {
			return value == null ? null : Date.from( value );
		}

		@Override
		public boolean isCompatibleWith(FromIndexFieldValueConverter<?, ?> other) {
			return INSTANCE.equals( other );
		}
	}

}