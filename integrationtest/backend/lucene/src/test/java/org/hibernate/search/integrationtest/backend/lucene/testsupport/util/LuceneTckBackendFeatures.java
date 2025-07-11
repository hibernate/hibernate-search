/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.lucene.testsupport.util;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.MonthDay;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;

import org.hibernate.search.engine.backend.types.ObjectStructure;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.BigDecimalFieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.BigIntegerFieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.BooleanFieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.ByteFieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.FieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.GeoPointFieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.InstantFieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.LocalDateFieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.LocalDateTimeFieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.LocalTimeFieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.MonthDayFieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.OffsetDateTimeFieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.OffsetTimeFieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.ShortFieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.YearFieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.YearMonthFieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.ZonedDateTimeFieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TckBackendFeatures;

import org.apache.lucene.document.DoublePoint;

class LuceneTckBackendFeatures extends TckBackendFeatures {

	@Override
	public boolean projectionPreservesNulls() {
		return false;
	}

	@Override
	public boolean fieldsProjectableByDefault() {
		return false;
	}

	@Override
	public boolean projectionPreservesEmptySingleValuedObject(ObjectStructure structure) {
		// For single-valued, flattened object fields,
		// we cannot distinguish between an empty object (non-null object, but no subfield carries a value)
		// and an empty object.
		return ObjectStructure.NESTED.equals( structure );
	}

	@Override
	public boolean reliesOnNestedDocumentsForMultiValuedObjectProjection() {
		return true;
	}

	@Override
	public boolean supportsHighlightableWithoutProjectable() {
		// The Lucene backend relies on stored values for highlighting
		return false;
	}

	@Override
	public boolean supportsHighlighterUnifiedTypeNoMatchSize() {
		// Lucene default unified highlighter does not support no-match-size setting.
		// While in ES a custom highlighter is used that allows for such option.
		return false;
	}

	@Override
	public boolean supportsHighlighterUnifiedTypeFragmentSize() {
		// Break iterators from `java.text.BreakIterator` do not allow for such config.
		// While in ES a custom iterator is available that wraps sentence and word break iterators and is using the max size option.
		return false;
	}

	@Override
	public boolean supportsHighlighterUnifiedPhraseMatching() {
		return true;
	}

	@Override
	public <F> Object toRawValue(FieldTypeDescriptor<F, ?> descriptor, F value) {
		if ( value == null ) {
			return null;
		}
		if ( BigIntegerFieldTypeDescriptor.INSTANCE.equals( descriptor ) ) {
			return new BigDecimal( ( (BigInteger) value ) ).setScale( -2, RoundingMode.HALF_UP ).unscaledValue().longValue();
		}
		if ( BigDecimalFieldTypeDescriptor.INSTANCE.equals( descriptor ) ) {
			return ( (BigDecimal) value ).setScale( 2, RoundingMode.HALF_UP ).unscaledValue().longValue();
		}
		if ( GeoPointFieldTypeDescriptor.INSTANCE.equals( descriptor ) ) {
			byte[] bytes = new byte[2 * Double.BYTES];
			DoublePoint.encodeDimension( ( (GeoPoint) value ).latitude(), bytes, 0 );
			DoublePoint.encodeDimension( ( (GeoPoint) value ).longitude(), bytes, Double.BYTES );
			return bytes;
		}
		if ( InstantFieldTypeDescriptor.INSTANCE.equals( descriptor ) ) {
			return ( (Instant) value ).toEpochMilli();
		}
		if ( OffsetTimeFieldTypeDescriptor.INSTANCE.equals( descriptor ) ) {
			// see private method OffsetTime#toEpochNano:
			long nod = ( (OffsetTime) value ).toLocalTime().toNanoOfDay();
			long offsetNanos = ( (OffsetTime) value ).getOffset().getTotalSeconds() * 1_000_000_000L;
			return nod - offsetNanos;
		}
		if ( YearFieldTypeDescriptor.INSTANCE.equals( descriptor ) ) {
			return ( (Year) value ).getValue();
		}
		if ( ZonedDateTimeFieldTypeDescriptor.INSTANCE.equals( descriptor ) ) {
			return ( (ZonedDateTime) value ).toInstant().toEpochMilli();
		}
		if ( LocalDateTimeFieldTypeDescriptor.INSTANCE.equals( descriptor ) ) {
			return ( (LocalDateTime) value ).toInstant( ZoneOffset.UTC ).toEpochMilli();
		}
		if ( LocalTimeFieldTypeDescriptor.INSTANCE.equals( descriptor ) ) {
			return ( (LocalTime) value ).toNanoOfDay();
		}
		if ( LocalDateFieldTypeDescriptor.INSTANCE.equals( descriptor ) ) {
			return ( (LocalDate) value ).toEpochDay();
		}
		if ( BooleanFieldTypeDescriptor.INSTANCE.equals( descriptor ) ) {
			return Boolean.TRUE.equals( value ) ? 1 : 0;
		}
		if ( MonthDayFieldTypeDescriptor.INSTANCE.equals( descriptor ) ) {
			return 100 * ( (MonthDay) value ).getMonthValue() + ( (MonthDay) value ).getDayOfMonth();
		}
		if ( YearMonthFieldTypeDescriptor.INSTANCE.equals( descriptor ) ) {
			return ( (YearMonth) value ).getLong( ChronoField.PROLEPTIC_MONTH );
		}
		if ( OffsetDateTimeFieldTypeDescriptor.INSTANCE.equals( descriptor ) ) {
			return ( (OffsetDateTime) value ).toInstant().toEpochMilli();
		}
		if ( ShortFieldTypeDescriptor.INSTANCE.equals( descriptor )
				|| ByteFieldTypeDescriptor.INSTANCE.equals( descriptor ) ) {
			return ( (Number) value ).intValue();
		}


		return value;
	}

	@Override
	public <F> Class<?> rawType(FieldTypeDescriptor<F, ?> descriptor) {
		if ( BooleanFieldTypeDescriptor.INSTANCE.equals( descriptor )
				|| ByteFieldTypeDescriptor.INSTANCE.equals( descriptor )
				|| ShortFieldTypeDescriptor.INSTANCE.equals( descriptor )
				|| MonthDayFieldTypeDescriptor.INSTANCE.equals( descriptor )
				|| YearFieldTypeDescriptor.INSTANCE.equals( descriptor ) ) {
			return Integer.class;
		}
		if ( GeoPointFieldTypeDescriptor.INSTANCE.equals( descriptor ) ) {
			return byte[].class;
		}
		if ( TemporalAccessor.class.isAssignableFrom( descriptor.getJavaType() )
				|| BigDecimalFieldTypeDescriptor.INSTANCE.equals( descriptor )
				|| BigIntegerFieldTypeDescriptor.INSTANCE.equals( descriptor ) ) {
			return Long.class;
		}
		return descriptor.getJavaType();
	}

	@SuppressWarnings("unchecked")
	@Override
	public <F, T> T fromRawAggregation(FieldTypeDescriptor<F, ?> descriptor, T value) {
		if ( BigIntegerFieldTypeDescriptor.INSTANCE.equals( descriptor ) ) {
			return (T) ( (Number) ( ( (Number) value ).doubleValue() * 100 ) );
		}
		if ( BigDecimalFieldTypeDescriptor.INSTANCE.equals( descriptor ) ) {
			return (T) ( (Number) ( ( (Number) value ).doubleValue() / 100 ) );
		}
		return super.fromRawAggregation( descriptor, value );
	}
}
