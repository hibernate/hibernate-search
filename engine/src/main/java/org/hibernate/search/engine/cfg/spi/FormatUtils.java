/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.cfg.spi;

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
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.util.common.annotation.Incubating;

@Incubating
public final class FormatUtils {

	private FormatUtils() {
	}

	public static String format(BigDecimal value) {
		return value.toPlainString();
	}

	public static String format(BigInteger value) {
		return value.toString();
	}

	public static String format(Boolean value) {
		return value.toString();
	}

	public static String format(Byte value) {
		return value.toString();
	}

	public static String format(Double value) {
		return value.toString();
	}

	public static String format(Float value) {
		return value.toString();
	}

	public static String format(GeoPoint value) {
		// to match parseGeoPoint
		return String.format( Locale.ROOT, "%s, %s", value.latitude(), value.longitude() );
	}

	public static String format(Instant value) {
		return DateTimeFormatter.ISO_INSTANT.format( value );
	}

	public static String format(Integer value) {
		return value.toString();
	}

	public static String format(LocalDate value) {
		return DateTimeFormatter.ISO_LOCAL_DATE.format( value );
	}

	public static String format(LocalDateTime value) {
		return DateTimeFormatter.ISO_LOCAL_DATE_TIME.format( value );
	}

	public static String format(LocalTime value) {
		return DateTimeFormatter.ISO_LOCAL_TIME.format( value );
	}

	public static String format(Long value) {
		return value.toString();
	}

	public static String format(MonthDay value) {
		return ParseUtils.ISO_MONTH_DAY.format( value );
	}

	public static String format(OffsetDateTime value) {
		return DateTimeFormatter.ISO_OFFSET_DATE_TIME.format( value );
	}

	public static String format(OffsetTime value) {
		return DateTimeFormatter.ISO_OFFSET_TIME.format( value );
	}

	public static String format(Short value) {
		return value.toString();
	}

	public static String format(String value) {
		return value;
	}

	public static String format(Year value) {
		return ParseUtils.ISO_YEAR.format( value );
	}

	public static String format(YearMonth value) {
		return ParseUtils.ISO_YEAR_MONTH.format( value );
	}

	public static String format(ZonedDateTime value) {
		return DateTimeFormatter.ISO_ZONED_DATE_TIME.format( value );
	}
}
