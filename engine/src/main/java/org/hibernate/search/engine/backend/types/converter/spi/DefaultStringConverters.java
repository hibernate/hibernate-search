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

import org.hibernate.search.engine.backend.types.converter.FromDocumentValueConverter;
import org.hibernate.search.engine.backend.types.converter.ToDocumentValueConverter;
import org.hibernate.search.engine.backend.types.converter.runtime.FromDocumentValueConvertContext;
import org.hibernate.search.engine.backend.types.converter.runtime.ToDocumentValueConvertContext;
import org.hibernate.search.engine.cfg.spi.FormatUtils;
import org.hibernate.search.engine.cfg.spi.ParseUtils;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.util.common.annotation.Incubating;

@Incubating
public final class DefaultStringConverters {
	private DefaultStringConverters() {
	}

	public static final Converter<BigDecimal> BIG_DECIMAL = new Converter<>( ParseUtils::parseBigDecimal, FormatUtils::format );
	public static final Converter<BigInteger> BIG_INTEGER = new Converter<>( ParseUtils::parseBigInteger, FormatUtils::format );
	public static final Converter<Boolean> BOOLEAN = new Converter<>( ParseUtils::parseBoolean, FormatUtils::format );
	public static final Converter<Byte> BYTE = new Converter<>( ParseUtils::parseByte, FormatUtils::format );
	public static final Converter<Double> DOUBLE = new Converter<>( ParseUtils::parseDouble, FormatUtils::format );
	public static final Converter<Float> FLOAT = new Converter<>( ParseUtils::parseFloat, FormatUtils::format );
	public static final Converter<GeoPoint> GEO_POINT = new Converter<>( ParseUtils::parseGeoPoint, FormatUtils::format );
	public static final Converter<Instant> INSTANT = new Converter<>( ParseUtils::parseInstant, FormatUtils::format );
	public static final Converter<Integer> INTEGER = new Converter<>( ParseUtils::parseInteger, FormatUtils::format );
	public static final Converter<LocalDate> LOCAL_DATE = new Converter<>( ParseUtils::parseLocalDate, FormatUtils::format );
	public static final Converter<LocalDateTime> LOCAL_DATE_TIME =
			new Converter<>( ParseUtils::parseLocalDateTime, FormatUtils::format );
	public static final Converter<LocalTime> LOCAL_TIME = new Converter<>( ParseUtils::parseLocalTime, FormatUtils::format );
	public static final Converter<Long> LONG = new Converter<>( ParseUtils::parseLong, FormatUtils::format );
	public static final Converter<MonthDay> MONTH_DAY = new Converter<>( ParseUtils::parseMonthDay, FormatUtils::format );
	public static final Converter<OffsetDateTime> OFFSET_DATE_TIME =
			new Converter<>( ParseUtils::parseOffsetDateTime, FormatUtils::format );
	public static final Converter<OffsetTime> OFFSET_TIME = new Converter<>( ParseUtils::parseOffsetTime, FormatUtils::format );
	public static final Converter<Short> SHORT = new Converter<>( ParseUtils::parseShort, FormatUtils::format );
	public static final Converter<String> STRING = new Converter<>( ParseUtils::parseString, FormatUtils::format );
	public static final Converter<Year> YEAR = new Converter<>( ParseUtils::parseYear, FormatUtils::format );
	public static final Converter<YearMonth> YEAR_MONTH = new Converter<>( ParseUtils::parseYearMonth, FormatUtils::format );
	public static final Converter<ZonedDateTime> ZONED_DATE_TIME =
			new Converter<>( ParseUtils::parseZonedDateTime, FormatUtils::format );


	public static class Converter<F> implements ToDocumentValueConverter<String, F>, FromDocumentValueConverter<F, String> {

		private final Function<String, F> parseDelegate;
		private final Function<F, String> formatDelegate;

		private Converter(Function<String, F> parseDelegate, Function<F, String> formatDelegate) {
			this.parseDelegate = parseDelegate;
			this.formatDelegate = formatDelegate;
		}

		@Override
		public F toDocumentValue(String value, ToDocumentValueConvertContext context) {
			return value == null ? null : parseDelegate.apply( value );
		}

		@Override
		public boolean isCompatibleWith(ToDocumentValueConverter<?, ?> other) {
			return other == this;
		}

		@Override
		public String fromDocumentValue(F value, FromDocumentValueConvertContext context) {
			if ( value == null ) {
				return null;
			}
			return formatDelegate.apply( value );
		}
	}
}
