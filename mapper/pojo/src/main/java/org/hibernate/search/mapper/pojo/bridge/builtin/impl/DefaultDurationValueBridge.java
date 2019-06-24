/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.bridge.builtin.impl;

import java.lang.invoke.MethodHandles;
import java.time.Duration;

import org.hibernate.search.engine.backend.types.converter.FromDocumentFieldValueConverter;
import org.hibernate.search.engine.backend.types.converter.runtime.FromDocumentFieldValueConvertContext;
import org.hibernate.search.engine.backend.types.dsl.StandardIndexFieldTypeOptionsStep;
import org.hibernate.search.engine.cfg.spi.ParseUtils;
import org.hibernate.search.mapper.pojo.bridge.ValueBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.ValueBridgeBindingContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.ValueBridgeToIndexedValueContext;
import org.hibernate.search.mapper.pojo.logging.impl.Log;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public final class DefaultDurationValueBridge implements ValueBridge<Duration, Long> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	@Override
	public String toString() {
		return getClass().getSimpleName();
	}

	@Override
	@SuppressWarnings("unchecked") // The bridge resolver performs the checks using reflection
	public StandardIndexFieldTypeOptionsStep<?, Long> bind(ValueBridgeBindingContext<Duration> context) {
		return context.getTypeFactory().asLong()
				.projectionConverter( PojoDefaultZoneOffsetFromDocumentFieldValueConverter.INSTANCE );
	}

	@Override
	public Long toIndexedValue(Duration value, ValueBridgeToIndexedValueContext context) {
		return toIndexedValue( value );
	}

	@Override
	public Duration cast(Object value) {
		return (Duration) value;
	}

	@Override
	public Long parse(String value) {
		return toIndexedValue( ParseUtils.parseDuration( value ) );
	}

	@Override
	public boolean isCompatibleWith(ValueBridge<?, ?> other) {
		return getClass().equals( other.getClass() );
	}

	private Long toIndexedValue(Duration value) {
		if ( value == null ) {
			return null;
		}
		try {
			return value.toNanos();
		}
		catch (ArithmeticException ae) {
			throw log.valueTooLargeForConversionException( Duration.class, value, ae );
		}
	}

	private static class PojoDefaultZoneOffsetFromDocumentFieldValueConverter
			implements FromDocumentFieldValueConverter<Long, Duration> {
		private static final PojoDefaultZoneOffsetFromDocumentFieldValueConverter INSTANCE = new PojoDefaultZoneOffsetFromDocumentFieldValueConverter();

		@Override
		public boolean isConvertedTypeAssignableTo(Class<?> superTypeCandidate) {
			return superTypeCandidate.isAssignableFrom( Duration.class );
		}

		@Override
		public Duration convert(Long value, FromDocumentFieldValueConvertContext context) {
			return value == null ? null : Duration.ofNanos( value );
		}

		@Override
		public boolean isCompatibleWith(FromDocumentFieldValueConverter<?, ?> other) {
			return INSTANCE.equals( other );
		}
	}
}
