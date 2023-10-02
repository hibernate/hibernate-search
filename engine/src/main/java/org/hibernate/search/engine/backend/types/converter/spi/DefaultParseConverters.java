/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.backend.types.converter.spi;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.MonthDay;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZonedDateTime;
import java.util.function.Function;

import org.hibernate.search.engine.backend.types.converter.ToDocumentValueConverter;
import org.hibernate.search.engine.backend.types.converter.runtime.ToDocumentValueConvertContext;
import org.hibernate.search.engine.cfg.spi.ParseUtils;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.util.common.annotation.Incubating;

@Incubating
public final class DefaultParseConverters {
	private DefaultParseConverters() {
	}

	public static final ToDocumentValueConverter<String, BigDecimal> BIG_DECIMAL =
			new Converter<>( ParseUtils::parseBigDecimal );
	public static final ToDocumentValueConverter<String, BigInteger> BIG_INTEGER =
			new Converter<>( ParseUtils::parseBigInteger );
	public static final ToDocumentValueConverter<String, Boolean> BOOLEAN = new Converter<>( ParseUtils::parseBoolean );
	public static final ToDocumentValueConverter<String, Byte> BYTE = new Converter<>( ParseUtils::parseByte );
	public static final ToDocumentValueConverter<String, Double> DOUBLE = new Converter<>( ParseUtils::parseDouble );
	public static final ToDocumentValueConverter<String, Float> FLOAT = new Converter<>( ParseUtils::parseFloat );
	public static final ToDocumentValueConverter<String, GeoPoint> GEO_POINT = new Converter<>( ParseUtils::parseGeoPoint );
	public static final ToDocumentValueConverter<String, Instant> INSTANT = new Converter<>( ParseUtils::parseInstant );
	public static final ToDocumentValueConverter<String, Integer> INTEGER = new Converter<>( ParseUtils::parseInteger );
	public static final ToDocumentValueConverter<String, LocalDate> LOCAL_DATE = new Converter<>( ParseUtils::parseLocalDate );
	public static final ToDocumentValueConverter<String, LocalDateTime> LOCAL_DATE_TIME =
			new Converter<>( ParseUtils::parseLocalDateTime );
	public static final ToDocumentValueConverter<String, LocalTime> LOCAL_TIME = new Converter<>( ParseUtils::parseLocalTime );
	public static final ToDocumentValueConverter<String, Long> LONG = new Converter<>( ParseUtils::parseLong );
	public static final ToDocumentValueConverter<String, MonthDay> MONTH_DAY = new Converter<>( ParseUtils::parseMonthDay );
	public static final ToDocumentValueConverter<String, OffsetDateTime> OFFSET_DATE_TIME =
			new Converter<>( ParseUtils::parseOffsetDateTime );
	public static final ToDocumentValueConverter<String, OffsetTime> OFFSET_TIME =
			new Converter<>( ParseUtils::parseOffsetTime );
	public static final ToDocumentValueConverter<String, Short> SHORT = new Converter<>( ParseUtils::parseShort );
	public static final ToDocumentValueConverter<String, String> STRING = new Converter<>( ParseUtils::parseString );
	public static final ToDocumentValueConverter<String, Year> YEAR = new Converter<>( ParseUtils::parseYear );
	public static final ToDocumentValueConverter<String, YearMonth> YEAR_MONTH = new Converter<>( ParseUtils::parseYearMonth );
	public static final ToDocumentValueConverter<String, ZonedDateTime> ZONED_DATE_TIME =
			new Converter<>( ParseUtils::parseZonedDateTime );


	private static class Converter<F> implements ToDocumentValueConverter<String, F> {

		private final Function<String, F> delegate;

		private Converter(Function<String, F> delegate) {
			this.delegate = delegate;
		}

		@Override
		public F toDocumentValue(String value, ToDocumentValueConvertContext context) {
			return value == null ? null : delegate.apply( value );
		}

		@Override
		public boolean isCompatibleWith(ToDocumentValueConverter<?, ?> other) {
			return other == this;
		}
	}
}
